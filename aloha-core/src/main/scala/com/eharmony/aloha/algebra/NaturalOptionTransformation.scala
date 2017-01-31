package com.eharmony.aloha.algebra

import scala.language.higherKinds

/**
  * Created by ryan on 1/25/17.
  */
trait NaturalOptionTransformation[F[_], G[_]] {
  def apply[A](implicit f: F[A]): Option[G[A]]
}
