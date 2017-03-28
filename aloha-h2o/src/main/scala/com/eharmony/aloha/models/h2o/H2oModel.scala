package com.eharmony.aloha.models.h2o

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.Properties

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory.{ModelParser, ModelSubmodelParsingPlugin, ParserProviderCompanion, SubmodelFactory}
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.AlohaReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.h2o.H2oModel.Features
import com.eharmony.aloha.models.h2o.categories._
import com.eharmony.aloha.models.h2o.compiler.Compiler
import com.eharmony.aloha.models.h2o.json.{H2oAst, H2oSpec}
import com.eharmony.aloha.models.{SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging}
import hex.genmodel.GenModel
import hex.genmodel.easy.exception.PredictUnknownCategoricalLevelException
import hex.genmodel.easy.{EasyPredictModelWrapper, RowData}
import org.apache.commons.codec.binary.Base64
import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json._

import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.collection.{immutable => sci}
import scala.util.{Failure, Success, Try}

/**
 * Created by deak on 9/30/15.
 */
final case class H2oModel[U, N: RefInfo, -A, +B <: U](
    modelId: ModelIdentity,
    h2OModel: GenModel,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[FeatureFunction[A]],
    auditor: Auditor[U, N, B],
    numMissingThreshold: Option[Int] = None)
  extends SubmodelBase[U, N, A, B]
     with Logging {

  // Because H2O's RowData object is essentially a Map of String to Object, we unapply the wrapper
  // and throw away the type information on the function return type.  We have type safety because
  // FeatureFunction is sealed (ADT).
  @transient private[this] lazy val lazyAnyRefFF = featureFunctions map {
    case DoubleFeatureFunction(f) => f
    case StringFeatureFunction(f) => f
  }

  @transient private[this] lazy val h2OPredictor: (RowData) => Either[IllConditioned, N] = {
    val retrieval = H2oModelCategory.predictor[N](new EasyPredictModelWrapper(h2OModel))
    H2oModel.mapRetrievalError[N](retrieval).get
  }

  // Force initialization of lazy vals.
  require(lazyAnyRefFF != null)
  require(h2OPredictor != null)

  override def subvalue(a: A): Subvalue[B, N] = {
    val f = constructFeatures(a)
    if (!f.missingOk)
      failureDueToMissing(f.missing)
    else
      try {
        predict(f)
      }
      catch {
        // We know about this specifically from the H2o documentation.
        case e: PredictUnknownCategoricalLevelException                => handleBadCategorical(e, f)
        case e: IllegalArgumentException if isCategoricalMissing(e, f) => handleMissingCategorical(e, f)
      }
  }

  protected[this] def predict(f: Features[RowData]): Subvalue[B, N] = {
    h2OPredictor(f.features).
      fold(ill => failure(Seq(ill.errorMsg), getMissingVariables(f.missing)),
           s   => success(s, missingVarNames = getMissingVariables(f.missing)))
  }

  /**
    */

  /**
    * ''Attempt'' to determine if a categorical was missing in the h2o model.
    *
    * Currently (3.6.0.3), h2o generated model says: "" when a categorical value is not supplied.
    * This is determined from inspecting the generated H2o model code so it's likely brittle and subject
    * to change but its better than throwing an IllegalArgumentException with no diagnostics information
    * when there is missing data in a categorical variable.
    * @param e exception thrown by h2o
    * @param f the data passed in.
    * @return whether to attempt to recover.  Don't attempt to recover unless a string-based feature appears to
    *         be missing.  This is so that we can diagnose when the model will fail every time.
    */
  protected[this] def isCategoricalMissing(e: IllegalArgumentException, f: Features[RowData]): Boolean =
    if (e.getClass == classOf[IllegalArgumentException] && e.getMessage.toLowerCase.contains("categorical")) {
       val foundSomeMissingString = featureFunctions.view.zipWithIndex.exists {
         case (StringFeatureFunction(sff), i) if f.missing contains sff.specification => true
         case _ => false
       }
      foundSomeMissingString
    }
    else false

  /**
    * Report a problem presumably resulting from a missing categorical variable.
    * @param t the error to be reported.
    * @param f the feature values
    * @return
    */
  protected[this] def handleMissingCategorical(t: IllegalArgumentException, f: Features[RowData]) = {
    val missing = featureFunctions.view.zipWithIndex.collect {
      case (StringFeatureFunction(sff), i) if f.missing.contains(sff.specification) => featureNames(i)
    }

    val prefix = "H2o model may have encountered a missing categorical variable.  Likely features: " + missing.mkString(", ")
    val stackError = t.getStackTrace.headOption.fold(List.empty[String])(s =>
      List("See: " + s.getClassName + "." + s.getMethodName + "(" + s.getFileName + ":" + s.getLineNumber + ")"))

    // TODO: Check this: f.missing.keySet
    failure(prefix :: stackError, getMissingVariables(f.missing))
  }

  protected[this] def handleBadCategorical(e: PredictUnknownCategoricalLevelException, f: Features[RowData]) =
    failure(Seq(s"unknown categorical value ${e.getUnknownLevel} for variable: ${e.getColumnName}"), getMissingVariables(f.missing))

  // TODO: Extract to trait.
  protected[this] def getMissingVariables(missing: Map[String, Seq[String]]): Set[String] =
    missing.flatMap(_._2)(scala.collection.breakOut)

  protected[this] def failureDueToMissing(missing: Map[String, Seq[String]]) =
    failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))

  protected[h2o] def constructFeatures(a: A): Features[RowData] = {

    // If we are going to err out, allow a linear scan (with repeated work so that we can get richer error
    // diagnostics.  Only include the values where the list of missing accessors variables is not empty.
    def fullMissing(ff: sci.IndexedSeq[GenAggFunc[A, _]]): Map[String, Seq[String]] =
      ff.foldLeft(Map.empty[String, Seq[String]])((missing, f) => f.accessorOutputMissing(a) match {
        case m if m.nonEmpty => missing + (f.specification -> m)
        case _               => missing
      })

    @tailrec def features(i: Int,
                          n: Int,
                          rowData: RowData,
                          missing: Map[String, Seq[String]],
                          ff: sci.IndexedSeq[GenAggFunc[A, Option[AnyRef]]]): Features[RowData] =
      if (i >= n) {
        val numMissingOk = numMissingThreshold.fold(true)(missing.size <= _)
        val m = if (numMissingOk) missing else fullMissing(ff)
        Features(rowData, m, numMissingOk)
      }
      else ff(i)(a) match {
        case Some(x) => features(i + 1, n, rowData + (featureNames(i), x), missing, ff)
        case None    => features(i + 1, n, rowData, missing + (ff(i).specification -> ff(i).accessorOutputMissing(a)), ff)
      }

    // Store lazyAnyRefFF to a local variable to avoid the repeated cost of asking for the lazy val.
    val ff = lazyAnyRefFF
    features(0, ff.size, new RowData, Map.empty, ff)
  }
}

