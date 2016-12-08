package com.eharmony.aloha.models.tree

import scala.language.higherKinds

object Tree {

    /** Determine the topological sort order where the root of the tree is visited last.
      *
      * @param root the tree root
      * @param childMap a map-based adjacency list structure where keys represent parents, values are child lists.
      * @param numNodes the number of nodes in the tree
      * @return zero-based order where i is the node and a(i) is the order to visit the node.
      */
    def topologicalSort(root: Int, childMap: Map[Int, Seq[Int]], numNodes: Int): Seq[Int] = {
        /** Helper function to get the topological sort order.
          *
          * @param i the order in which the node pulled out of the stack should be visited.
          * @param s a stack
          * @param order the induced topological sort order
          * @return
          */
        def h(i: Int, s: collection.mutable.Stack[Int], order: Array[Int]): Seq[Int] = {
            if (s.isEmpty) order
            else {
                val node = s.pop()
                order(i) = node
                h(i - 1, s.pushAll(childMap.getOrElse(node, Nil)), order)
            }
        }

        h(numNodes - 1, collection.mutable.Stack(root), new Array[Int](numNodes))
    }

    /** Get an adjacency list structure.
      * @param root the index of the root node in the parents list
      * @param parents parents(i) refers to the node id of the parent of i.  For the root node, parents(i) == i.
      *   Since there is only one root to a tree, there should only be one index i such that parents(i) == i.
      * @return a map-based adjacency list structure where keys represent parents, values are child lists.
      */
    def adjacenyListStructure(root: Int, parents: Seq[Int]): Map[Int, List[Int]] = {
        val m = collection.mutable.Map[Int, List[Int]]()
        Iterator.range(0, root).foreach(i => {val p = (parents(i), m.getOrElse(parents(i), Nil) :+ i); m += p})
        Iterator.range(root + 1, parents.size).foreach(i => {val p = (parents(i), m.getOrElse(parents(i), Nil) :+ i); m += p})
        m.toMap
    }

    /** Determine the root node.
      * @param parents parents(i) refers to the node id of the parent of i.  For the root node, parents(i) == i.
      *   Since there is only one root to a tree, there should only be one index i such that parents(i) == i.
      * @return Some(i) if there exists an i such that i == parents(i); otherwise, None
      */
    def findRoot(parents: Seq[Int]): Option[Int] = {
        Iterator.range(0, parents.size).indexWhere(i => i == parents(i)) match {
            case -1 => None
            case i => Some(i)
        }
    }

    /** Build a tree from a sequence of nodes in some raw form with an adjacency list.
      *
      * This is useful when it is difficult to actually build the tree from the bottom up.  This method can efficiently
      * build a tree from the bottom up given a tree in a tabular format.  O(n) time and O(n) auxiliary space.
      *
      * For a binary search tree with three nodes, we may want to do something like the following:
      * {{{
      * scala> case class StringTree(value: String, subtrees: Seq[StringTree]) extends Tree[String, StringTree]
      * defined class StringTree
      *
      * scala> val f = (v: String, c: Seq[String], ch: Seq[StringTree]) => StringTree(v, ch)
      * f: (String, Seq[String], Seq[StringTree]) => StringTree = <function3>
      *
      * scala> Tree(Seq("zero", "one", "two"), Map(1 -> List(0,2)), 1, f)
      * res0: StringTree = StringTree(one,List(StringTree(zero,List()), StringTree(two,List())))
      * }}}
      * @param nodes a sequence of nodes in raw form
      * @param childMap an adjacency list
      * @param rootId the index of the root in the nodes parameter
      * @param builder a function that builds a tree given a node in its raw form and its children in raw form and tree form.
      * @tparam A the value type of the tree
      * @tparam C the container type of the descendants in the tree representation
      * @tparam TreeImpl the tree representation
      * @return a tree of type TreeImpl
      */
    def apply[A, C[_] <: Iterable[_], TreeImpl <: Tree[_, C, _]](
            nodes: Seq[A],
            childMap: Map[Int, Seq[Int]],
            rootId: Int,
            builder: (A, Seq[A], Seq[TreeImpl]) => TreeImpl): TreeImpl = {
        val n = nodes.size
        val order = topologicalSort(rootId, childMap, n)
        val built = collection.mutable.ArrayBuffer.fill(n)(null.asInstanceOf[TreeImpl])
        order.foreach(i => {
            val c = childMap.getOrElse(i, Nil)
            built(i) = builder(nodes(i), c.map(nodes(_)), c.map(built(_)))
        })
        built(rootId)
    }

