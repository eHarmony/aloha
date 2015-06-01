package com.eharmony.matching.featureSpecExtractor.csv.encoding

import spray.json._
import spray.json.DefaultJsonProtocol.lift

sealed trait Encoding {
    def finalizer[A](sep: String, nullString: String, values: Iterable[String]): Option[A] => String
}

object Encoding {
    def hotOne: Encoding = HotOneEncoding
    def regular: Encoding = RegularEncoding
    def thermometer: Encoding = ThermometerEncoding

    implicit val encodingJsonFormat: JsonFormat[Encoding] = lift(new JsonReader[Encoding] {
        override def read(json: JsValue): Encoding = json match {
            case JsString("regular") => RegularEncoding
            case JsString("hotOne") => HotOneEncoding
            case JsString("thermometer") => ThermometerEncoding
            case d => deserializationError(s"The following encodings are supported: 'regular', 'hotOne', 'thermometer'.  Given: '${d.compactPrint}'." )
        }
    })
}

// TODO: Fill in
case object ThermometerEncoding extends Encoding {
    override def finalizer[A](sep: String, nullString: String, values: Iterable[String]) =
        throw new UnsupportedOperationException("not implemented!")
}

case object HotOneEncoding extends Encoding {
    private[this] case class HotOne[A](sep: String, ind: Map[String, Int]) extends (Option[A] => String) {
        private[this] val n = ind.size
        private[this] def missing(n: Int, sep: String) = (0 to n).mkString(sep)

        private[this] def densify(i: Int, n: Int, sep: String): String = {
            val b = new StringBuilder
            var j = 0
            while (j < i) {
                b.append(0).append(sep)
                j += 1
            }
            b.append(1)
            j += 1
            while (j < n) {
                b.append(sep).append(0)
                j += 1
            }
            b.toString()
        }

        def apply(x: Option[A]) =
            x.fold(missing(n, sep))(a =>
                ind.get(a.toString).
                    fold(missing(n, sep))(i => densify(i, n, sep)))
    }

    def finalizer[A](sep: String, nullString: String, values: Iterable[String]): Option[A] => String = HotOne[A](sep, values.view.zipWithIndex.toMap)
}

case object RegularEncoding extends Encoding {
    private[this] case class Regular[A](nullString: String, ok: Set[String]) extends (Option[A] => String) {
        def apply(o: Option[A]) =
            o.fold(nullString){ v =>
                val s = v.toString
                if (ok contains s) s else nullString
            }
    }

    def finalizer[A](sep: String, nullString: String, values: Iterable[String]): Option[A] => String = Regular[A](nullString, values.toSet)
}
