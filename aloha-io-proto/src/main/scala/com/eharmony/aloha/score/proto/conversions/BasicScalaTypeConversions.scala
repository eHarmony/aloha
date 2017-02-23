package com.eharmony.aloha.score.proto.conversions

import scala.language.higherKinds
import com.eharmony.aloha.score.Scores.Score

trait BasicScalaTypeConversions [C[_] <: Option[_]] {
    def asBoolean(s: Score): C[Boolean]
    def asByte(s: Score): C[Byte]
    def asShort(s: Score): C[Short]
    def asInt(s: Score): C[Int]
    def asLong(s: Score): C[Long]
    def asFloat(s: Score): C[Float]
    def asDouble(s: Score): C[Double]
    def asString(s: Score): C[String]
}
