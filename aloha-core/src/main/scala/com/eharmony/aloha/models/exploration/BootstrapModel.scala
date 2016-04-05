package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{BaseModel, Model}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.IntScore
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.mwt.explorers.BootstrapExplorer
import com.mwt.policies.Policy

import scala.annotation.tailrec
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.{immutable => sci}

/**
  * Since explore-java has chosen to force the Policy to evaluate we can't just evaluate the model inside of the policy.
  * We have to invert the control and evaluate the models first, and only upon all models successfully evaluating
  * can we then do the policy evaluation.  Because of this IOC the policy becomes a simple lookup into the model map.
  * @param index the model to get the action for
  */
private[this] case class NumberedPolicy(index: Int) extends Policy[sci.IndexedSeq[Int]] {
  override def chooseAction(actions: sci.IndexedSeq[Int]): Int = actions(index)
}

/**
  * A model for performing bootstrap style exploration.  This makes use of a number of policies.  The algorithm chooses
  * one policy and then uses the other to calculate the appropriate probability of choosing that action.  Note that the
  * models MUST return a value between 1 and the number of actions, and if not an exception will be thrown.
  * @param modelId a model identifier
  * @param models a set of models that generate Int's.  These models MUST be deterministic for the probability to be correct.
  *               Each model must return a value in the range 1 to `classLabels.size` (inclusive).
  * @param salt a function that generates a salt for the randomization layer.  This salt allows the random choice of which policy
  *             to follow to be repeatable.
  * @param classLabels a list of class labels to output for the final type.  Also note that the size of this controls the
  *                    number of actions.  If the submodel returns a score < 1 or > classLabels.size (note the 1 offset)
  *                    then a RuntimeException will be thrown.
  * @param scB a score context for B
  * @tparam A model input type
  * @tparam B model output type
  */
case class BootstrapModel[A, B](
  modelId: ModelIdentity,
  models: sci.IndexedSeq[Model[A, Int]],
  salt: GenAggFunc[A, Long],
  classLabels: sci.IndexedSeq[B])(implicit scB: ScoreConverter[B]) extends BaseModel[A, B] {

  @transient private[this] lazy val explorer = new BootstrapExplorer[sci.IndexedSeq[Int]](
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
  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = combineScores(a, models)

  /**
    * We want to get either one failure or aggregate all the successes.  This will do that and then return the correct
    * type.
    * @param a the item to score
    * @param models the list of policies to evaluate a for
    * @param subScores all the scores generated so far
    * @param successes all the successes generated so far
    * @return a final success if all the models evaluated properly or else a failure as soon as one fails
    */
  @tailrec private[this] def combineScores(
    a: A,
    models: sci.IndexedSeq[Model[A, Int]],
    subScores: Seq[Score] = Seq.empty,
    successes: sci.IndexedSeq[Int] = sci.IndexedSeq.empty)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    if (models.isEmpty) {
      val decision = explorer.chooseAction(salt(a), successes)
      success(
        score = classLabels(decision.getAction - 1),
        subScores = subScores.filter(_.getScore.getExtension(IntScore.impl) == classLabels(decision.getAction - 1)),
        probability = Option(decision.getProbability)
      )
    }
    else {
      val (mo, os) = models.head.getScore(a)
      val newScores = os.fold(subScores)(subScores :+ _)
      mo match {
        case Left((e, m)) => failure(e, m, newScores)
        case Right(s)     => combineScores(a, models.tail, newScores, successes :+ s)
      }
    }
  }

  override def close() = models.foreach(_.close())
}

object BootstrapModel extends ParserProviderCompanion {

  object Parser extends ModelParser {
    val modelType = "BootstrapExploration"

    import spray.json._
    import DefaultJsonProtocol._

    protected[this] case class Ast[B: JsonReader: ScoreConverter](policies: sci.IndexedSeq[JsValue], salt: String, classLabels: sci.IndexedSeq[B]) {
      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
        val models = policies.map(factory.getModel(_, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get)
        val saltFunc = semantics.createFunction[Long](salt).fold(l => throw new DeserializationException(l.mkString("\n")), identity)
        BootstrapModel(modelId, models, saltFunc, classLabels)
      }
    }

    protected[this] implicit def astJsonFormat[B: JsonFormat: ScoreConverter] = jsonFormat(Ast.apply[B], "policies", "salt", "classLabels")

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A model input type
      * @tparam B model input type
      * @return
      */
    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[BootstrapModel[A, B]] {
      def read(json: JsValue): BootstrapModel[A, B] = {
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
