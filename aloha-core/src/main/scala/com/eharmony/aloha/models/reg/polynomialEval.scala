package com.eharmony.aloha.models.reg

import scala.collection.mutable
import scala.collection.mutable.{Set => MSet, Map => MMap, Queue => MQueue, ArrayBuffer}

import com.eharmony.aloha.models.tree.Tree


/** A tree with a map structure for the descendants data structure.
  * '''Note''': Map keys are invariant.  We could make K contravariant and use existential types in the type
  * lambda to make it so we could construct MapTree without having to specify the type in the root
  * instance or leave instance.
  * @tparam K key type of the map structure
  * @tparam A type of the descendant data structure.
  */
trait MapTreeLike[K, +A] extends Tree[A, ({type L[+V] = Map[K, V]})#L, MapTreeLike[K, A]]

case class Coefficient(coeff: Double, featureIndices: IndexedSeq[Int] = IndexedSeq.empty[Int])

/** An algorithm for efficiently evaluating polynomials at a given point.  Evaluating first order polynomials is
  * obviously a sub case, which is important because first order polynomial evaluation is isomorphic to linear
  * regression, which may be the standard use case.
  *
  * As an example, imagine that we wanted to evaluate ''Z''(''u'',''v'',''x'',''y'') = ''w,,u,v,,uv + w,,u,v,x,,uvx + w,,u,v,y,,uvy''
  * for coefficients ''W'' = [''w,,u,v,,'', ''w,,u,v,x,,'', ''w,,u,v,y,,'']^T^.
  *
  * This is:
  *   - ''Z'' = ''w,,u,v,,uv + w,,u,v,x,,uvx + w,,u,v,y,,uvy''
  *   - ''Z'' = ''uv(w,,u,v,, + w,,u,v,x,,x + w,,u,v,y,,y)''
  *
  * That ''Z'' can be factored indicates there is a way to structure the algorithm to efficiently reuse
  * computations.  The way to achieve this is to structure the possible polynomials as a tree and traverse and
  * aggregate over the tree.  As the tree is traversed, the computation is accumulated.  As a concrete example,
  * let's take the above example and show it using real code.  Then a motivation of the example will be provided.
  *
  * The computation tree works as follows: the edge labels are multiplied by the associated coefficient (0 if non-existent)
  * to get the node values.  Node values are added together to get the inner product.  So, every time we descend farther
  * into the tree, we multiply the running product by the value we extract from the input vector X and every time a
  * weight is found, it is multiplied by the current product and added to the running sum.  The process recurses until
  * the tree can no longer by traversed.  The sum is then returned.
  *
  * {{{
  * //           u            u      v                                u     v     x
  * //    (1)*1.00      (1*1.00)*1.000        u     v    w1     (1*1.00*1.000)*0.75        u     v    x      w2
  * //   ----------> 0 ----------------> 1*1.00*1.000 * 0.5 ------------------------> 1*1.00*1.000*0.75 * 0.111
  * //                                                      \
  * //                                                       \        u     v     y
  * //                                                        \ (1*1.00*1.000)*0.25        u     v    y       w3
  * //                                                         ---------------------> 1*1.00*1.000*0.25 * 0.4545
  * //
  * //         u *     v *  w1    +       u *     v *    x *    w2   +       u *     v *    y *     w3
  * val Z = 1.00 * 1.000 * 0.5    +    1.00 * 1.000 * 0.75 * 0.111   +    1.00 * 1.000 * 0.25 * 0.4545
  *
  * val X = IndexedSeq(
  *           Seq(("a=1", 1.00)),                  // u
  *           Seq(("b=1", 1.000)),                 // v
  *           Seq(("c=1", 0.75), ("c=2", 0.25)))   // x and y, respectively
  *
  * val W1 =
  *   PolynomialEvaluator(Coefficient(0, IndexedSeq(0)), Map(
  *     "a=1" -> PolynomialEvaluator(Coefficient(0, IndexedSeq(1)), Map(
  *       "b=1" -> PolynomialEvaluator(Coefficient(0.5, IndexedSeq(2)), Map(   // w1
  *         "c=1" -> PolynomialEvaluator(Coefficient(0.111)),                  // w2
  *         "c=2" -> PolynomialEvaluator(Coefficient(0.4545))))))))            // w3
  *
  *
  * assert(Z == (W1 dot X))
  * }}}
  *
  * While constructing a [[PolynomialEvaluator]] via direct means is entirely
  * possible, it is less straightforward than using a builder to do it.  Below, we show a better way to construct
  * [[PolynomialEvaluator]] instances where we just specify the terms in the
  * polynomial and the associated coefficient values.  Note linear regression is the special case when all of the
  * inner maps contain exactly one element.
  *
  * {{{
  * val W2 = (PolynomialEvaluator.builder ++= Map(
  *   Map("a=1" -> 0, "b=1" -> 1            ) -> 0.5,
  *   Map("a=1" -> 0, "b=1" -> 1, "c=1" -> 2) -> 0.111,
  *   Map("a=1" -> 0, "b=1" -> 1, "c=2" -> 2) -> 0.4545
  * )).result
  *
  * assert(W2 == W1)
  * }}}
  *
  * Notice the values in the inner map look a little weird.  These are the indices into the input vector x from which
  * the key comes.  This is for efficiency purposes but allows the algorithm to dramatically prune the search
  * space while accumulating over the tree.
  */
trait PolynomialEvaluationAlgo { self: MapTreeLike[String, Coefficient] =>
    import collection.mutable.{Stack => MStack}

    /** Evaluate the polynomial represented by this object at the point x.
      * @param x A representation of a point.  The representation is as follows:
      *          The outer indexed sequence represents groups of features in feature space generated by the same
      *          feature expander.  The inner sequence is an iterable sequence of key value pairs.  This representation
      *          is used because it allows efficient encoding of sparse feature spaces.  For instance, we can very
      *          easily encode multi-part bag of word models as follows.  Let's say the 0th index in the outer sequence
      *          represents the title of HTML documents and the 1st index represents text inside the body tags in the
      *          HTML document.
      * {{{
      * case class Doc(title: String, body: String) {
      *   def multiPartBagOfWords = IndexedSeq(tokens(title), tokens(body))
      *   private[this] def tokens(s: String) = s.trim.toLowerCase.split("\\W+").groupBy(identity).mapValues(_.size.toDouble)
      * }
      *
      * val fox = Doc("fox story", "The quick brown fox jumps over the lazy dog")
      * val p: PolynomialEvaluationAlgo = ...
      * p at fox.multiPartBagOfWords
      *
      * }}}
      * @return this polynomial evaluated at x.
      */
    def at(x: IndexedSeq[Iterable[(String, Double)]]): Double = {
        hSmall(x, x.size, this, 1)

        // TODO branch algorithm on depth to avoid stack overflow at the price of speed.
        // if (treeDepthIsSmall) hSmall(x, this, 1) else hBig(x, MStack((this, 1)), 0)
    }

    /** Accumulate, in a depth-first way, the evaluation of the polynomial.
      *
      * '''NOTE''': This algorithm is recursive (but not tail-recursive) by seems to be about 20% faster than the
      * following tail-recursive equivalent.  This is probably due to additional object creation in the tail-recursive
      * method.  Tuple2 instances must be created and the linked list containers probably needs to be created.  In the
      * non-tail recursive version, we don't do any of this and only perform arithmetic operations (aside from the
      * iterators and the Options created in the descendant lookup).
      *
      * We aren't too afraid of stack overflows because these trees will typically be shallow.  This is because most
      * use cases don't involve polynomials in thousands of variables (or of degree in the thousands).  That being
      * said, stack overflows are a real possibility (in test at a depth of ~3000).  To combat this, hBig is provided
      * but not yet integrated into the at function.
      *
      * {{{
      * import collection.mutable.{Stack => MStack}
      * def h3(x: IndexedSeq[Seq[(String, Double)]], s: MStack[(MapTreeLike[String, Value], Double)], z: Double): Double = {
      *   if (s.isEmpty) z
      *   else {
      *     val h = s.pop
      *     for (i <- h._1.value.applicableFeatures; p <- x(i); c <- h._1.descendants.get(p._1)) s.push((c, p._2 * h._2))
      *     h3(x, s, z + h._1.value.weight * h._2)
      *   }
      * }
      * }}}
      * @param x an input vector
      * @param xSize size of input vector (computed once for possible speed improvement).
      * @param t a tree containing the information necessary to compute the higher order inner product
      * @param prod product of the items in the path from the root to leaf.
      * @return this polynomial evaluated at x.
      */
    final protected[this] def hSmall(x: IndexedSeq[Iterable[(String, Double)]], xSize: Int, t: MapTreeLike[String, Coefficient], prod: Double): Double = {
        // This isn't really very Scala-like code but needs to be fast.  This could be a fold or a for comprehension
        // but those were slower in testing.
        var z = t.value.coeff * prod
        var i = 0
        val n = t.value.featureIndices.size
        while(i < n && t.value.featureIndices(i) < xSize) { // "t.value.featureIndices(i) < xSize" ensures no out of bounds
            val pIt = x(t.value.featureIndices(i)).iterator
            while(pIt.hasNext) {
                val p = pIt.next()
                val c = t.descendants.get(p._1)
                if (c.nonEmpty) {
                    val h = hSmall(x, xSize, c.get, prod * p._2)
                    z += h
//                    z += hSmall(x, xSize, c.get, prod * p._2)
                }
            }
            i += 1
        }
        z
    }

    /** A tail-recursive variant of hSmall that won't stack overflow on deep trees.  This function is slower in
      * empirical tests that hSmall.
      * @param x an input vector
      * @param s a stack of trees and current sums at those trees (replaces call stack in non-tail-recursive hSmall).
      * @param sum the current sum.
      * @return this polynomial evaluated at x.
      */
    final protected[this] def hBig(x: IndexedSeq[Iterable[(String, Double)]], s: MStack[(MapTreeLike[String, Coefficient], Double)], sum: Double): Double = {
        if (s.isEmpty) sum
        else {
            val h = s.pop()
            for (i <- h._1.value.featureIndices; p <- x(i); c <- h._1.descendants.get(p._1)) s.push((c, p._2 * h._2))
            hBig(x, s, sum + h._1.value.coeff * h._2)
        }
    }
}

/** Provides a method to evaluate polynomials given an input.  Default implementation of
  * [[com.eharmony.aloha.models.reg.PolynomialEvaluationAlgo]].
  * @param value
  * @param descendants
  */
case class PolynomialEvaluator(value: Coefficient, descendants: Map[String, PolynomialEvaluator] = Map.empty[String, PolynomialEvaluator])
    extends MapTreeLike[String, Coefficient]
    with PolynomialEvaluationAlgo {

    /** Need to overload so that calling toString doesn't stack overflow like it would with the default implementation.
      * @return
      */
    // TODO: Better toString that doesn't stack overflow on big polynomials.
    override def toString = s"PolynomialEvaluator($value, ${descendants.keys} ...) [Full toString to be available later]"
}

/** A polynomial evaluator.
  *
  */
object PolynomialEvaluator {
    type Path = TraversableOnce[(String, Int)]
    type PathAndWeight = (Path, Double)

    /** Get a builder (that is NOT THREADSAFE).
      *
      * @return
      */
    def builder = new PolynomialEvaluatorBuilder()

    private[this] case class MutableValue(var weight: Double = 0, featureIndices: MSet[Int] = MSet.empty[Int])

    /** A scala.collection.mutable.Builder implementation
      */
    class PolynomialEvaluatorBuilder extends mutable.Builder[PathAndWeight, PolynomialEvaluator] {

        /**
         *
         */
        private[PolynomialEvaluator] val value = new MutableValue

        /**
         *
         */
        private[PolynomialEvaluator] val descendants = MMap.empty[String, PolynomialEvaluatorBuilder]

        /** Add an element to the builder.
          * @param p A path in the tree and the associated weight.  Said another way, this is a term in the polynomial
          *          and the associated coefficient.
          * @return
          */
        def +=(p: PathAndWeight) = {
            addHelper(this, p._1.toVector.sortWith(_._1 < _._1).toList, p._2)
            this
        }

        /** Clear the builder.
          */
        def clear() {
            value.weight = 0
            value.featureIndices.clear()
            descendants.clear()
        }

        /** Build.
          * @return
          */
        def result() = {
            val (nodes, childMap) = toTabularStructure
            val t = Tree[PolynomialEvaluatorBuilder, ({type L[+V] = Map[String, V]})#L, PolynomialEvaluator](nodes, childMap, 0, buildHelper)
            t
        }

        /** Add first order terms.
          * @param ps set of paths of length one.
          * @param n the size of x passed to [[com.eharmony.aloha.models.reg.PolynomialEvaluationAlgo]].at
          * @return
          */
        def addAllFirstOrder(ps: Iterable[(String, Double)], n: Int) = {
            value.featureIndices ++= (0 until n)
            ps foreach {case (k,v) => this += ((List((k, 0)), v)) }
            this
        }

        /** The builder method passed to [[com.eharmony.aloha.models.tree.Tree]] object's apply method.
          */
        private[this] val buildHelper = (raw: PolynomialEvaluatorBuilder, rawChildren: Seq[PolynomialEvaluatorBuilder], children: Seq[PolynomialEvaluator]) => {
            val m = rawChildren.zipWithIndex.toMap
            val d = raw.descendants.map{case(k,v) => (k, children(m(v)))}.toMap
            PolynomialEvaluator(Coefficient(raw.value.weight, raw.value.featureIndices.toIndexedSeq.sortWith(_ < _)), d)
        }

        /** Tail-recursive helper method use to add a term to the polynomial.
          *
          * @param t This builder.
          * @param path The term in the polynomial to add (this is isomorphic to a path in the tree)
          * @param wt the weight associated with the term.
          */
        private[this] def addHelper(t: PolynomialEvaluatorBuilder, path: Path, wt: Double) {
            path match {
                case Nil => t.value.weight = wt
                case (s, i) :: tail =>
                    t.value.featureIndices += i
                    addHelper(t.descendants.getOrElseUpdate(s, new PolynomialEvaluatorBuilder), tail, wt)
            }
        }

        /** Turn the tree represented by this structure into a table-based structure with a lookup map so that we
          * can use the [[com.eharmony.aloha.models.tree.Tree]] objects apply method to build an immutable
          * version.
          * @return
          */
        private[this] def toTabularStructure: (IndexedSeq[PolynomialEvaluatorBuilder], Map[Int, Seq[Int]]) = {
            def h(q: MQueue[PolynomialEvaluatorBuilder], b: ArrayBuffer[PolynomialEvaluatorBuilder], pInd: MMap[Int, Seq[Int]], n: Int): (IndexedSeq[PolynomialEvaluatorBuilder], Map[Int, Seq[Int]]) = {
                if (q.isEmpty) (b, pInd.toMap)
                else {
                    val f = q.dequeue()
                    val d = f.descendants.values.toSeq
                    val m = b.size
                    val ids = ((n + 1) to (n + d.size)).toSeq
                    h(q ++= d, b += f, pInd += (m -> ids), n + d.size)
                }
            }
            h(MQueue(this), ArrayBuffer.empty[PolynomialEvaluatorBuilder], MMap.empty[Int, Seq[Int]], 0)
        }

        override def toString() = (value, descendants).toString()
    }
}
