package com.eharmony.matching.aloha.models.tree.decision

import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz

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

    private[this] case class LinearNodeSelectorAst(
            children: Seq[Int],
            predicates: Seq[String],
            selectorType: String = "linear") extends NodeSelectorAst {

        def nodeSelector[A](semantics: Semantics[A], missingOk: Boolean): NodeSelector[A] = {
            val p = predicates.map{p => semantics.createFunction[Option[Boolean]](p, Option(None)).right.get}.toList
            LinearNodeSelector(p, missingOk)
        }
    }

    private[this] implicit val linearNodeSelectorAstJsonFormat = jsonFormat3(LinearNodeSelectorAst)
    private[this] implicit object NodeSelectorJsonFormat extends JsonFormat[NodeSelectorAst] with JsValuePimpz {
        def read(json: JsValue) =
            json("selectorType") map {_ match {
                case JsString("linear") => json.convertTo[LinearNodeSelectorAst]
                case JsString(selType) => throw new DeserializationException(s"unrecognized selectorType '$selType'. with one of the following values: 'linear'")
                case x => throw new DeserializationException(s"selectorType must be a string with one of the following values: 'linear'. found $x")
            }} getOrElse {
                throw new DeserializationException("node selector needs selectorType field with one of the following values: 'linear'")
            }

        def write(ns: NodeSelectorAst) = ns match {
            case l: LinearNodeSelectorAst => linearNodeSelectorAstJsonFormat write l
        }
    }

    protected[this] final case class NodeAst[B](id: Int, selector: Option[NodeSelectorAst], value: B)
//    implicit val modelNodeJsonFormat = jsonFormat3(NodeAst.apply[JsValue])

    private[this] implicit def modelNodeJsonFormat[B: JsonReader] = {
        implicit val jf = jsonReaderToJsonFormat[B]
        jsonFormat3(NodeAst.apply[B])
    }

    protected[this] final case class DecisionTreeAst[B](returnBest: Boolean, missingDataOk: Boolean, nodes: Seq[NodeAst[B]])
//    implicit val decisionTreeAstJsonFormat = jsonFormat3(DecisionTreeAst.apply[JsValue])

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


//trait asdf {
//    import spray.json._
//    import spray.json.DefaultJsonProtocol._

    // Same: NodeSelectorAst
    // Same: LinearNodeSelectorAst(
    // Same: linearNodeSelectorAstJsonFormat
    // Same: NodeSelectorJsonFormat
    // Same: NodeAst

    // Only in BasicDecisionTree: nodeAstJsonFormat
    // Only in ModelDecisionTree: modelNodeJsonFormat
    // Same: ModelDecisionTree
    // Same: implicit def decisionTreeAstJsonFormat[B: JsonFormat]: JsonFormat[DecisionTreeAst[B]] = jsonFormat3(DecisionTreeAst.apply[B])

    // Same: root
    // Same: id
    // Same: childIds = (n: NodeAst[_]) => n.selector.map(_.children).getOrElse(Nil)

//    def treeBuilder[A, B](sem: Semantics[A], missingDataOk: Boolean) =
//        (n: NodeAst[B], rawChildren: Seq[NodeAst[B]], children: Seq[Node[A, B]]) =>
//            if (children.isEmpty) n.selector.toLeft(Node(n.value)).fold(_ => throw new Exception(s"No children but has node selector: $n"), identity)
//            else n.selector.toRight(n).fold(f => throw new Exception("a"), s => Node(n.value, children, s.nodeSelector(sem, missingDataOk)))
//}
