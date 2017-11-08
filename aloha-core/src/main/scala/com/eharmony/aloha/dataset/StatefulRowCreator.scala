package com.eharmony.aloha.dataset

/**
  * A row creator that requires state.  This state should be modeled functionally, meaning
  * implementations should be referentially transparent.
  *
  * Created by ryan.deak on 11/2/17.
  */
trait StatefulRowCreator[-A, +B, @specialized(Int, Float, Long, Double) S] extends Serializable {

  /**
    * Some initial state that can be used on the very first call to `apply(A, S)`.
    * @return some state.
    */
  val initialState: S

  /**
    * Given an `a` and some `state`, produce output, including a new state.
    *
    * When using this function, the user is responsible for keeping track of,
    * and providing the state.
    *
    * The implementation of this function should be referentially transparent.
    *
    * @param a input
    * @param state the state
    * @return a tuple where the first element is a Tuple2 whose first element is
    *         missing and error information and second element is an optional result.
    *         The second element of the outer Tuple2 is the new state.
    */
  def apply(a: A, state: S): ((MissingAndErroneousFeatureInfo, Option[B]), S)

  /**
    * Apply the `apply(A, S)` method to the elements of the iterator.  In the first
    * application of `apply(A, S)`, `state` will be used as the state.  In subsequent
    * applications, the state will come from the state generated in the output of the
    * previous application of `apply(A, S)`.
    *
    * ''This variant of mapping with state is'' '''non-strict''', so if that's a requirement,
    * prefer this function over the `mapSeqWithState` variant.  Note that for this method
    * to work, the first element is computed eagerly.
    *
    * To verify non-strictness, this method could be rewritten as:
    *
    * {{{
    * def statefulMap[A, B, S](as: Iterator[A], s: S)
    *                         (f: (A, S) => (B, S)): Iterator[(B, S)] = {
    *   if (as.isEmpty)
    *     Iterator.empty
    *   else {
    *     val firstA = as.next()
    *     val initEl = f(firstA, s)
    *     as.scanLeft(initEl){ case ((_, newS), a) => f(a, newS) }
    *   }
    * }
    * }}}
    *
    * Then using the method, it's easy to verify non-strictness:
    *
    * {{{
    * // cycleModulo4 and res are infinite.
    * val cycleModulo4 = Iterator.iterate(0)(s => (s + 1) % 4)
    * val res = statefulMap(cycleModulo4, 0)((a, s) => ((a + s).toDouble, s + 1))
    *
    * // returns:
    * res.take(8)
    *    .map(_._1)
    *    .toVector
    *    .groupBy(identity)
    *    .mapValues(_.size)
    *    .toVector
    *    .sorted
    *    .foreach(println)
    *
    * res.size // never returns
    * }}}
    *
    * @param as Note the first element of `as` ''will be forced'' in this method in order
    *           to construct the output.
    * @param state the initial state to use at the start of the iterator.
    * @return an iterator containing the `a` mapped to a
    *         `(MissingAndErroneousFeatureInfo, Option[B])` along with the resulting
    *         state that is created in the process.
    */
  def mapIteratorWithState(as: Iterator[A], state: S): Iterator[((MissingAndErroneousFeatureInfo, Option[B]), S)] = {
    if (!as.hasNext)
      Iterator.empty
    else {
      // Force the first A.  Then apply the `apply` transformation to get
      // the initial element of a scanLeft.  Inside the scanLeft, use the
      // state outputted by previous `apply` calls as input to current
      // calls to `apply(A, S)`.
      val firstA = as.next()
      val initEl = apply(firstA, state)
      as.scanLeft(initEl){ case ((_, mostRecentState), a) => apply(a, mostRecentState) }
    }
  }

  /**
    * Apply the `apply(A, S)` method to the elements of the sequence.  In the first
    * application of `apply(A, S)`, `state` will be used as the state.  In subsequent
    * applications, the state will come from the state generated in the output of the
    * previous application of `apply(A, S)`.
    *
    * ''This variant of mapping with state is'' '''strict'''.
    *
    * '''NOTE''': This method isn't really parallelizable via chunking.  The way to
    * parallelize this method is to provide a separate starting state for each unit
    * of parallelism.
    *
    * @param as input to map.
    * @param state the initial state to use at the start of the Vector.
    * @return a Tuple2 where the first element is a vector of results and the second
    *         element is the resulting state.
    */
  def mapSeqWithState(as: Seq[A], state: S): (Vector[(MissingAndErroneousFeatureInfo, Option[B])], S) = {
    as.foldLeft((Vector.empty[(MissingAndErroneousFeatureInfo, Option[B])], state)){
      case ((bs, s), a) =>
        val (b, newS) = apply(a, s)
        (bs :+ b, newS)
    }
  }
}
