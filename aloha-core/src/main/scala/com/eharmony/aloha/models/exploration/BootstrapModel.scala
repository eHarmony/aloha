package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{Submodel, SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.mwt.explorers.BootstrapExplorer
import com.mwt.policies.Policy
import spray.json.DefaultJsonProtocol.{jsonFormat3, immIndexedSeqFormat, JsValueFormat, StringJsonFormat}
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader, RootJsonFormat}

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
  * @tparam A model input type
  * @tparam B model output type
  */
case class BootstrapModel[U, N, A, B <: U](
    modelId: ModelIdentity,
    models: sci.IndexedSeq[Submodel[Int, A, U]],
    salt: GenAggFunc[A, Long],
    classLabels: sci.IndexedSeq[N],
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, A, B] {

  @transient private[this] lazy val explorer = new BootstrapExplorer[sci.IndexedSeq[Int]](
    models.indices.map(i => NumberedPolicy(i): Policy[sci.IndexedSeq[Int]]),
    classLabels.size
  )

  override def subvalue(a: A): Subvalue[B, N] = combineScores(a, models)

  /**
    * We want to get either one failure or aggregate all the successes.  This will do that and then return the correct
    * type.
    * @param a the item to score
    * @param models the list of policies to evaluate a for
    * @param subvalues all the scores generated so far
    * @param successfulActions all the successfully generated submodel actions
    * @return a final success if all the models evaluated properly or else a failure as soon as one fails
    */
  @tailrec private[this] def combineScores(
      a: A,
      models: sci.IndexedSeq[Submodel[Int, A, U]],
      subvalues: sci.IndexedSeq[U] = Vector.empty,
      successfulActions: sci.IndexedSeq[Int] = Vector.empty): Subvalue[B, N] = {

    // If models is empty then all models returned success.
    if (models.isEmpty) {
      val decision = explorer.chooseAction(salt(a), successfulActions)
      val action = decision.getAction

      // We want to return only those subscores that contributed to the chosen action.  Hence
      // we're going to filter out those successes (in this case actions) that are not the same
      // as the action chosen by the explorer.
      success(
        naturalValue = classLabels(action - 1),
        subvalues = subvalues.zip(successfulActions).collect{ case (ss, sa) if sa == action => ss },
        prob = Option(decision.getProbability)
      )
    }
    else {
      val s = models.head.subvalue(a)
      s.natural match {
        case Some(act) => combineScores(a, models.tail, subvalues :+ s.audited, successfulActions :+ act)
        case None =>
          // Short-circuit on submodels that can't produce a score.
          failure(
            Seq(s"Bootstrap model failed becauase ${models.head.modelId} failed."),
            Set.empty,
            subvalues)
      }
    }
  }

  override def close(): Unit = models.foreach(_.close())
}

object BootstrapModel extends ParserProviderCompanion {

  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "BootstrapExploration"

    protected[this] case class Ast[N: JsonReader](
        policies: sci.IndexedSeq[JsValue],
        salt: String,
        classLabels: sci.IndexedSeq[N])

    protected[this] implicit def astJsonFormat[N: JsonFormat]: RootJsonFormat[Ast[N]] =
      jsonFormat3(Ast.apply[N]) // , "policies", "salt", "classLabels")


    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[BootstrapModel[U, N, A, B]]] = {
      Some(new JsonReader[BootstrapModel[U, N, A, B]] {
        override def read(json: JsValue): BootstrapModel[U, N, A, B] = {
          val mId = getModelId(json).get
          val ast = json.convertTo[Ast[N]]

          // TODO: Determine if these should these be handled more gracefully.  See ModelDecisionTree.
          // TODO: Create common code: def submodels[N: RefInfo](s: Seq[JsValue]): Try[Seq[Submodel[N, A, U]]]
          val models = ast.policies.map(p => factory.submodel[Int](p).get)
          val saltFn = semantics.createFunction[Long](ast.salt).
                                 fold(l => throw new DeserializationException(l.mkString("\n")), identity)

          BootstrapModel(mId, models, saltFn, ast.classLabels, auditor)
        }
      })
    }
  }

  override def parser: NewModelParser = Parser


//
//  object Parser extends ModelParser {
//    val modelType = "BootstrapExploration"
//
//    import spray.json._
//    import DefaultJsonProtocol._
//
//    protected[this] case class Ast[B: JsonReader: ScoreConverter](policies: sci.IndexedSeq[JsValue], salt: String, classLabels: sci.IndexedSeq[B]) {
//      def createModel[A, B](factory: ModelFactory, semantics: Semantics[A], modelId: ModelIdentity) = {
//        val models = policies.map(factory.getModel(_, Option(semantics))(semantics.refInfoA, IntScoreConverter.ri, IntJsonFormat, IntScoreConverter).get)
//        val saltFunc = semantics.createFunction[Long](salt).fold(l => throw new DeserializationException(l.mkString("\n")), identity)
//        BootstrapModel(modelId, models, saltFunc, classLabels)
//      }
//    }
//
//    protected[this] def astJsonFormat[B: JsonFormat: ScoreConverter]: RootJsonFormat[Ast[B]] = jsonFormat(Ast.apply[B], "policies", "salt", "classLabels")
//
//    /**
//      * @param factory ModelFactory[Model[_, _] ]
//      * @tparam A model input type
//      * @tparam B model input type
//      * @return
//      */
//    def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])
//      (implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[BootstrapModel[A, B]] {
//      def read(json: JsValue): BootstrapModel[A, B] = {
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
