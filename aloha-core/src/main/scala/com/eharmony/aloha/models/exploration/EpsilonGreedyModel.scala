package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{BaseModel, Model}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.mwt.explorers.EpsilonGreedyExplorer
import com.mwt.policies.Policy

import scala.collection.{immutable => sci}

/**
  * Since explore-java has chosen to force the Policy to evaluate we can't just evaluate the model inside of the policy.
  * Instead the Policy acts as a pass through for the model which is evaluated externally.  This inversion of control
  * allows the model to fail before evaluating the Policy.
  */
private[this] case object ModelPolicy extends Policy[Int] {
  override def chooseAction(action: Int): Int = action
}

/**
  * A model which does epsilon greedy style exploration.  This will choose a random action with probability epsilon
  * or an action from the defaultPolicy with probability 1 - epsilon.
  * @param modelId a model identifier
  * @param defaultPolicy the model to use for exploitation.  This MUST be deterministic for the probability to be correct.
  * @param epsilon the exploration/exploitation tradeoff parameter
  * @param salt a function that generates a salt for the randomization layer.  This salt allows the random choice of which policy
  *             to follow to be repeatable.
  * @param classLabels a list of class labels to output for the final type.  Also note that the size of this controls the
  *                    number of actions.  If the submodel returns a score < 1 or > classLabels.size (note the 1 offset)
  *                    then a RuntimeException will be thrown.
  * @param scB a score context for B
  * @tparam A model input type
  * @tparam B model output type
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

    import spray.json._
    import DefaultJsonProtocol._

    protected[this] case class Ast[B: JsonReader: ScoreConverter](defaultPolicy: JsValue, epsilon: Float, salt: String, classLabels: sci.IndexedSeq[B]) {
      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
        val m = factory.getModel(defaultPolicy, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get
        val saltFunc = semantics.createFunction[Long](salt).fold(l => throw new DeserializationException(l.mkString("\n")), identity)
        EpsilonGreedyModel(modelId, m, epsilon, saltFunc, classLabels)
      }
    }

    protected[this] implicit def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "defaultPolicy", "epsilon", "salt", "classLabels")

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A model input type
      * @tparam B model input type
      * @return
      */
    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[EpsilonGreedyModel[A, B]] {
      def read(json: JsValue): EpsilonGreedyModel[A, B] = {
        import com.eharmony.aloha.factory.ScalaJsonFormats.lift

        val mId = getModelId(json).get
        val ast = json.convertTo[Ast[B]]

        val model = ast.createModel[A, B](factory, semantics.get, mId)

        model
      }
    }
  }

  override def parser: ModelParser = Parser
}
