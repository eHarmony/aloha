package com.eharmony.aloha.feature

import scala.language.implicitConversions

/**
  * These can be useful for models requiring implicit feature conversion a "''primitive''" type
  * to an option of that type.  This is most useful for when we need to be able to supply a
  * default feature value without knowing anything about the type.  If the type is in a context
  * then the zero element for the context can be used without additional information about the
  * inner type.
  *
  * This definition allows the feature author to be sloppy about types so there is no blanket
  * method:
  *
  * {{{
  * implicit def liftToOption[A](a: A): Option[A] = Option(a)
  * }}}
  *
  * @author rdeak
  */
trait OptionLifting {
  implicit def double2Option(a: Double): Option[Double] = Option(a)
  implicit def string2Option(a: String): Option[String] = Option(a)
  implicit def byte2Option(a: Byte): Option[Byte] = Option(a)
  implicit def short2Option(a: Short): Option[Short] = Option(a)
  implicit def int2Option(a: Int): Option[Int] = Option(a)
  implicit def long2Option(a: Long): Option[Long] = Option(a)
  implicit def float2Option(a: Float): Option[Float] = Option(a)
}
