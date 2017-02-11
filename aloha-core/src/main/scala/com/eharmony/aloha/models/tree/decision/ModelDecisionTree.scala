package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.factory.ex.AlohaFactoryException
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.tree.Tree
import com.eharmony.aloha.models.{Submodel, SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.DefaultJsonProtocol.JsValueFormat
import spray.json.{JsValue, JsonFormat, JsonReader}

import scala.annotation.tailrec
import scala.collection.{immutable => sci}
import scala.util.{Failure, Success, Try}

// TODO: Unit tests failing.  Figure out what to do to make them pass.

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
case class ModelDecisionTree[U, N, -A, +B <: U](
    modelId: ModelIdentity,
    root: Node[A, Submodel[N, A, U]],
    returnBest: Boolean,
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, A, B] {

  def subvalue(a: A): Subvalue[B, N] = {
    // Find the proper node.
    val n = root.getNode(a)

    // Deal with internal nodes and leaves differently.  A leaf indicates a success. An internal node can
    // indicate either a partial success or a failure, depending on configuration.
    val r = n.fold(processInterior(a, _), processLeaf(a, _))

    r
  }

  /*
    protected [this] def processLeaf(a: A, m: Leaf[Model[A, B]])(implicit audit: Boolean) = {
      val (mo, os) = m.value.getScore(a)
      val o = mo.fold({case (e, missing) => failure(e, missing, os)}, s => success(score = s, subScores = os))
      o
    }
   */
  protected[this] def processLeaf(a: A, m: Leaf[Submodel[N, A, U]]): Subvalue[B, N] = {
    val s = m.value.subvalue(a)

    s.fold(
      failure(
        Seq(s"Problem evaluating submodel ${m.value.modelId} at leaf node in model decision tree."),
        Set.empty,
        Seq(s.audited)),
      n => success(n, subvalues = Seq(s.audited))
    )
  }

  /*
   protected [this] def processInterior(a: A, interior: InteriorNodeResult[A, Model[A, B]])
                                       (implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        val o =
            if (returnBest) {
                val (mo, os) = interior.node.value.getScore(a)
                mo.fold({ case (errs, missing) } => failure(errs, interior.missing, os) },
                        s => success(s, interior.missing, os))
            }
            else failure(interior.errors, interior.missing)
        o
    }
   */
  protected [this] def processInterior(a: A, interior: InteriorNodeResult[A, Submodel[N, A, U]]): Subvalue[B, N] = {
    val o =
      if (returnBest) {
        val s = interior.node.value.subvalue(a)
        s.fold(
          failure(
            Seq(s"Problem evaluating submodel ${interior.node.value.modelId} at internal node in model decision tree."),
            interior.missing.toSet,
            Seq(s.audited)),
          n => success(n, missingVarNames = interior.missing.toSet, subvalues = Seq(s.audited))
        )
      }
      else
        failure(
          s"Could not get to leaf node in model decision tree ${modelId} and returnBest == false." +: interior.errors,
          interior.missing.toSet)

    o
  }

  /**
    * Recursively close the submodels.
    */
  override def close(): Unit = root.dfs() foreach { node => node._1.value.close() }
}


object ModelDecisionTree extends ParserProviderCompanion {

  object Parser
    extends ModelSubmodelParsingPlugin
      // with EitherHelpers
       with DecisionTreeJson {

    val modelType = "ModelDecisionTree"

    private[this] def getSubmodels[U, N: RefInfo, A](
        factory: SubmodelFactory[U, A],
        subJsons: Seq[NodeAst[JsValue]]): Try[Vector[NodeAst[Submodel[N, A, U]]]] = {

      @tailrec def h(subs: List[NodeAst[JsValue]],
                     completed: Vector[NodeAst[Submodel[N, A, U]]]): Try[Vector[NodeAst[Submodel[N, A, U]]]] = {
        subs match {
          case Nil => Try { completed }
          case sub :: rest =>
            factory.submodel[N](sub.value) match {
              case Success(s) =>
                h(rest, completed :+ sub.copy(value = s))
              case Failure(f) =>
                val idStr = getModelId(sub.value).fold("")(id => s"${id.toString} ")
                val lastSuccess = completed.headOption.fold(""){s =>
                  s"Last successfully parsed submodel: ${s.value.modelId.toString}.  "
                }

                // TODO: Add parsers available information.
                val msg = s"Problem getting submodel ${idStr}from JSON.  ${lastSuccess}Factory contains parsers: "
                Failure(new AlohaFactoryException(msg, f))
            }
        }
      }

      h(subJsons.toList, Vector.empty)
    }

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[ModelDecisionTree[U, N, A, B]]] = {

      Some(new JsonReader[ModelDecisionTree[U, N, A, B]] {
        override def read(json: JsValue): ModelDecisionTree[U, N, A, B] = {
          // Get the model ID. Failure early before doing additional work
          val mId = getModelId(json).get

          // Submodels are still just JSON ASTs at this point.
          val dtAst = json.convertTo(decisionTreeAstJsonFormat[JsValue])

          getSubmodels[U, N, A](factory, dtAst.nodes) match {
            case Success(nodes) =>
              // If the nodes are all successfully converted to Submodels, build the Tree.
              val r = Tree[NodeAst[Submodel[N, A, U]], sci.IndexedSeq, Node[A, Submodel[N, A, U]]](
                nodes, root, id, childIds, treeBuilder[A, Submodel[N, A, U]](semantics, dtAst.missingDataOk))
              ModelDecisionTree(mId, r, dtAst.returnBest, auditor)
            case Failure(f) =>
              throw f
          }
        }
      })
    }
  }

  def parser: ModelParser = Parser

  //    object Parser
//        extends ModelParser
//        with EitherHelpers
//        with DecisionTreeJson {
//
//        val modelType = "ModelDecisionTree"
//
//        /** Construct a submodel by calling the factory on the supplied json.
//          * @param f a factory
//          * @param s a semantics
//          * @param json json to turn into a model
//          * @tparam A the submodel input type (same as parent model's input type)
//          * @tparam B the submodel output type (same as parent model's output type)
//          * @return a submodel.
//          */
//        @throws[AlohaFactoryException]("When a submodel could not be constructed from json")
//        private[this] def subModel[A, B](f: ModelFactory, s: Option[Semantics[A]], json: JsValue)(implicit jr: JsonReader[B], sc: ScoreConverter[B]) = {
//
//            // Get the reflection information and convert it to implicits so we can call the factory.
//            implicit val a = s.get.refInfoA
//            implicit val b = implicitly[ScoreConverter[B]].ri
//
//            // Call the factory and change the exception to an Aloha exception, if one occurs.
//            f.getModel[A, B](json, s).recover {
//                case e => // Map the throwable to a Factory exception.
//                    throw new AlohaFactoryException(
//                        "Problem getting submodel from JSON.  Factory contains parsers: " +
//                            f.availableParsers.keys.mkString(", "), e)
//            }.get
//        }
//
//        def modelJsonReader[A, B: JsonReader : ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]): JsonReader[ModelDecisionTree[A, B]] = new JsonReader[ModelDecisionTree[A, B]] {
//            def read(json: JsValue): ModelDecisionTree[A, B] = {
//                val mId = getModelId(json).get
//
//                // Submodels are still just JSON at this point.
//                import spray.json.DefaultJsonProtocol.JsValueFormat
//                val dtAst = json.convertTo(decisionTreeAstJsonFormat[JsValue])
//
//                // Realize all JSON submodels.
//                val nodes = dtAst.nodes.map(n => n.copy(value = subModel[A, B](factory, semantics, n.value)))
//
//                val r = Tree[NodeAst[Model[A, B]], immutable.IndexedSeq, Node[A, Model[A, B]]](
//                            nodes, root, id, childIds, treeBuilder[A, Model[A, B]](semantics.get, dtAst.missingDataOk))
//
//                val t = ModelDecisionTree(mId, r, dtAst.returnBest)
//                t
//            }
//        }
//    }

}
