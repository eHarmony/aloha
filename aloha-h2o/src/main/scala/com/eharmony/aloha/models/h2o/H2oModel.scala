package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.io.AlohaReadable
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.BaseModel
import com.eharmony.aloha.models.h2o.H2oModel.Features
import com.eharmony.aloha.models.h2o.categories._
import com.eharmony.aloha.models.h2o.compiler.Compiler
import com.eharmony.aloha.models.h2o.json.{H2oSpec, H2oAst, H2oModelJson}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging}
import hex.genmodel.GenModel
import hex.genmodel.easy.exception.PredictUnknownCategoricalLevelException
import hex.genmodel.easy.{EasyPredictModelWrapper, RowData}
import spray.json.{DeserializationException, JsValue, JsonReader}

import scala.annotation.tailrec
import scala.collection.{immutable => sci}
import scala.util.{Failure, Success, Try}

/**
 * Created by deak on 9/30/15.
 */
// TODO: Need to make sure the model source is used rather than a model.
//       This is because we want to protected against a user compiling a model locally and serializing it.
//       Then deserializing on a cluster where the class files don't exist.  If we follow the same methodology as
//       the VW model, then we can get the same guarantees.
final case class H2oModel[-A, +B](
    modelId: ModelIdentity,
    modelSource: ModelSource,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[FeatureFunction[A]],
    numMissingThreshold: Option[Int] = None)(implicit private[this] val scb: ScoreConverter[B])
  extends BaseModel[A, B]
     with Logging {

  // Because H2o's RowData object is essentially a Map of String to Object, we unapply the wrapper
  // and throw away the type information on the function return type.  We have type safety because
  // FeatureFunction is sealed (ADT).
  @transient private[this] lazy val lazyAnyRefFF = featureFunctions map {
    case DoubleFeatureFunction(f) => f
    case StringFeatureFunction(f) => f
  }

  @transient private[this] lazy val h2oPredictor: RowData => Either[IllConditioned, B] = {
    val sourceFile = new java.io.File(modelSource.localVfs.descriptor)
    val p = getH2oPredictor(sourceFile, _.fromFile).get
    if (modelSource.shouldDelete)
      Try[Unit] { sourceFile.delete() }
    p
  }

  // Force initialization of lazy vals.
  require(lazyAnyRefFF != null)
  require(h2oPredictor != null)

  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    val f = constructFeatures(a)
    if (!f.missingOk)
      failureDueToMissing(f.missing)
    else
      try {
        predict(f)
      } catch {
        // We know about this specifically from the H2o documentation.
        case e: PredictUnknownCategoricalLevelException => handleBadCategorical(e, f)
      }
  }

  protected[this] def predict(f: Features[RowData])(implicit audit: Boolean) =
    h2oPredictor(f.features).fold(ill => failure(Seq(ill.errorMsg), getMissingVariables(f.missing)),
                                  s   => success(s))

  protected[this] def handleBadCategorical(e: PredictUnknownCategoricalLevelException, f: Features[RowData])(implicit audit: Boolean) =
    failure(Seq(s"unknown categorical value ${e.getUnknownLevel} for variable: ${e.getColumnName}"), getMissingVariables(f.missing))

  // TODO: Extract to trait.
  protected[this] def getMissingVariables(missing: Map[String, Seq[String]]): Seq[String] =
    missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

  protected[this] def failureDueToMissing(missing: Map[String, Seq[String]])(implicit audit: Boolean) =
    failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))

  protected[this] def constructFeatures(a: A): Features[RowData] = {

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

  protected[h2o] def mapRetrievalError[B: RefInfo](genModel: GenModel, retrieval: Either[PredictionFuncRetrievalError, RowData => Either[IllConditioned, B]]) = retrieval match {
    case Right(f) => Success(f)
    case Left(UnsupportedModelCategory(category)) => Failure(new UnsupportedOperationException(s"In model ${genModel.getClass.getCanonicalName}: ModelCategory ${category.name} non supported."))
    case Left(TypeCoercionNotFound(category)) => Failure(new IllegalArgumentException(s"In model ${genModel.getClass.getCanonicalName}: Could not ${category.name} model to Aloha output type: ${RefInfoOps.toString[B]}."))
  }

  protected[h2o] def getH2oPredictor[C](input: => C, f: AlohaReadable[Try[GenModel]] => C => Try[GenModel]) = {
    val compiler = new Compiler[GenModel]
    implicit val rib = scb.ri
    for {
      genModel           <- f(compiler)(input)
      predictorRetrieval = H2oModelCategory.predictor[B](new EasyPredictModelWrapper(genModel))
      predictor          <- mapRetrievalError[B](genModel, predictorRetrieval)
    } yield predictor
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
                   with H2oModelJson
                   with Logging {

  protected[h2o] case class Features[F](features: F,
                                        missing: Map[String, Seq[String]] = Map.empty,
                                        missingOk: Boolean = true)

  override def parser: ModelParser = Parser

  object Parser extends ModelParserWithSemantics with EitherHelpers { self =>
    val modelType = "H2o"

    override def modelJsonReader[A, B](semantics: Semantics[A])(implicit jrB: JsonReader[B], scB: ScoreConverter[B]): JsonReader[H2oModel[A, B]] = new JsonReader[H2oModel[A, B]] {
      override def read(json: JsValue): H2oModel[A, B] = {
        val h2o = json.convertTo[H2oAst]

        features(h2o.features.toSeq, semantics) match {
          case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
          case Right(featureMap) =>
            val (names, functions) = featureMap.toIndexedSeq.unzip
            H2oModel[A, B](h2o.modelId,
              h2o.modelSource,
              names,
              functions,
              h2o.numMissingThreshold)
        }
      }

      // TODO: Copied from RegressionModel.  Refactor for reuse.
      private[this] def features[A](featureMap: Seq[(String, H2oSpec)], semantics: Semantics[A]) =
        mapSeq(featureMap) { case (k, s) =>
          s.compile(semantics).
            left.map(Seq(s"Error processing spec '${s.spec}'") ++ _).
            right.map(v => (k, v))
        }
    }
  }
}