/**
 * {{{
 *
 * {
 *   "modelType": "H2o",
 *   "modelId": { "id": 0, "name": "" },
 *   "features": {
 *     "Gender": "gender.toString"
 *   },
 *   "model": "b64 encoded model"
 * }
 *
 * }}}
 *
 * {{{
 * {
 *   "modelType": "H2o",
 *   "modelId": { "id": 0, "name": "" },
 *   "features": {
 *     "Gender": "gender.toString"
 *   },
 *   "modelUrl": "hdfs://asdf",
 *   "via": "vfs1"
 * }
 * }}}
 */


object H2oModel extends ParserProviderCompanion
                   with Logging {

  protected[h2o] case class Features[F](features: F,
                                        missing: Map[String, Seq[String]] = Map.empty,
                                        missingOk: Boolean = true)

  override def parser: ModelParser = Parser

  object Parser extends ModelSubmodelParsingPlugin
                   with EitherHelpers { self =>

    val modelType = "H2o"

    private[this] def features[A](featureMap: Seq[(String, H2oSpec)], semantics: Semantics[A]) =
      mapSeq(featureMap) { case (k, s) =>
        s.compile(semantics).
          left.map(Seq(s"Error processing spec '${s.spec}'") ++ _).
          right.map(v => (k, v))
      }

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[H2oModel[U, N, A, B]]] = {

      Some(new JsonReader[H2oModel[U, N, A, B]] {
        override def read(json: JsValue): H2oModel[U, N, A, B] = {
          val h2o = json.convertTo[H2oAst]
          val classCacheDir = getClassCacheDir(semantics)

          features(h2o.features.toSeq, semantics) match {
            case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
            case Right(featureMap) =>
              val (names, functions) = featureMap.toIndexedSeq.unzip
              val genModel = getGenModelFromFile(h2o.modelSource, classCacheDir)
              H2oModel(h2o.modelId,
                genModel,
                names,
                functions,
                auditor,
                h2o.numMissingThreshold)
          }
        }
      })
    }
  }

  protected[h2o] def getClassCacheDir[A](semantics: Semantics[A]): Option[File] = {
    semantics match {
      case c: CompiledSemantics[A] => c.compiler match {
        case t: TwitterEvalCompiler => t.classCacheDir
        case _ => None
      }
      case _ => None
    }
  }

  protected[h2o] def mapRetrievalError[N: RefInfo](retrieval: Either[PredictionFuncRetrievalError, RowData => Either[IllConditioned, N]]) = retrieval match {
    case Right(f) => Success(f)
    case Left(UnsupportedModelCategory(category)) => Failure(new UnsupportedOperationException(s"In model ${classOf[H2oModel[_, _, _, _]].getCanonicalName}: ModelCategory ${category.name} non supported."))
    case Left(TypeCoercionNotFound(category)) => Failure(new IllegalArgumentException(s"In model ${classOf[H2oModel[_, _, _, _]].getCanonicalName}: Could not ${category.name} model to Aloha output type: ${RefInfoOps.toString[N]}."))
  }

  private[h2o] def getJar(collectFn: URL => Boolean): Array[File] =
    currentClassLoader match {
      case urlClassLoader: URLClassLoader => urlClassLoader.getURLs.flatMap { url =>
        // The File constructor can throw if the URL does not reference a local file.  This might be possible if
        // running in some kind of applet.
        if (collectFn(url)) Try(new File(url.toURI)).toOption
        else None
      }
      case _ => Array.empty[File]
    }

  // This guarantees that the h2oGenModelName property used below is guaranteed to be in sync with the maven artifact
  // dependency defined in the POM. This is because the h2o.properties is a filtered resource, meaning maven injects
  // values from the build into the properties file at build time.
  private[h2o] lazy val h2oProps = {
    val stream = getClass.getClassLoader.getResourceAsStream("h2o.properties")
    try {
      val p = new Properties
      p.load(stream)
      p
    }
    finally stream.close()
  }

  private[this] lazy val currentClassLoader = Thread.currentThread().getContextClassLoader

  protected[h2o] def getGenModel[B, C](input: => C,
                                       f: AlohaReadable[Try[GenModel]] => C => Try[GenModel],
                                       classCacheDir: Option[File]) = {
    // It turns out that running Aloha under some environments has class loader issues.  Specifically when compiling an
    // H2O model within Jetty this has proven to fail.  Because Jetty has its own classloader the h2o-genmodel jar is
    // not available in the System's classloader, however, it is available in the thread's local classloader.
    //
    // This has been proven to work within a Jetty environment.
    val h2oGenModelJarName = h2oProps.getProperty("h2oGenModelName")
    val h2oGenModelJar = getJar((url: URL) => url.toString.contains(h2oGenModelJarName))
    val compiler = new Compiler[GenModel](currentClassLoader, h2oGenModelJar, classCacheDir)
    f(compiler)(input)
  }

  private[this] def getGenModelFromFile[B](modelSource: ModelSource, classCacheDir: Option[File]): GenModel = {
    val sourceFile = new java.io.File(modelSource.localVfs.descriptor)
    val p = getGenModel(sourceFile, _.fromFile, classCacheDir).get
    if (modelSource.shouldDelete)
      Try[Unit] { sourceFile.delete() }
    p
  }

  @throws(classOf[IllegalArgumentException])
  def json(spec: Vfs,
           model: Vfs,
           id: ModelId,
           responseColumn: Option[String] = None,
           externalModel: Boolean = false,
           numMissingThreshold: Option[Int] = None,
           notes: Option[Seq[String]] = None): JsValue = {
    val modelSource = getModelSource(model, externalModel)
    val features = getFeatures(spec, responseColumn)
    json(spec.toString, features, modelSource, id, numMissingThreshold, notes)
  }

  @throws(classOf[IllegalArgumentException])
  def json(spec: String,
           model: String,
           id: ModelId,
           responseColumn: Option[String],
           numMissingThreshold: Option[Int],
           notes: Option[Seq[String]]): JsValue = {
    val modelSource = getLocalSource(model.getBytes)
    val features = getFeatures(spec.parseJson.asJsObject, responseColumn)
    json(spec, features, modelSource, id, numMissingThreshold, notes)
  }

  /**
    *
    * @param spec
    * @param modelSource
    * @param id
    * @param numMissingThreshold
    * @param notes
    * @return
    */
  @throws(classOf[IllegalArgumentException])
  def json(spec: String,
           features: Option[ListMap[String, H2oSpec]],
           modelSource: ModelSource,
           id: ModelId,
           numMissingThreshold: Option[Int],
           notes: Option[Seq[String]]): JsValue = {
    val notesList = notes filter {_.nonEmpty}

    features.map { fs =>
      val ast = H2oAst(H2oModel.parser.modelType, id, modelSource, fs, numMissingThreshold, notesList)
      ast.toJson
    } getOrElse { throw new IllegalArgumentException(s"Couldn't get features from $spec.") }
  }

  private[this] def getModelSource(model: Vfs, externalModel: Boolean): ModelSource =
    if (externalModel)
      ExternalSource(model)
    else getLocalSource(model.asByteArray())

  private[this] def getLocalSource(modelBytes: Array[Byte]) =
    Base64StringSource(new String(Base64.encodeBase64(modelBytes)))

  private[this] def getFeatures(spec: Vfs, responseColumn: Option[String]): Option[ListMap[String, H2oSpec]] = {
    getFeatures(spec.asString().parseJson.asJsObject, responseColumn)
  }

  private[this] def getFeatures(spec: JsObject, responseColumn: Option[String]): Option[ListMap[String, H2oSpec]] = {
    spec.getFields("features") match {
      case Seq(JsArray(fs)) =>

        // Note that the it is being assumed that an H2oSpec cannot be instantiated for the response column
        // hence we cannot call f.convertTo[H2oSpec] on the response column.
        def convertToSpec(f: JsValue) = {
          val s = f.convertTo[H2oSpec]
          (s.name, s)
        }
        val features = responseColumn.fold(fs.map(convertToSpec)){ r =>
          fs.collect { case f if r != f.asJsObject.fields("name").convertTo[String] => convertToSpec(f)}
        }
        Some(ListMap(features:_*))
      case _ => None
    }
  }
}
