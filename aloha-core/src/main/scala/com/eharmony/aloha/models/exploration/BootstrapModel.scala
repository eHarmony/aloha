package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{Model, BaseModel}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.{ModelFailure, ModelOutput}
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.mwt.explorers.BootstrapExplorer
import com.mwt.policies.Policy

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.{immutable => sci}

case class NumberedPolicy(index: Int) extends Policy[sci.IndexedSeq[Int]] {
  override def chooseAction(actions: sci.IndexedSeq[Int]): Int = actions(index)
}

/**
  * Created by jmorra on 2/26/16.
  */
case class BootstrapModel[A, B](
  modelId: ModelIdentity,
  models: sci.IndexedSeq[Model[A, Int]],
  salt: Long,
  classLabels: sci.IndexedSeq[B])(implicit scB: ScoreConverter[B]) extends BaseModel[A, B] {

  @transient lazy val explorer = new BootstrapExplorer[sci.IndexedSeq[Int]](
    models.indices.map(i => NumberedPolicy(i): Policy[sci.IndexedSeq[Int]]),
    classLabels.size
  )

  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    val mos = models.map(_.getScore(a))

    // TODO This can be done better using ApplicativeFunctors and sequence from scalaz.
    val scores = for {
      m <- mos
      result <- m._1.right.toOption
    } yield result

    // Since we know we have at least 1 failure calling get on the option here is guaranteed to succeed.
    // I know this is bad, the above to do should fix it.
    if (scores.size != mos.size)
      mos.collectFirst{ case (Left(error), os) => failure(error._1, error._2, os) }.get
    else {
      val decision = explorer.chooseAction(salt, scores)

      val s = success(
        score = classLabels(decision.getAction - 1),
        subScores = mos.flatMap(_._2),
        probability = Option(decision.getProbability)
      )
      s
    }
  }

  override def close() = models.foreach(_.close())
}

object BootstrapModel extends ParserProviderCompanion {

  object Parser extends ModelParser {
    val modelType = "BootstrapExploration"

    import spray.json._, DefaultJsonProtocol._

    protected[this] case class Ast[B: JsonReader: ScoreConverter](policies: sci.IndexedSeq[JsValue], salt: Long, classLabels: sci.IndexedSeq[B]) {
      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
        val models = policies.map(factory.getModel(_, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get)
        BootstrapModel(modelId, models, salt, classLabels)
      }
    }

    protected[this] def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "policies", "salt", "classLabels")

    // This is a very slightly modified copy of the lift from Additional formats that removes the type bound.
    protected[this] def lift[A](reader: JsonReader[A]) = new JsonFormat[A] {
      def write(a: A): JsValue = throw new UnsupportedOperationException("No JsonWriter[" + a.getClass + "] available")
      def read(value: JsValue) = reader.read(value)
    }

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A model input type
      * @tparam B model input type
      * @return
      */
    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[BootstrapModel[A, B]] {
      def read(json: JsValue): BootstrapModel[A, B] = {
        val mId = getModelId(json).get
        val ast = json.convertTo(astJsonFormat(lift(jr), sc))

        val model = ast.createModel[A, B](factory, semantics.get, mId)

        model
      }
    }
  }

  override def parser: ModelParser = Parser
}
