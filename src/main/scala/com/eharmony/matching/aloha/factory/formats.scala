package com.eharmony.matching.aloha.factory

import java.{lang => jl}
import spray.json.{JsValue, JsonFormat}
import spray.json.DefaultJsonProtocol.{BooleanJsonFormat, ByteJsonFormat, ShortJsonFormat, IntJsonFormat, LongJsonFormat, FloatJsonFormat, DoubleJsonFormat}

object JavaJsonFormats extends JavaJsonFormats

trait JavaJsonFormats {
    implicit object JavaBooleanJsonFormat extends JsonFormat[jl.Boolean] {
        def write(i: jl.Boolean) = BooleanJsonFormat write i
        def read(json: JsValue) = BooleanJsonFormat read json
    }

    implicit object JavaByteJsonFormat extends JsonFormat[jl.Byte] {
        def write(i: jl.Byte) = ByteJsonFormat write i
        def read(json: JsValue) = ByteJsonFormat read json
    }

    implicit object JavaShortJsonFormat extends JsonFormat[jl.Short] {
        def write(i: jl.Short) = ShortJsonFormat write i
        def read(json: JsValue) = ShortJsonFormat read json
    }

    implicit object JavaIntJsonFormat extends JsonFormat[jl.Integer] {
        def write(i: jl.Integer) = IntJsonFormat write i
        def read(json: JsValue) = IntJsonFormat read json
    }

    implicit object JavaLongJsonFormat extends JsonFormat[jl.Long] {
        def write(i: jl.Long) = LongJsonFormat write i
        def read(json: JsValue) = LongJsonFormat read json
    }

    implicit object JavaFloatJsonFormat extends JsonFormat[jl.Float] {
        def write(i: jl.Float) = FloatJsonFormat write i
        def read(json: JsValue) = FloatJsonFormat read json
    }

    implicit object JavaDoubleJsonFormat extends JsonFormat[jl.Double] {
        def write(i: jl.Double) = DoubleJsonFormat write i
        def read(json: JsValue) = DoubleJsonFormat read json
    }
}
