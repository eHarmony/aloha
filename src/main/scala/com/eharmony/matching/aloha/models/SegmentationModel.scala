package com.eharmony.matching.aloha.models

import scala.collection.immutable

import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics

/** A model that runs the subModel and returns the label associated with the segment in which the inner model's
  * score falls.  This is done via a linear scan of the thresholds.
  * @param modelId a model identifier
  * @param subModel a sub model
  * @param thresholds a sequence of ordered thresholds against which
  * @param labels a set of labesl to use
  * @param bOrd an implicit ordering
  * @tparam A model input type
  * @tparam B output type of the inner model.
  * @tparam C model output type
  */
case class SegmentationModel[A, B, C](
        modelId: ModelIdentity,
        subModel: Model[A, B],
        thresholds: immutable.IndexedSeq[B],
        labels: immutable.IndexedSeq[C])(implicit bOrd: Ordering[B], scB: ScoreConverter[B], scC: ScoreConverter[C])
    extends Model[A, C] {

    require(thresholds.size + 1 == labels.size, s"thresholds size (${thresholds.size}}) should be one less than labels size (${labels.size}})")
    require(thresholds == thresholds.sorted, s"thresholds must be sorted. Found ${thresholds.mkString(", ")}")


    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean) = {
        val (mo, os) = subModel.getScore(a)

        val smo = mo.right.map {v => thresholds.indexWhere(bOrd.lteq(v, _)) match {
            case -1 => labels.last
            case i => labels(i)
        }}

        val s = smo.fold({case (e, m) => failure(e, m, os)}, sc => success(score = sc, subScores = os))

        s
    }
}

object SegmentationModel extends ParserProviderCompanion {

    object Parser extends ModelParser {
        val modelType = "Segmentation"

        import spray.json._, DefaultJsonProtocol._

        protected[this] case class Ast[C: JsonReader: ScoreConverter](subModel: JsValue, subModelOutputType: String, thresholds: JsValue, labels: immutable.IndexedSeq[C]) {
            def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity)(implicit jf: JsonFormat[B], sc: ScoreConverter[B], o: Ordering[B]) = {
                val m = factory.getModel(subModel, Option(semantics))(semantics.refInfoA, sc.ri, jf, sc).get
                val t = thresholds.convertTo[immutable.IndexedSeq[B]]
                SegmentationModel(modelId, m, t, labels)(o, sc, implicitly[ScoreConverter[C]])
            }
        }

        protected[this] def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "subModel", "subModelOutputType", "thresholds", "labels")

        // This is a very slightly modified copy of the lift from Additional formats that removes the type bound.
        protected[this] def lift[A](reader :JsonReader[A]) = new JsonFormat[A] {
            def write(a: A): JsValue = throw new UnsupportedOperationException("No JsonWriter[" + a.getClass + "] available")
            def read(value: JsValue) = reader.read(value)
        }

        /**
         * @param factory ModelFactory[Model[_, _] ]
         * @tparam A model input type
         * @tparam B model input type
         * @return
         */
        def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])(implicit jr: JsonReader[B], sc: ScoreConverter[B]) =  new JsonReader[SegmentationModel[A, _, B]] {
            def read(json: JsValue): SegmentationModel[A, _, B] = {
                // TODO: Make this way better and way more generalized so that it can be used in the ensemble code.
                val mId = getModelId(json).get
                val ast = json.convertTo(astJsonFormat(lift(jr), sc))

                import ScoreConverter.Implicits._

                val model = ast.subModelOutputType match {
                    case "Byte" =>   ast.createModel[A, Byte](factory, semantics.get, mId)
                    case "Short" =>  ast.createModel[A, Short](factory, semantics.get, mId)
                    case "Int" =>    ast.createModel[A, Int](factory, semantics.get, mId)
                    case "Long" =>   ast.createModel[A, Long](factory, semantics.get, mId)
                    case "Float" =>  ast.createModel[A, Float](factory, semantics.get, mId)
                    case "Double" => ast.createModel[A, Double](factory, semantics.get, mId)
                    case "String" => ast.createModel[A, String](factory, semantics.get, mId)
                    case t =>        throw new DeserializationException(s"Unsupported sub-model output type: $t")
                }

                model
            }
        }
    }

    def parser: ModelParser = Parser
}
