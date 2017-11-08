package com.eharmony.aloha.dataset

import com.eharmony.aloha.util.StatefulMapOps

import scala.collection.{SeqLike, immutable => sci}
import scala.collection.generic.{CanBuildFrom => CBF}

/**
  * A row creator that requires state.  This state should be modeled functionally, meaning
  * implementations should be referentially transparent.
  *
  * Created by ryan.deak on 11/2/17.
  */
trait StatefulRowCreator[-A, +B, S] extends Serializable {

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
    * For more information, see [[com.eharmony.aloha.util.StatefulMapOps]]
    *
    * @param as Note the first element of `as` ''will be forced'' in this method in order
    *           to construct the output.
    * @param state the initial state to use at the start of the iterator.
    * @return an iterator containing the `a` mapped to a
    *         `(MissingAndErroneousFeatureInfo, Option[B])` along with the resulting
    *         state that is created in the process.
    */
  def statefulMap(as: Iterator[A], state: S): Iterator[((MissingAndErroneousFeatureInfo, Option[B]), S)] =
    StatefulMapOps.statefulMap(as, state)(apply)

  /**
    * Apply the `apply(A, S)` method to the elements of the sequence.  In the first
    * application of `apply(A, S)`, `state` will be used as the state.  In subsequent
    * applications, the state will come from the state generated in the output of the
    * previous application of `apply(A, S)`.
    *
    * '''NOTE''': This method isn't really parallelizable via chunking.  The way to
    * parallelize this method is to provide a separate starting state for each unit
    * of parallelism.
    *
    * For more information, see [[com.eharmony.aloha.util.StatefulMapOps]]
    *
    * @param as input to map.
    * @param state the initial state to use at the start of mapping.
    * @param cbf object responsible for building the output collection.
    * @return 
    */
  def statefulMap[In <: sci.Seq[A], Out](as: SeqLike[A, In], state: S)(implicit
      cbf: CBF[In, ((MissingAndErroneousFeatureInfo, Option[B]), S), Out]
  ): Out = StatefulMapOps.statefulMap(as, state)(apply)
}
