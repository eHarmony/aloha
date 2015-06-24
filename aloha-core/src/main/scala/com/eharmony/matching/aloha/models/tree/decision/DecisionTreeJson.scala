package com.eharmony.matching.aloha.models.tree.decision

import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.matching.aloha.util.rand.HashedCategoricalDistribution
import com.eharmony.matching.aloha.factory.ex.AlohaFactoryException

trait DecisionTreeJson {
    import spray.json.{JsonReader, JsonFormat, JsValue, JsString, DeserializationException}
    import spray.json.DefaultJsonProtocol._

    protected[this] trait NodeSelectorAst {
        val children: Seq[Int]
        def nodeSelector[A](semantics: Semantics[A], missingOk: Boolean): NodeSelector[A]
    }

    private[this] def jsonReaderToJsonFormat[A](implicit jr: JsonReader[A]): JsonFormat[A] = jr match {
        case jf: JsonFormat[A] => jf
        case _ => new JsonFormat[A] {
            def write(a: A): JsValue = throw new UnsupportedOperationException("write not supported")
            def read(json: JsValue): A = jr.read(json)
        }
    }

    private[this] case class RandomNodeSelectorAst(
            children: Seq[Int],
            features: Seq[String],
            probabilities: Seq[Double],
            selectorType: String) extends NodeSelectorAst {

        def nodeSelector[A](semantics: Semantics[A], missingOk: Boolean): NodeSelector[A] = {

            // TODO: Check this out: A little dangerous compiling a function that can return any type.
            // I guess no more dangerous than not, if we allow impure functions (which we do).
            val functions = features.map { f => semantics.createFunction[Any](f, Option(None)).fold(
                // Preemptively throw an exception containing the actual actual problem rather than passively
                // getting a NoSuchElementException when accessing the .right of a Left.
                failMsgs => throw new AlohaFactoryException(s"Problem creating random node selector feature '$f'${failMsgs.mkString(":\n", "\n", "")}" ),
                identity
            )}.toIndexedSeq

            val distribution = HashedCategoricalDistribution(probabilities:_*)
            RandomNodeSelector(functions, distribution, missingOk)
        }
    }

    private[this] implicit val randomNodeSelectorAstJsonFormat = jsonFormat4(RandomNodeSelectorAst)

    private[this] case class LinearNodeSelectorAst(
            children: Seq[Int],
            predicates: Seq[String],
            selectorType: String = "linear") extends NodeSelectorAst {

        def nodeSelector[A](semantics: Semantics[A], missingOk: Boolean): NodeSelector[A] = {
            val pred = predicates.map { p =>
                semantics.createFunction[Option[Boolean]](p, Option(None)).fold(
                    // Preemptively throw an exception containing the actual actual problem rather than passively
                    // getting a NoSuchElementException when accessing the .right of a Left.
                    failMsgs => throw new AlohaFactoryException(s"Problem creating linear node selector predicate '$p'${failMsgs.mkString(":\n", "\n", "")}" ),
                    identity
                )
            }.toList

            // val p = predicates.map{p => semantics.createFunction[Option[Boolean]](p, Option(None)).right.get}.toList
            LinearNodeSelector(pred, missingOk)
        }
    }

    private[this] implicit val linearNodeSelectorAstJsonFormat = jsonFormat3(LinearNodeSelectorAst)
    private[this] implicit object NodeSelectorJsonFormat extends JsonFormat[NodeSelectorAst] with JsValuePimpz {
        val acceptable = Seq("linear", "random").map("'" + _ + "'").mkString(", ")
        def read(json: JsValue) =
            json("selectorType") map {
                case JsString("linear") => json.convertTo[LinearNodeSelectorAst]
                case JsString("random") => json.convertTo[RandomNodeSelectorAst]
                case JsString(selType) => throw new DeserializationException(s"unrecognized selectorType '$selType'. with one of the following values: 'linear'")
                case x => throw new DeserializationException(s"selectorType must be a string with one of the following values: $acceptable. found $x")
            } getOrElse {
                throw new DeserializationException(s"node selector needs selectorType field with one of the following values: $acceptable")
            }

        def write(ns: NodeSelectorAst) = ns match {
            case l: LinearNodeSelectorAst => linearNodeSelectorAstJsonFormat write l
        }
    }

    protected[this] case class NodeAst[B](id: Int, selector: Option[NodeSelectorAst], value: B)

    private[this] implicit def modelNodeJsonFormat[B: JsonReader] = {
        implicit val jf = jsonReaderToJsonFormat[B]
        jsonFormat3(NodeAst.apply[B])
    }

    protected[this] case class DecisionTreeAst[B](returnBest: Boolean, missingDataOk: Boolean, nodes: Seq[NodeAst[B]])

    protected[this] final def decisionTreeAstJsonFormat[B: JsonReader] =  {
        implicit val jf = jsonReaderToJsonFormat[B]
        jsonFormat3(DecisionTreeAst.apply[B])
    }

    protected[this] final val root = (_: Any) => 0
    protected[this] final val id = (_: NodeAst[_]).id
    protected[this] final val childIds = (n: NodeAst[_]) => n.selector.map(_.children).getOrElse(Nil)

    protected[this] final def treeBuilder[A, B](sem: Semantics[A], missingDataOk: Boolean): (NodeAst[B], Seq[NodeAst[B]], Seq[Node[A, B]]) => Node[A, B] =
        (n: NodeAst[B], rawChildren: Seq[NodeAst[B]], children: Seq[Node[A, B]]) =>
            if (children.isEmpty) n.selector.toLeft(Node(n.value)).fold(_ => throw new Exception(s"No children but has node selector: $n"), identity)
            else n.selector.toRight(n).fold(f => throw new Exception("a"), s => Node(n.value, children, s.nodeSelector(sem, missingDataOk)))
}
