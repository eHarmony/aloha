package com.eharmony.aloha.util

import com.eharmony.aloha.reflect.RefInfo
import spray.json._
import scala.util.{Failure, Try}

sealed trait SimpleTypeSeq {
  type A
  def refInfo: RefInfo[A]
  def values: Vector[A]
  require(values.distinct.size == values.size, "Elements must be distinct.")
}

object SimpleTypeSeq {
  implicit object SimpleTypeSeqFormat extends JsonFormat[SimpleTypeSeq] {
    import DefaultJsonProtocol.{vectorFormat, LongJsonFormat, DoubleJsonFormat, BooleanJsonFormat, StringJsonFormat, BigDecimalJsonFormat}

    override def read(json: JsValue): SimpleTypeSeq = readSafe(json).get

    override def write(seq: SimpleTypeSeq): JsValue = seq match {
      case LongSeq(s)    => implicitly[JsonFormat[Vector[Long]]].write(s)
      case DoubleSeq(s)  => implicitly[JsonFormat[Vector[Double]]].write(s)
      case BooleanSeq(s) => implicitly[JsonFormat[Vector[Boolean]]].write(s)
      case StringSeq(s)  => implicitly[JsonFormat[Vector[String]]].write(s)
    }

    private[this] def readSafe(json: JsValue) = json match {
      case jsa@JsArray(values) =>
        Try { jsa.convertTo[Vector[BigDecimal]] } flatMap {
          case a if a.forall(x => x.isValidLong && !x.toString().contains(".")) => Try { LongSeq(a.map(_.toLong)) }

          // TODO: Need to extract to version specific src dirs to avoid deprecation errors.
          // TODO: isValidDouble in 2.10 and isExactDouble in 2.11.
          case a if a.forall(_.isValidDouble)                                   => Try { DoubleSeq(a.map(_.toDouble)) }
          case a                                                                => Failure(new DeserializationException(""))
        } recoverWith {
          case _ => Try { BooleanSeq(jsa.convertTo[Vector[Boolean]]) }
        } recoverWith {
          case _ => Try { StringSeq(jsa.convertTo[Vector[String]]) }
        } recoverWith {
          case _ => Failure(new DeserializationException(s"Couldn't produce SimpleTypeSeq for array: ${errorObj(jsa)}"))
        }
      case x => Failure(new DeserializationException(s"Expected Array, found ${errorObj(json)}"))
    }

    private[this] def errorObj(js: JsValue, size: Int = 50) = {
      val compact = js.compactPrint
      if (compact.length < size) compact else compact.substring(0, size) + "..."
    }
  }
}

case class LongSeq(values: Vector[Long]) extends SimpleTypeSeq {
  type A = Long
  def refInfo = RefInfo[Long]
}

object LongSeq {
  def apply(values: Long*) = new LongSeq(values.toVector)
}

case class DoubleSeq(values: Vector[Double]) extends SimpleTypeSeq {
  type A = Double
  def refInfo = RefInfo[Double]
}

object DoubleSeq {
  def apply(values: Double*) = new DoubleSeq(values.toVector)
}

case class BooleanSeq(values: Vector[Boolean]) extends SimpleTypeSeq {
  type A = Boolean
  def refInfo = RefInfo[Boolean]
}

object BooleanSeq {
  def apply(values: Boolean*) = new BooleanSeq(values.toVector)
}

case class StringSeq(values: Vector[String]) extends SimpleTypeSeq {
  type A = String
  def refInfo = RefInfo[String]
}

object StringSeq {
  def apply(values: String*) = new StringSeq(values.toVector)
}
