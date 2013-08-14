package com.eharmony.matching.aloha.models.tree.decision

import scala.collection.immutable

import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.factory.{ModelParser, ParserProviderCompanion, ModelParserWithSemantics}
import com.eharmony.matching.aloha.util.EitherHelpers
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.models.tree.Tree

/** A decision tree whose node values are the values returned by this model.
  * @param modelId An id with which to identify this model
  * @param root the root node of the decision tree
  * @param returnBest if no path from the root to a leaf can be generated for a given input, should we return a score
  *                   associated with an interior node?
  * @tparam A model input type
  * @tparam B model output type
  */
case class BasicDecisionTree[-A, +B: ScoreConverter](
        modelId: ModelIdentity,
        root: Node[A, B],
        returnBest: Boolean)
    extends Model[A, B] {

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
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
      * @param audit whether to audit the data in a Score object.
      * @return
      */
    private[this] def scoreFailure(interior: InteriorNodeResult[A, B])(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        val o = if (returnBest) success(interior.node.value, interior.missing) else failure(interior.errors, interior.missing)
        o
    }
}
object BasicDecisionTree extends ParserProviderCompanion {

    object Parser
        extends ModelParserWithSemantics
        with EitherHelpers
        with DecisionTreeJson {

        import spray.json._

        val modelType = "DecisionTree"

        private[this] def dtBuilder[A, B: ScoreConverter](mId: ModelIdentity, t: Node[A, B], returnBest: Boolean) =
            BasicDecisionTree(mId, t, returnBest)

        /**
          *
          * @param semantics This reader requires semantics to be provided (some).  Otherwise, an error will occur. This
          *                  is because the regression models create functions for each feature in the model and
          *                  function creation is performed by the semantics.
          * @tparam A input type of the model
          * @tparam B output type of the model
          * @return
          */
        def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[BasicDecisionTree[A, B]] = new JsonReader[BasicDecisionTree[A, B]] {
            def read(json: JsValue) = {
                val dtAst = json.convertTo(decisionTreeAstJsonFormat[B])
                val mId = getModelId(json)
                val t = Tree[NodeAst[B], immutable.IndexedSeq, Node[A, B]](
                            dtAst.nodes, root, id, childIds, treeBuilder[A, B](semantics, dtAst.missingDataOk))
                val dt = dtBuilder[A, B](mId.get, t, dtAst.returnBest)
                dt
            }
        }
    }

    def parser: ModelParser = Parser
}

