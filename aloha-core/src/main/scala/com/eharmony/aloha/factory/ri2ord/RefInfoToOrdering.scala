package com.eharmony.aloha.factory.ri2ord

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 2/9/17.
  */
private[aloha] sealed trait RefInfoToOrdering {
  def apply[A](implicit r: RefInfo[A]): Option[Ordering[A]] = {
    if (r == RefInfo[Boolean])         ord(Ordering.Boolean)
    else if (r == RefInfo[Byte])       ord(Ordering.Byte)
    else if (r == RefInfo[Short])      ord(Ordering.Short)
    else if (r == RefInfo[Int])        ord(Ordering.Int)
    else if (r == RefInfo[Long])       ord(Ordering.Long)
    else if (r == RefInfo[Float])      ord(Ordering.Float)
    else if (r == RefInfo[Double])     ord(Ordering.Double)
    else if (r == RefInfo[BigDecimal]) ord(Ordering.BigDecimal)
    else if (r == RefInfo[BigInt])     ord(Ordering.BigInt)
    else if (r == RefInfo[Unit])       ord(Ordering.Unit)
    else if (r == RefInfo[Char])       ord(Ordering.Char)
    else if (r == RefInfo[String])     ord(Ordering.String)
    else None
  }

  private[this] def ord[A, B](o: Ordering[A]): Option[Ordering[B]] = Option(o.asInstanceOf[Ordering[B]])
}

private[aloha] object RefInfoToOrdering extends RefInfoToOrdering
