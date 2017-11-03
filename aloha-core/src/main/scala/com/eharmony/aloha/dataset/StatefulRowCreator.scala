package com.eharmony.aloha.dataset

/**
  * Created by ryan.deak on 11/2/17.
  */
trait StatefulRowCreator[-A, +B, S] extends Serializable {
  def initialState: S

  def apply(a: A, s: S): ((MissingAndErroneousFeatureInfo, Option[B]), S)

  def apply(as: Iterator[A], s: S): Iterator[(MissingAndErroneousFeatureInfo, Option[B])]

  def apply(as: Vector[A], s: S): (Vector[(MissingAndErroneousFeatureInfo, Option[B])], S)
}
