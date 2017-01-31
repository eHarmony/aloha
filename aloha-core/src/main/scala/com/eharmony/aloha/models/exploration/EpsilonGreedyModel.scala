package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{Submodel, SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.mwt.explorers.EpsilonGreedyExplorer
import com.mwt.policies.Policy
import spray.json.DefaultJsonProtocol.{FloatJsonFormat, JsValueFormat, StringJsonFormat, immIndexedSeqFormat, jsonFormat4}
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader, RootJsonFormat}

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
  * or an action from the defaultPolicy with probability 1 - epsilon.  Note that the
  * default policy MUST return a value between 1 and the number of actions, and if not an exception will be thrown.
  * @param modelId a model identifier
  * @param defaultPolicy the model to use for exploitation.  This MUST be deterministic for the probability to be correct.
  *                      The model must return a value in the range 1 to `classLabels.size` (inclusive).
  * @param epsilon the exploration/exploitation tradeoff parameter.  epsilon must be in the interval [0, 1].
  *                0 indicates never select an action randomly.
  *                1 indicates always select an action randomly.
  * @param salt a function that generates a salt for the randomization layer.  This salt allows the random choice of which policy
  *             to follow to be repeatable.
  * @param classLabels a list of class labels to output for the final type.  Also note that the size of this controls the
  *                    number of actions.  If the submodel returns a score < 1 or > classLabels.size (note the 1 offset)
  *                    then a RuntimeException will be thrown.
  * @tparam A model input type
  * @tparam B model output type
  */
case class EpsilonGreedyModel[U, N, -A, B <: U](
    modelId: ModelIdentity,
    defaultPolicy: Submodel[Int, A, U],
    epsilon: Float,
    salt: GenAggFunc[A, Long],
    classLabels: sci.IndexedSeq[N],
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, A, B] {

  @transient lazy val explorer = new EpsilonGreedyExplorer(ModelPolicy, epsilon, classLabels.size)

  override def subvalue(a: A): Subvalue[B, N] = {
    val s = defaultPolicy.subvalue(a)

    s.natural.fold(failure(Seq(), Set.empty, Seq(s.audited))){ n =>
      val decision = explorer.chooseAction(salt(a), n)
      val action = decision.getAction

      // We only want to add the subscore of the default policy if that policy returns the
      // same action as the one chosen by the explorer.  This allows the differentiation of the explore
      // and exploit groups using the subscores.
      success(
        classLabels(action - 1),
        subvalues = if (n == action) Seq(s.audited) else Nil,
        prob = Option(decision.getProbability)
      )
    }
  }

  override def close(): Unit = defaultPolicy.close()
}

object EpsilonGreedyModel extends ParserProviderCompanion {

  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "EpsilonGreedyExploration"

    protected[this] case class Ast[N: JsonReader](
        defaultPolicy: JsValue,
        epsilon: Float,
        salt: String,
        classLabels: sci.IndexedSeq[N])

    protected[this] implicit def astJsonFormat[N: JsonFormat]: RootJsonFormat[Ast[N]] = jsonFormat4(Ast.apply[N])

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[EpsilonGreedyModel[U, N, A, B]]] = {
      Some(new JsonReader[EpsilonGreedyModel[U, N, A, B]] {
        override def read(json: JsValue): EpsilonGreedyModel[U, N, A, B] = {
          val mId = getModelId(json).get
          val ast = json.convertTo[Ast[N]]

          val m = factory.submodel[Int](ast.defaultPolicy).get
          val saltFn = semantics.createFunction[Long](ast.salt).
                                 fold(l => throw new DeserializationException(l.mkString("\n")), identity)

          EpsilonGreedyModel(mId, m, ast.epsilon, saltFn, ast.classLabels, auditor)
        }
      })
    }
  }

  override def parser: NewModelParser = Parser


  //  object Parser extends ModelParser {
//    val modelType = "EpsilonGreedyExploration"
//
//    import spray.json._
//    import DefaultJsonProtocol._
//
//    protected[this] case class Ast[B: JsonReader: ScoreConverter](defaultPolicy: JsValue, epsilon: Float, salt: String, classLabels: sci.IndexedSeq[B]) {
//      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
//        val m = factory.getModel(defaultPolicy, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get
//        val saltFunc = semantics.createFunction[Long](salt).fold(l => throw new DeserializationException(l.mkString("\n")), identity)
//        EpsilonGreedyModel(modelId, m, epsilon, saltFunc, classLabels)
//      }
//    }
//
//    protected[this] def astJsonFormat[N: JsonFormat]: RootJsonFormat[Ast[N]] =
//      jsonFormat(Ast.apply[N], "defaultPolicy", "epsilon", "salt", "classLabels")
//
//    /**
//      * @param factory ModelFactory[Model[_, _] ]
//      * @tparam A model input type
//      * @tparam B model input type
//      * @return
//      */
//    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
//      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[EpsilonGreedyModel[A, B]] {
//      def read(json: JsValue): EpsilonGreedyModel[A, B] = {
//        import com.eharmony.aloha.factory.ScalaJsonFormats.lift
//
//        val mId = getModelId(json).get
//        val ast = json.convertTo[Ast[B]](astJsonFormat(lift(jr), sc))
//
//        val model = ast.createModel[A, B](factory, semantics.get, mId)
//
//        model
//      }
//    }
//  }

}