    /** Build a tree from a sequence of nodes in some raw form with a parent list.
      *
      * This is useful when it is difficult to actually build the tree from the bottom up.  This method can efficiently
      * build a tree from the bottom up given a tree in a tabular format.  O(n) time and O(n) auxiliary space.
      *
      * For a binary search tree with three nodes, we may want to do something like the following:
      * {{{
      * scala> case class StringTree(value: String, subtrees: Seq[StringTree]) extends Tree[String, StringTree]
      * defined class StringTree
      *
      * scala> val f = (v: String, c: Seq[String], ch: Seq[StringTree]) => StringTree(v, ch)
      * f: (String, Seq[String], Seq[StringTree]) => StringTree = <function3>
      *
      * scala> Tree(Seq("zero", "one", "two"), Seq(1,1,1), f)
      * res0: StringTree = StringTree(one,List(StringTree(zero,List()), StringTree(two,List())))
      * }}}
      * @param nodes a sequence of nodes in raw form
      * @param parents parents(i) refers to the node id of the parent of i.  For the root node, parents(i) == i.
      *   Since there is only one root to a tree, there should only be one index i such that parents(i) == i.
      * @param builder a function that builds a tree given a node in its raw form and its children in raw form and tree form.
      * @tparam A the value type of the tree
      * @tparam TreeImpl the tree representation
      * @return a tree of type TreeImpl
      */
    def apply[A, C[_] <: Iterable[_], TreeImpl <: Tree[_, C, _]](
            nodes: Seq[A],
            parents: Seq[Int],
            builder: (A, Seq[A], Seq[TreeImpl]) => TreeImpl): TreeImpl = findRoot(parents) match {
        case None => throw new IllegalArgumentException
        case Some(rootId) => apply[A, C, TreeImpl](nodes, adjacenyListStructure(rootId, parents), rootId, builder)
    }

    /**
      *
      * @param nodes a list of nodes.
      * @param root index in nodes of the root node.
      * @param getId a function taking a node that returns an ID
      * @param childrenIds a function taking a node and a that returns ID for it children
      * @param builder a builder function
      * @tparam A type of nodes
      * @tparam C type of collection for children
      * @tparam TreeImpl the implementation of the tree
      * @return
      */
    def apply[A, C[_] <: Iterable[_], TreeImpl <: Tree[_, C, _]](
            nodes: Seq[A],
            root: Seq[A] => Int,
            getId: A => Int,
            childrenIds: A => Seq[Int],
            builder: (A, Seq[A], Seq[TreeImpl]) => TreeImpl): TreeImpl = {
        val ids = nodes.map(getId)
        val idMap = nodes.map(getId).zipWithIndex.toMap
        val children = ids.indices.zip(nodes.map(n => childrenIds(n).map(idMap.apply))).toMap
        val t = apply[A, C, TreeImpl](nodes, children, root(nodes), builder)
        t
    }
}

trait Tree[+A, +C[_] <: Iterable[_], +This <: Tree[_, C, _]] {
    val value: A
    def descendants: C[_ <: This]

    // TODO: BFS iterator
    // TODO: DFS iterator

    /** Get a NON thread-safe iterator over the nodes in the tree for a DFS ordering.  Each iterator value contains
      * the node in the tree and the depth (root has 0 depth).
      * This iterator requires O(B * D) auxiliary space where B is the branching factor and D is the tree depth.
      * @return a NON thread-safe iterator over the nodes in the tree for a DFS ordering.
      */
    def dfs(): Iterator[(This, Int)] = {
        val s = new collection.mutable.Stack[(This, Int)]
        s.push((this,0).asInstanceOf[(This, Int)])
        new Iterator[(This, Int)] {
            def hasNext = s.nonEmpty
            def next() = {
                val n = s.pop()
                s.pushAll(n._1.descendants.iterator.asInstanceOf[Iterator[This]].zip(Iterator.continually(n._2 + 1)))
                n
            }
        }
    }

}

