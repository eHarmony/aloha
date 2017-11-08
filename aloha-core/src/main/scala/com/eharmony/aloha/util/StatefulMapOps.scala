package com.eharmony.aloha.util

import scala.collection.{breakOut, SeqLike, immutable => sci}
import scala.collection.generic.{CanBuildFrom => CBF}

/**
  * Some ways to map with state without needing extra libraries like ''cats'' or ''scalaz''.
  * @author deaktator
  * @since 11/8/2017
  */
private[aloha] object StatefulMapOps {

  /**
    * Map over `as` while maintaining a running state to produce `(B, S)` pairs contained
    * within some container type.
    *
    * Note that the input container type `In` determines the strictness of this method.
    * For instance, this can produce values ''strictly'' with a strict sequence like
    * `Vector` or ''non-strictly'' for a non-strict sequence like a `Stream`.
    * For instance:
    *
    * {{{
    * val sCycleModulo4 = Stream.iterate(0)(s => (s + 1) % 4)
    * val vCycleModulo4 = Vector.iterate(0, 10)(s => (s + 1) % 4)
    * val addAndIncrement = (a: Int, s: Int) => ((a + s).toDouble, s + 1)
    *
    *
    * // Non-strict, so it returns even though it's an infinite stream.  O(1).
    * // type: Stream[(Double, Int)]
    * val stream = statefulMap(sCycleModulo4, 0)(addAndIncrement)
    *
    * // Strict, so it's runtime is O(N), where N = vCycleModulo4.size.
    * import scala.collection.breakOut
    * val list: List[(Double, Int)] =
    *   statefulMap(vCycleModulo4, 0)(addAndIncrement)(breakOut)
    * }}}
    *
    * @param as elements to map (given some state that may change).  '''NOTE''': the first
    *           element of `as` ''will be forced'' in this method in order to construct the
    *           output.
    * @param startState the initial state.
    * @param f a function that maps a value and a state and to an output and a new state.
    * @param cbf object responsible for building the output.
    * @tparam A the input element type
    * @tparam B the output element type
    * @tparam S the state type
    * @tparam In a concrete implementation of the input sequence.
    * @tparam Out the output container type (including type parameters).
    * @return an output container with the `A`s mapped to `B`s and the state resulting
    *         from applying `f` and getting the second element (''e.g.'': `f(a, s)._2`).
    */
  def statefulMap[A, B, S, In <: sci.Seq[A], Out](as: SeqLike[A, In], startState: S)
                                                 (f: (A, S) => (B, S))
                                                 (implicit cbf: CBF[In, (B, S), Out]): Out = {
    if (as.isEmpty)
      cbf().result()
    else {
      val initEl = f(as.head, startState)
      as.tail.scanLeft(initEl){ case ((_, newState), a) => f(a, newState) }(breakOut)
    }
  }


  /**
    * Map over `as` while maintaining a running state to produce `(B, S)` pairs contained
    * within some container type.
    *
    * {{{
    * val sCycleModulo4 = Iterator.iterate(0)(s => (s + 1) % 4)
    * val addAndIncrement = (a: Int, s: Int) => ((a + s).toDouble, s + 1)
    *
    * val stream = statefulMap(sCycleModulo4, 0)(addAndIncrement)
    * }}}
    * @param as elements to map (given some state that may change).  '''NOTE''': the first
    *           element of `as` ''will be forced'' in this method in order to construct the
    *           output.
    * @param startState the initial state.
    * @param f a function that maps a value and a state and to an output and a new state.
    * @tparam A the input element type
    * @tparam B the output element type
    * @tparam S the state type
    * @return an iterator with the `A`s mapped to `B`s and the state resulting
    *         from applying `f` and getting the second element (''e.g.'': `f(a, s)._2`).
    */
  def statefulMap[A, B, S](as: Iterator[A], startState: S)(f: (A, S) => (B, S)): Iterator[(B, S)] = {
    if (!as.hasNext)
      Iterator.empty
    else {
      val initEl = f(as.next(), startState)
      as.scanLeft(initEl){ case ((_, newState), a) => f(a, newState) }
    }
  }
}
