package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.tree.Tree
import com.eharmony.aloha.models.{SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.{JsValue, JsonFormat, JsonReader}

import scala.collection.immutable

/** A decision tree whose node values are the values returned by this model.
  * @param modelId An id with which to identify this model
  * @param root the root node of the decision tree
  * @param returnBest if no path from the root to a leaf can be generated for a given input, should we return a score
  *                   associated with an interior node?
  * @tparam A model input type
  * @tparam B model output type
  */
case class BasicDecisionTree[U, N, -A, +B <: U](
    modelId: ModelIdentity,
    root: Node[A, N],
    returnBest: Boolean,
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, A, B] {

  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  def subvalue(a: A): Subvalue[B, N] = {
    // Find the proper node.
    val n = root.getNode(a)

    // Deal with internal nodes and leaves differently.  A leaf indicates a success. An internal node can
    // indicate either a partial success or a failure, depending on configuration.
    val r = n.fold(interior => scoreFailure(interior), leaf => success(leaf.value))
    r
  }

  /** Process an interior node result.  Not proceding to the leaf in a decision tree typically indicates a problem.
    * This function determines whether we should produce a compromised result or just produce an error.
    * @param interior interior node and failure information
    * @return
    */
  private[this] def scoreFailure(interior: InteriorNodeResult[A, N]): Subvalue[B, N] =
    if (returnBest)
      success(interior.node.value, missingVarNames = interior.missing.toSet)
    else failure(interior.errors, interior.missing.toSet)
}

object BasicDecisionTree extends ParserProviderCompanion {

  object Parser
    extends ModelSubmodelParsingPlugin
//      with EitherHelpers
      with DecisionTreeJson {

    val modelType = "DecisionTree"

    /**
      *
      * @param semantics This reader requires semantics to be provided (some).  Otherwise, an error will occur. This
      *                  is because the regression models create functions for each feature in the model and
      *                  function creation is performed by the semantics.
      * @tparam A input type of the model
      * @tparam B output type of the model
      * @return
      */
    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[BasicDecisionTree[U, N, A, B]]] = {

      Some(new JsonReader[BasicDecisionTree[U, N, A, B]] {
        override def read(json: JsValue): BasicDecisionTree[U, N, A, B] = {
          val dtAst = json.convertTo(decisionTreeAstJsonFormat[N])
          val mId = getModelId(json)
          val t = Tree[NodeAst[N], immutable.IndexedSeq, Node[A, N]](
            dtAst.nodes, root, id, childIds, treeBuilder[A, N](semantics, dtAst.missingDataOk))
          val dt = BasicDecisionTree(mId.get, t, dtAst.returnBest, auditor)
          dt
        }
      })
    }
  }

  def parser: ModelParser = Parser
}

