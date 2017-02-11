package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.factory.ex.AlohaFactoryException
import com.eharmony.aloha.factory.ri2ord.RefInfoToOrdering
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

/** A model that runs the subModel and returns the label associated with the segment in which the inner model's
  * score falls.  This is done via a linear scan of the thresholds.
  * @param modelId a model identifier
  * @param submodel a sub model
  * @param thresholds a sequence of ordered thresholds against which
  * @param labels a set of labesl to use
  * @param thresholdOrdering an implicit ordering
  * @tparam U upper type bound for output of model and all submodels.
  * @tparam SN submodel's natural type
  * @tparam N segmentation model's natural type
  * @tparam A the model input type
  * @tparam B the model's ultimate output type
  */
case class SegmentationModel[U, SN, N, A, B <: U](
    modelId: ModelIdentity,
    submodel: Submodel[SN, A, U],
    thresholds: immutable.IndexedSeq[SN],
    labels: immutable.IndexedSeq[N],
    auditor: Auditor[U, N, B])(implicit thresholdOrdering: Ordering[SN])
extends SubmodelBase[U, N, A, B] {

  require(thresholds.size + 1 == labels.size, s"thresholds size (${thresholds.size}}) should be one less than labels size (${labels.size}})")
  require(thresholds == thresholds.sorted, s"thresholds must be sorted. Found ${thresholds.mkString(", ")}")

  def subvalue(a: A): Subvalue[B, N] = {
    val s: Subvalue[U, SN] = submodel.subvalue(a)

    s.natural map { sn =>
      val n = thresholds.indexWhere(t => thresholdOrdering.lteq(sn, t)) match {
        case -1 => labels.last
        case i => labels(i)
      }
      success(n, subvalues = Seq(s.audited))
    } getOrElse {
      failure(Seq("Couldn't segment value because submodel failed"), Set.empty, Seq(s.audited))
    }
  }

  override def close(): Unit = submodel.close()
}

object SegmentationModel extends ParserProviderCompanion {

  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "Segmentation"

    import spray.json._
    import DefaultJsonProtocol._

    protected[this] case class Ast[N: JsonReader](
        subModel: JsValue,
        subModelOutputType: String,
        thresholds: JsValue,
        labels: immutable.IndexedSeq[N])

    protected[this] implicit def astJsonFormat[N: JsonFormat]: JsonFormat[Ast[N]] =
      jsonFormat(Ast.apply[N], "subModel", "subModelOutputType", "thresholds", "labels")

    protected[this] def riFromString(riStr: String): Try[RefInfo[_]] =
      RefInfo.fromString(riStr).fold(
        err => Failure(new DeserializationException(s"Unsupported sub-model output type: '$riStr'")),
        ri => Success(ri)
      )

    protected[this] def getOrdering[SN: RefInfo]: Try[Ordering[SN]] =
      RefInfoToOrdering[SN].map(o => Success(o)).getOrElse {
        Failure(new AlohaFactoryException(s"Couldn't find Ordering[${RefInfoOps.toString[SN]}]."))
      }

    protected[this] def getJsonFormat[SN: RefInfo, A, U](factory: SubmodelFactory[U, A]): Try[JsonFormat[SN]] =
      factory.jsonFormat[SN].
        map { Success.apply }.
        getOrElse { Failure(new AlohaFactoryException(s"Could find JsonFormat[${RefInfoOps.toString[SN]}].")) }

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[SegmentationModel[U, _, N, A, B]]] = {
      Some(new JsonReader[SegmentationModel[U, _, N, A, B]] {
        override def read(json: JsValue): SegmentationModel[U, _, N, A, B] = {
          val mId = getModelId(json).get
          val ast = json.convertTo[Ast[N]]

          val m = for {
            risn <- riFromString(ast.subModelOutputType)
            jfsn <- getJsonFormat(factory)(risn)
            osn <- getOrdering(risn)
            submodel <- factory.submodel(ast.subModel)(risn)
          } yield {
            val thresholds = ast.thresholds.convertTo(DefaultJsonProtocol.immIndexedSeqFormat(jfsn))
            val o = osn.asInstanceOf[Ordering[Any]]
            SegmentationModel(mId, submodel, thresholds, ast.labels, auditor)(o)
          }
          m.get
        }
      })
    }
  }

  def parser: ModelParser = Parser

//    object Parser extends ModelParser {
//        val modelType = "Segmentation"
//
//        import spray.json._, DefaultJsonProtocol._
//
//        protected[this] case class Ast[C: JsonReader: ScoreConverter](subModel: JsValue, subModelOutputType: String, thresholds: JsValue, labels: immutable.IndexedSeq[C]) {
//            def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity)(implicit jf: JsonFormat[B], sc: ScoreConverter[B], o: Ordering[B]) = {
//                val m = factory.getModel(subModel, Option(semantics))(semantics.refInfoA, sc.ri, jf, sc).get
//                val t = thresholds.convertTo[immutable.IndexedSeq[B]]
//                SegmentationModel(modelId, m, t, labels)(o, sc, implicitly[ScoreConverter[C]])
//            }
//        }
//
//        // TODO: Remove commented code after getting SBT build working.
////        protected[this] implicit def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "subModel", "subModelOutputType", "thresholds", "labels")
//        protected[this] def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "subModel", "subModelOutputType", "thresholds", "labels")
//
//        /**
//         * @param factory ModelFactory[Model[_, _] ]
//         * @tparam A model input type
//         * @tparam B model input type
//         * @return
//         */
//        def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])(implicit jr: JsonReader[B], sc: ScoreConverter[B]) =  new JsonReader[SegmentationModel[A, _, B]] {
//            def read(json: JsValue): SegmentationModel[A, _, B] = {
//                import com.eharmony.aloha.factory.ScalaJsonFormats.lift
//
//                // TODO: Make this way better and way more generalized so that it can be used in the ensemble code.
//                val mId = getModelId(json).get
//                val ast = json.convertTo[Ast[B]](astJsonFormat(lift(jr), sc))
//
//                import ScoreConverter.Implicits._
//
//                val model = ast.subModelOutputType match {
//                    case "Byte" =>   ast.createModel[A, Byte](factory, semantics.get, mId)
//                    case "Short" =>  ast.createModel[A, Short](factory, semantics.get, mId)
//                    case "Int" =>    ast.createModel[A, Int](factory, semantics.get, mId)
//                    case "Long" =>   ast.createModel[A, Long](factory, semantics.get, mId)
//                    case "Float" =>  ast.createModel[A, Float](factory, semantics.get, mId)
//                    case "Double" => ast.createModel[A, Double](factory, semantics.get, mId)
//                    case "String" => ast.createModel[A, String](factory, semantics.get, mId)
//                    case t =>        throw new DeserializationException(s"Unsupported sub-model output type: $t")
//                }
//
//                model
//            }
//        }
//    }

}
