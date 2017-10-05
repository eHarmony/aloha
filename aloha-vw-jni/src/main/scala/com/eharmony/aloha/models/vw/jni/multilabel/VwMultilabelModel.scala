package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.json.VwMultilabeledJson
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.models.multilabel.json.MultilabelModelJson
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.models.vw.jni.VwJniModel
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictor.ExpectedLearner
import com.eharmony.aloha.models.vw.jni.multilabel.json.VwMultilabelModelJson
import org.apache.commons.io.IOUtils
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter, pimpAny, pimpString}
import vowpalWabbit.learner.VWLearners

import scala.collection.immutable.ListMap
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


/**
  * Created by ryan.deak on 9/29/17.
  */
object VwMultilabelModel
  extends MultilabelModelJson
     with VwMultilabelModelJson {

  sealed trait VwParamError {
    def originalParams: String
    def modifiedParams: Option[String]
    def errorMessage: String
  }

  final case class UnrecoverableParams(
      originalParams: String,
      unrecoverable: Set[String]
  ) extends VwParamError {
    def modifiedParams: Option[String] = None
    def errorMessage: String =
      "Encountered parameters that cannot be augmented: " +
        unrecoverable.toSeq.sorted.mkString(", ") +
      s"\n\toriginal parameters: $originalParams"
  }

  final case class IncorrectLearner(
      originalParams: String,
      modifiedPs: String,
      learnerCanonicalName: String
  ) extends VwParamError {
    override def modifiedParams: Option[String] = Option(modifiedPs)
    override def errorMessage: String =
      s"Params produced an incorrect learner type.  " +
        s"Expected: ${classOf[ExpectedLearner].getCanonicalName}  " +
        s"Found: $learnerCanonicalName." +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
  }

  final case class ClassCastErr(
      originalParams: String,
      modifiedPs: String,
      ccException: ClassCastException
  ) extends VwParamError {
    override def modifiedParams: Option[String] = Option(modifiedPs)
    override def errorMessage: String =
      s"Params produced an incorrect learner type.  " +
        s"Expected: ${classOf[ExpectedLearner].getCanonicalName}  " +
        s"Encountered ClassCastException: ${ccException.getMessage}" +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
  }

  final case class VwError(
      originalParams: String,
      modifiedPs: String,
      vwErrMsg: String
  ) extends VwParamError {
    override def modifiedParams: Option[String] = Option(modifiedPs)
    override def errorMessage: String =
      s"VW could not create a learner of type " +
        s"${classOf[ExpectedLearner].getCanonicalName}. Error: $vwErrMsg. " +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
  }

  private[this] def pad(s: String) = "\\s" + s + "\\s"
  private[this] val ClassCastMsg        = """(\S+) cannot be cast to (\S+)""".r
  private[this] val CsoaaLdf            = pad("""--csoaa_ldf\s+(mc?)""").r
  private[this] val CsoaaRegression     = "m"
  private[this] val CsoaaClassification = "mc"
  private[this] val Quiet               = pad("--quiet").r
  private[this] val Keep                = pad("""--keep\s+(\S+)""").r
  private[this] val Ignore              = pad("""--ignore\s+(\S+)""").r
  private[this] val IgnoreLinear        = pad("""--ignore_linear\s+(\S+)""").r
  private[this] val UnrecoverableFlags  = pad("""--(cubic|redefine)""").r
  private[this] val QuadraticsShort     = pad("""-q\s*([\S])([\S])\s""").r
  private[this] val QuadraticsLong      = pad("""--quadratic\s+([\S])([\S])""").r

  //$VW -qas -qdf --quadratic  fg
  // --ignore
  // --ignore_linear
  // -q, --quadratic
  // cubic            return None
  // --redefine       return None
  // --noconstant
  // -C, --constant
  // --csoaa_ldf mc
  // --wap_ldf

  /**
    * Add VW parameters to make the multilabel model work:
    * @param vwParams current VW parameters passed to the VW JNI
    * @param namespaceNames
    * @return
    */
  def updatedVwParams(vwParams: String, namespaceNames: Set[String]): Either[VwParamError, String] = {
    val padded = s" ${vwParams.trim} "

    // --ignore
    // --ignore_linear
    // -q, --quadratic
    // cubic            return None
    // --redefine       return None
    // --noconstant
    // -C, --constant
    // --csoaa_ldf mc
    // --wap_ldf

    val unrecoverableFlags = UnrecoverableFlags.findAllMatchIn(vwParams).map(m => m.group(1)).toSet
    if (unrecoverableFlags.nonEmpty)
      Left(UnrecoverableParams(vwParams, unrecoverableFlags))
    else {
      val q      = quadratics(padded)
      val nsq    = nssInQuadratics(q)
      val k      = kept(padded)
      val i      = ignored(padded)
      val il     = ignoredLinear(padded)
      val quiet  = isQuiet(padded)

      // TODO: or just determineLabelNamespaces(namespaceNames) ?
      val allNs  = k ++ nsq ++ namespaceNames.flatMap(_.take(1).toCharArray)
      val labelNss = VwMultilabelRowCreator.determineLabelNamespaces(allNs.map(_.toString))

      // lift "kept" first order namespaces to quadratics with the output namespace
      // lift                    quadratics to cubics     with the output namespace
      // introduce
      //   ignore for the dummy class namespace
      //   ignore linear for the other namespaces
      //   no constant
      //

      // how to deal with ignore (not ignore linear)


      val updatedParams: String = vwParams // TODO: Fill this in.

      validateVwParams(vwParams, updatedParams, !quiet)
    }
  }

  /**
    * Create a JSON representation of an Aloha model.
    *
    * '''NOTE''': Because of the inclusion of the unrestricted `labelsOfInterest` parameter,
    *             the JSON produced by this function is not guaranteed to result in a valid
    *             Aloha model.  This is because no semantics are required by this function and
    *             so, the `labelsOfInterest` function specification cannot be validated.
    *
    * @param datasetSpec a location of a dataset specification.
    * @param binaryVwModel a location of a VW binary model file.
    * @param id a model ID.
    * @param labelsInTrainingSet The sequence of all labels encountered in the training set used
    *                            to produce the `binaryVwModel`.
    *                            '''''It is extremely important that this sequence has the
    *                                 same order as the sequence of labels used in the
    *                                 dataset creation process.'''''  Otherwise, the VW model might
    *                                 associate scores with an incorrect label.
    * @param labelsOfInterest It is possible that a model is trained on a super set of labels for
    *                         which predictions can be made.  If the labels at prediction time
    *                         differs (''or should be extracted from the input to the model''),
    *                         this function can provide that capability.
    * @param vwArgs arguments that should be passed to the VW model.  This likely isn't strictly
    *               necessary.
    * @param externalModel whether the underlying binary VW model should remain as a separate
    *                      file and be referenced by the Aloha model specification (`true`)
    *                      or the binary model content should be embeeded directly into the model
    *                      (`false`).  '''Keep in mind''' Aloha models must be smaller than 2 GB
    *                      because they are decoded to `String`s and `String`s are indexed by
    *                      32-bit integers (which have a max value of 2^32^ - 1).
    * @param numMissingThreshold the number of missing features to tolerate before emitting a
    *                            prediction failure.
    * @tparam K the type of label or class.
    * @return a JSON object.
    */
  def json[K: JsonWriter](
      datasetSpec: Vfs,
      binaryVwModel: Vfs,
      id: ModelIdentity,
      labelsInTrainingSet: Seq[K],
      labelsOfInterest: Option[String] = None,
      vwArgs: Option[String] = None,
      externalModel: Boolean = false,
      numMissingThreshold: Option[Int] = None
  ): JsValue = {

    val dsJsAst = StringReadable.fromInputStream(datasetSpec.inputStream).parseJson
    val ds = dsJsAst.convertTo[VwMultilabeledJson]
    val features = modelFeatures(ds.features)
    val namespaces = ds.namespaces.map(modelNamespaces)
    val modelSrc = modelSource(binaryVwModel, externalModel)
    val vw = vwModelPlugin(modelSrc, vwArgs, namespaces)
    val model = modelAst(id, features, numMissingThreshold, labelsInTrainingSet, labelsOfInterest, vw)

    // Even though we are only *writing* JSON, we need a JsonFormat[K], (which is a reader
    // and writer) because multilabelDataJsonFormat has a JsonFormat context bound on K.
    // So to turn MultilabelData into JSON, we need to lift the JsonWriter for K into a
    // JsonFormat and store it as an implicit value (or create an implicit conversion
    // function on implicit arguments.
    implicit val labelJF = DefaultJsonProtocol.lift(implicitly[JsonWriter[K]])
    model.toJson
  }

  // ==============   updatedVwParams support functions   ===============

  private[multilabel] def quad(r: Regex, chrSeq: CharSequence): Set[(Char, Char)] =
    r.findAllMatchIn(chrSeq).map { m =>
      val Seq(a, b) = (1 to 2).map(i => m.group(i).charAt(0))
      if (a < b) (a, b) else (b, a)
    }.toSet

  private[multilabel] def charsIn(r: Regex, chrSeq: CharSequence): Set[Char] =
    r.findAllMatchIn(chrSeq).flatMap(m => m.group(1).toCharArray).toSet

  private[multilabel] def quadratics(padded: String): Set[(Char, Char)] =
    quad(QuadraticsShort, padded) ++ quad(QuadraticsLong, padded)

  private[multilabel] def nssInQuadratics(quadratics: Set[(Char, Char)]): Set[Char] =
    quadratics flatMap { case (a, b) => Set(a, b) }

  private[multilabel] def isQuiet(padded: String): Boolean = Quiet.findFirstIn(padded).nonEmpty

  private[multilabel] def kept(padded: String): Set[Char] = charsIn(Keep, padded)
  private[multilabel] def ignored(padded: String): Set[Char] = charsIn(Ignore, padded)
  private[multilabel] def ignoredLinear(padded: String): Set[Char] = charsIn(IgnoreLinear, padded)

  private[multilabel] def handleClassCastException(orig: String, mod: String, ex: ClassCastException) =
    ex.getMessage match {
      case ClassCastMsg(from, _) => IncorrectLearner(orig, mod, from)
      case _                     => ClassCastErr(orig, mod, ex)
    }

  private[multilabel] def validateVwParams(orig: String, mod: String, addQuiet: Boolean) = {
    val ps = if (addQuiet) s"--quiet $mod" else mod

    Try { VWLearners.create[ExpectedLearner](ps) } match {
      case Success(m) =>
        IOUtils.closeQuietly(m)
        Right(mod)
      case Failure(cce: ClassCastException) =>
        Left(handleClassCastException(orig, mod, cce))
      case Failure(ex) =>
        Left(VwError(orig, mod, ex.getMessage))
    }
  }

  // ====================   json support functions   ====================

  private[multilabel] def modelFeatures(featuresSpecs: Seq[SparseSpec]) =
    ListMap (
      featuresSpecs.map { case SparseSpec(name, spec, defVal) =>
        name -> Spec(spec, defVal.map(ts => ts.toSeq))
      }: _*
    )

  private[multilabel] def modelNamespaces(nss: Seq[Namespace]) =
    ListMap(
      nss.map(ns => ns.name -> ns.features) : _*
    )

  private[multilabel] def modelSource(binaryVwModel: Vfs, externalModel: Boolean) =
    if (externalModel)
      ExternalSource(binaryVwModel)
    else Base64StringSource(VwJniModel.readBinaryVwModelToB64String(binaryVwModel.inputStream))

  // Private b/c VwMultilabelAst is protected[this].  Don't let it escape.
  private def vwModelPlugin(
      modelSrc: ModelSource,
      vwArgs: Option[String],
      namespaces: Option[ListMap[String, Seq[String]]]) =
    VwMultilabelAst(
      VwSparseMultilabelPredictorProducer.multilabelPlugin.name,
      modelSrc,
      vwArgs.map(a => Right(a)),
      namespaces
    )

  // Private b/c MultilabelData is private[this].  Don't let it escape.
  private def modelAst[K](
      id: ModelIdentity,
      features: ListMap[String, Spec],
      numMissingThreshold: Option[Int],
      labelsInTrainingSet: Seq[K],
      labelsOfInterest: Option[String],
      vwPlugin: VwMultilabelAst) =
    MultilabelData(
      modelType = MultilabelModel.parser.modelType,
      modelId = ModelId(id.getId(), id.getName()),
      features = features,
      numMissingThreshold = numMissingThreshold,
      labelsInTrainingSet = labelsInTrainingSet.toVector,
      labelsOfInterest = labelsOfInterest,
      underlying = vwPlugin.toJson.asJsObject
    )
}
