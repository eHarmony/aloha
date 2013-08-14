package com.eharmony.matching.aloha.models.tree.decision

import scala.collection.immutable
import scala.language.higherKinds
import spray.json.{JsValue, JsonReader}

import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.factory.{ModelFactory, ModelParser, ParserProviderCompanion}
import com.eharmony.matching.aloha.util.EitherHelpers
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.factory.ex.AlohaFactoryException
import com.eharmony.matching.aloha.models.tree.Tree


/** A decision tree containing models at the nodes.  The evaluation algorithm works as follows:
  -   Given input '''a'''
  -   Find the terminal node, '''n''', in the tree using the standard decision tree algorithm
  -   At node '''n''', there exists a model, '''m'''.
  -   Let '''o''' be the value of '''m''' evaluated at '''a'''.
  -   '''o''' will be a subscore of this model and this model will also output the same score.
  *
  * If a
  *
  *
  *
  * The benefit to this is that we report which submodel was responsible for producing the score.
  * @param modelId An id with which to identify this model
  * @param root the root node of the decision tree
  * @param returnBest if no path from the root to a leaf can be generated for a given input, should we return a score
  *                   associated with an interior node?
  * @tparam A model input type
  * @tparam B model output type
  */
case class ModelDecisionTree[-A, +B: ScoreConverter](
        modelId: ModelIdentity,
        root: Node[A, Model[A, B]],
        returnBest: Boolean)
    extends Model[A, B] {

    private[aloha] def getScore(a: A)(implicit audit: Boolean) = {
        // Find the proper node.
        val n = root.getNode(a)

        // Deal with internal nodes and leaves differently.  A leaf indicates a success. An internal node can
        // indicate either a partial success or a failure, depending on configuration.
        val r = n.fold(processInterior(a, _), processLeaf(a, _))

        r
    }

    protected [this] def processLeaf(a: A, m: Leaf[Model[A, B]])(implicit audit: Boolean) = {
        val (mo, os) = m.value.getScore(a)
        val o = mo.fold({case (e, missing) => failure(e, missing, os)}, s => success(score = s, subScores = os))
        o
    }

    protected [this] def processInterior(a: A, interior: InteriorNodeResult[A, Model[A, B]])(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        val o =
            if (returnBest) {
                val (mo, os) = interior.node.value.getScore(a)
                mo.fold(f => failure(f._1, interior.missing, os), s => success(s, interior.missing, os))
            }
            else failure(interior.errors, interior.missing)

        o
    }
}


object ModelDecisionTree extends ParserProviderCompanion {
    object Parser
        extends ModelParser
        with EitherHelpers
        with DecisionTreeJson {

        val modelType = "ModelDecisionTree"

        /** Construct a submodel by calling the factory on the supplied json.
          * @param f a factory
          * @param s a semantics
          * @param json json to turn into a model
          * @tparam A the submodel input type (same as parent model's input type)
          * @tparam B the submodel output type (same as parent model's output type)
          * @return a submodel.
          */
        @throws[AlohaFactoryException]("When a submodel could not be constructed from json")
        private[this] def subModel[A, B](f: ModelFactory, s: Option[Semantics[A]], json: JsValue)(implicit jr: JsonReader[B], sc: ScoreConverter[B]) = {

            // Get the reflection information and convert it to implicits so we can call the factory.
            implicit val a = s.get.refInfoA
            implicit val b = implicitly[ScoreConverter[B]].ri

            // Call the factory and change the exception to an Aloha exception, if one occurs.
            f.getModel[A, B](json, s).recover {
                case e => // Map the throwable to a Factory exception.
                    throw new AlohaFactoryException(
                        "Problem getting submodel from JSON.  Factory contains parsers: " +
                            f.availableParsers.keys.mkString(", "), e)
            }.get
        }

        def modelJsonReader[A, B: JsonReader : ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]): JsonReader[ModelDecisionTree[A, B]] = new JsonReader[ModelDecisionTree[A, B]] {
            def read(json: JsValue): ModelDecisionTree[A, B] = {
                val mId = getModelId(json).get

                // Submodels are still just JSON at this point.
                import spray.json.DefaultJsonProtocol.JsValueFormat
                val dtAst = json.convertTo(decisionTreeAstJsonFormat[JsValue])

                // Realize all JSON submodels.
                val nodes = dtAst.nodes.map(n => n.copy(value = subModel[A, B](factory, semantics, n.value)))

                val r = Tree[NodeAst[Model[A, B]], immutable.IndexedSeq, Node[A, Model[A, B]]](
                            nodes, root, id, childIds, treeBuilder[A, Model[A, B]](semantics.get, dtAst.missingDataOk))

                val t = ModelDecisionTree(mId, r, dtAst.returnBest)
                t
            }
        }
    }

    def parser: ModelParser = Parser
}
