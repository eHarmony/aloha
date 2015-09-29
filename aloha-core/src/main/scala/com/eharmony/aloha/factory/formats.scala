package com.eharmony.aloha.factory

import java.{lang => jl, util => ju}

import spray.json.DefaultJsonProtocol.{BooleanJsonFormat, ByteJsonFormat, DoubleJsonFormat, FloatJsonFormat, IntJsonFormat, LongJsonFormat, ShortJsonFormat, seqFormat}
import spray.json._

import scala.collection.JavaConversions
import scala.collection.immutable.ListMap

object Formats
    extends JavaJsonFormats
    with ScalaJsonFormats

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

    implicit def javaListJsonFormat[A : JsonFormat]: JsonFormat[ju.List[A]] = new JsonFormat[ju.List[A]] {
        def write(l: ju.List[A]) = JsArray(JavaConversions.asScalaBuffer(l).view.map(_.toJson).toVector)
        def read(json: JsValue) = JavaConversions.seqAsJavaList(json.convertTo[Seq[A]])
    }
}

object ScalaJsonFormats extends ScalaJsonFormats

trait ScalaJsonFormats {
    implicit def listMapFormat[K :JsonFormat, V :JsonFormat] = new RootJsonFormat[ListMap[K, V]] {
        def write(m: ListMap[K, V]) = JsObject {
            m.map { field =>
                field._1.toJson match {
                    case JsString(x) => x -> field._2.toJson
                    case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
                }
            }
        }

        @throws[DeserializationException](cause = "When duplicate keys are detected in the map to be created.")
        def read(value: JsValue) = value match {
            case x: JsObject =>
                val duplicateKeys =
                    x.fields.view.unzip._1.groupBy(identity).collect{ case (k, v) if v.size > 1 => k }.toSeq.sorted

                if (duplicateKeys.nonEmpty)
                    deserializationError(s"ListMap to be deserialized has duplicate keys: ${duplicateKeys.mkString(", ")}.")

                x.fields.map {
                    field => (JsString(field._1).convertTo[K], field._2.convertTo[V])
                } (collection.breakOut)
            case x => deserializationError("Expected Map as JsObject, but got " + x)
        }
    }
}
