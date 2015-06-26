package com.eharmony.aloha.models.tree.decision

import scala.collection.immutable
import com.eharmony.aloha.models.tree.Tree

/** A simple representation of a decision tree node.  It is just like a [[com.eharmony.aloha.models.tree.Tree]]
  * except that it has a way of choosing a descendant, given an input.
  *
  * @tparam A the type of node in the decision tree.
  * @tparam B the domain of the childSelector function
  * @author R M Deak
  */
sealed trait Node[-A, +B] extends Tree[B, immutable.IndexedSeq, Node[A, B]] {

    /** Find the deepest node in the tree possible, given the data (in v), on which to branch.  Progress down the
      * decision tree until no further progress can be made and return the node.
      *
      * [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] when a leaf is reached or
      * [[scala.util.Right]][[(com.eharmony.aloha.models.tree.decision.InteriorNode, Option[String])]]
      * when progress down the tree was stopped prior to reaching a leaf node.  The node in the tuple is the last node
      * in which we could make a successful choice.
      * @param v the input whose data is used to branch down the decision tree.
      * @return either a [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] representing a
      *         leaf node in the tree or a [[scala.util.Left]] with a node where no further progress down the tree
      *         could be made and an optional sequence of log messages.
      */
    def getNode(v: A): Either[InteriorNodeResult[A, B], Leaf[B]]
}

/** Provides factory methods for creating Decision Tree nodes.
  */
object Node {

    /** Create a decision tree leaf node.
      * @param value a value stored in the leaf node.
      * @tparam B the type of the value
      * @return
      */
    def apply[B](value: B) = Leaf(value)

    /** Create a decision tree node.  The concrete type may be a Leaf or an InteriorNode depending on whether number the
      * number of descendants is 0 or more.  If descendants size is non-zero, then an scala.collection.immutable.Seq
      * will be produced from descendants.
      * @param value a value stored in the node
      * @param descendants the descendant nodes
      * @param ns a means of choosing the descendant
      * @tparam A type of the value passed to the decision tree for use in making the branching decisions.
      * @tparam B the type of data stored in the nodes
      * @return
      */
    def apply[A, B](value: B, descendants: Seq[Node[A, B]], ns: NodeSelector[A]) = {
        if (descendants.isEmpty) Leaf(value)
        else InteriorNode(value, descendants.toIndexedSeq, ns)
    }

    /** Progress down the decision tree until no further progress can be made and return the node.
      *
      * [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] when a leaf is reached or
      * [[scala.util.Right]][[(com.eharmony.aloha.models.tree.decision.InteriorNode, Option[String])]]
      * when progress down the tree was stopped prior to reaching a leaf node.  The node in the tuple is the last node
      * in which we could make a successful choice.
      * @param n a node from which to start the search.
      * @param v the input whose data is used to branch down the decision tree.
      * @return either a [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] representing a
      *         leaf node in the tree or a [[scala.util.Left]] with a node where no further progress down the tree
      *         could be made and an optional sequence of log messages.
      */
    private[decision] def getNode[A, B](n: Node[A, B], v: A): Either[InteriorNodeResult[A, B], Leaf[B]] = n match {
        case leaf: Leaf[B] => Right(leaf)
        case interior: InteriorNode[A, B] => interior.nodeSelector(v) match {
            case i if i < 0 =>
                val em = interior.nodeSelector.processErrorAt(v, i)
                Left(InteriorNodeResult(interior, em.errors, em.missing))
            case i if i >= n.descendants.size =>
                val missing = interior.nodeSelector.processErrorAt(v, i).missing
                Left(InteriorNodeResult(interior, Seq(s"Node selector returned index = $i. Number of children = ${n.descendants.size}.  node selector: ${interior.nodeSelector}"), missing))
            case i => getNode(n descendants i, v)  // Select child and recurse.
        }
    }
}

/** Representation of a decision tree leaf node.
  * @param value a value stored in this leaf node.
  * @tparam B the domain of the childSelector function
  */
case class Leaf[+B](value: B) extends Node[Any, B] {

    /** Return this node wrapped in a [[scala.util.Right]].
      *
      * @param v irrelevant input data. (Not used)
      * @return either a [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] this node.
      */
    def getNode(v: Any) = Right(this)

    /** Empty descendants list
      */
    val descendants = immutable.IndexedSeq.empty[Nothing]
}

case class InteriorNode[-A, +B](
        value: B,
        descendants: immutable.IndexedSeq[Node[A, B]],
        nodeSelector: NodeSelector[A]
) extends Node[A, B] {

    /** Progress down the decision tree until no further progress can be made and return the node.
      *
      * [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] when a leaf is reached or
      * [[scala.util.Right]][[(com.eharmony.aloha.models.tree.decision.InteriorNode, Option[String])]]
      * when progress down the tree was stopped prior to reaching a leaf node.  The node in the tuple is the last node
      * in which we could make a successful choice.
      * @param v the input whose data is used to branch down the decision tree.
      * @return either a [[scala.util.Right]][[com.eharmony.aloha.models.tree.decision.Leaf]] representing a
      *         leaf node in the tree or a [[scala.util.Left]] with a node where no further progress down the tree
      *         could be made and an optional sequence of log messages.
      */
    def getNode(v: A): Either[InteriorNodeResult[A, B], Leaf[B]] = Node.getNode(this, v)
}
