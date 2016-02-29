package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{Model, BaseModel}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.mwt.explorers.EpsilonGreedyExplorer
import com.mwt.policies.Policy

import scala.collection.{immutable => sci}

case object ModelPolicy extends Policy[Int] {
  override def chooseAction(action: Int): Int = action
}

/**
  * Created by jmorra on 2/26/16.
  */
case class EpsilonGreedyModel[A, B](
  modelId: ModelIdentity,
  defaultPolicy: Model[A, Int],
  epsilon: Float,
  salt: GenAggFunc[A, Long],
  classLabels: sci.IndexedSeq[B])(implicit scB: ScoreConverter[B]) extends BaseModel[A, B] {

  @transient lazy val explorer = new EpsilonGreedyExplorer(ModelPolicy, epsilon, classLabels.size)

  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    val (mo, os) = defaultPolicy.getScore(a)
    val decision = mo.right.map(explorer.chooseAction(salt(a), _))

    val s = decision.fold(
      {case (e, m) => failure(e, m, os)},
      sc => success(
        score = classLabels(sc.getAction - 1),
        subScores = os,
        probability = Option(sc.getProbability)
      )
    )
    s
  }

  override def close() = defaultPolicy.close()
}

object EpsilonGreedyModel extends ParserProviderCompanion {

  object Parser extends ModelParser {
    val modelType = "EpsilonGreedyExploration"

    import spray.json._, DefaultJsonProtocol._

    protected[this] case class Ast[B: JsonReader: ScoreConverter](defaultPolicy: JsValue, epsilon: Float, salt: String, classLabels: sci.IndexedSeq[B]) {
      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
        val m = factory.getModel(defaultPolicy, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get
        val saltFunc = semantics.createFunction[Long](salt).fold(l => throw new DeserializationException(l.mkString("\n")), identity)
        EpsilonGreedyModel(modelId, m, epsilon, saltFunc, classLabels)
      }
    }

    protected[this] def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "defaultPolicy", "epsilon", "salt", "classLabels")

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A model input type
      * @tparam B model input type
      * @return
      */
    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[EpsilonGreedyModel[A, B]] {
      def read(json: JsValue): EpsilonGreedyModel[A, B] = {
        val mId = getModelId(json).get
        val ast = json.convertTo(astJsonFormat(lift(jr), sc))

        val model = ast.createModel[A, B](factory, semantics.get, mId)

        model
      }
    }
  }

  override def parser: ModelParser = Parser
}
