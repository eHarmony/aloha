package com.eharmony.aloha.dataset.csv.encoding

import com.eharmony.aloha.dataset.csv.json._
import spray.json._
import spray.json.DefaultJsonProtocol.lift

import scala.collection.{breakOut, immutable => sci}

private[encoding] sealed trait VectorHeaders {
    def vectorHeaders(n: Int, name: String): sci.IndexedSeq[String] =
        (0 until n) map (i => s"${name}_$i")
}

sealed trait Encoding {

    /**
      * Creates a single string representing the entire feature output.
      * @param sep a column separator
      * @param nullString the string representing null values.
      * @param values
      * @tparam A
      * @return
      */
    def finalizer[A](sep: String, nullString: String, values: Seq[String]): Option[A] => String

    /**
      * Creates a sequence of strings representing the columns created by a feature's output.
      * @param nullString the string representing null values.
      * @param values
      * @tparam A
      * @return
      */
    def columnarFinalizer[A](nullString: String, values: Seq[String]): Option[A] => Seq[String]

    /**
      * Creates a Seq[Option[Any]] representing the columns created by a feature's output.
      * @param values
      * @tparam A
      * @return
      */
    def anyColumnarFinalizer[A](values: Seq[String]): Option[A] => Seq[Option[Any]]

    def csvHeadersForColumn(c: CsvColumn): Seq[String]
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
    override def finalizer[A](sep: String, nullString: String, values: Seq[String]) =
        throw new UnsupportedOperationException("ThermometerEncoding not implemented!")

    override def columnarFinalizer[A](nullString: String, values: Seq[String]) =
        throw new UnsupportedOperationException("ThermometerEncoding not implemented!")

    override def anyColumnarFinalizer[A](values: Seq[String]) =
        throw new UnsupportedOperationException("ThermometerEncoding not implemented!")

    override def csvHeadersForColumn(c: CsvColumn): Seq[String] =
        throw new UnsupportedOperationException("ThermometerEncoding not implemented!")
}

case object HotOneEncoding extends Encoding with VectorHeaders {
    final case class HotOneVector[A](zero: A, one: A, oneIndex: Int, length: Int) extends scala.collection.immutable.IndexedSeq[A] {
        override def apply(idx: Int): A =
            if (idx < length)
                if (idx == oneIndex) one
                else zero
            else throw new ArrayIndexOutOfBoundsException(idx)
    }

    final case class Repeated[A](length: Int, a: A) extends scala.collection.immutable.IndexedSeq[A] {
        override def apply(idx: Int): A =
            if (idx < length)
                a
            else throw new ArrayIndexOutOfBoundsException(idx)
    }

    private[this] final case class HotOneVec1[A](ind: Map[String, Int]) extends (Option[A] => sci.IndexedSeq[Option[Int]]) {
        private[this] val n = ind.size
        def apply(x: Option[A]): sci.IndexedSeq[Option[Int]] = {
            x.map{ v =>
                ind.get(v.toString)
                  .map(i => HotOneVector(Option(0), Option(1), i, n))
                  .getOrElse(Repeated(n, Option.empty[Int]))
            }.getOrElse(Repeated(n, Option.empty[Int]))
        }
    }


    private[this] final case class HotOneVec[A](ind: Map[String, Int]) extends (Option[A] => Seq[String]) {
        private[this] val n = ind.size
        private[this] val missing = Stream.fill(n)("0")

        private[this] def densifyVector(i: Int, n: Int): Seq[String] =
            (Stream.fill(i)("0") :+ "1") ++ Stream.fill(n - i - 1)("0")

        def apply(x: Option[A]): Seq[String] = {
            val vec = for {
                a <- x
                i <- ind.get(a.toString)
            } yield densifyVector(i, n)
            vec.getOrElse(missing)
        }
    }

    private[this] case class HotOne[A](sep: String, ind: Map[String, Int]) extends (Option[A] => String) {
        private[this] val n = ind.size
        private[this] val missing = Iterator.fill(n)("0").mkString(sep)

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

        def apply(x: Option[A]): String = {
            val vec = for {
                a <- x
                i <- ind.get(a.toString)
            } yield densify(i, n, sep)
            vec.getOrElse(missing)
        }
    }

    override def csvHeadersForColumn(c: CsvColumn): Seq[String] = c match {
        case e@EnumCsvColumn(name, _, _)                => e.values.map(v => s"${name}_$v")
        case SyntheticEnumCsvColumn(name, _, values, _) => values.map(v => s"${name}_$v")
        case SeqCsvColumnWithNoDefault(_, _, n)         => vectorHeaders(n, c.name)
        case OptionSeqCsvColumnWithNoDefault(_, _, n)   => vectorHeaders(n, c.name)
        case d                                          => Seq(d.name)
    }

    override def finalizer[A](sep: String, nullString: String, values: Seq[String]): Option[A] => String =
        HotOne[A](sep, values.zipWithIndex(breakOut[Seq[String], (String, Int), Map[String, Int]]))

    override def columnarFinalizer[A](nullString: String, values: Seq[String]): Option[A] => Seq[String] =
        HotOneVec[A](values.zipWithIndex(breakOut[Seq[String], (String, Int), Map[String, Int]]))

    override def anyColumnarFinalizer[A](values: Seq[String]): Option[A] => Seq[Option[Any]] =
        HotOneVec1[A](values.zipWithIndex(breakOut[Seq[String], (String, Int), Map[String, Int]]))
}

case object RegularEncoding extends Encoding with VectorHeaders {
    private[this] case class Regular[A](nullString: String, ok: Set[String]) extends (Option[A] => String) {
        def apply(o: Option[A]): String =
            o.fold(nullString){ v =>
                val s = v.toString
                if (ok contains s) s else nullString
            }
    }

    private[this] case class RegularVec[A](nullString: String, ok: Set[String]) extends (Option[A] => Seq[String]) {
        def apply(o: Option[A]): Seq[String] = {
            o.fold(List(nullString)) { v =>
                val s = v.toString
                val value = if (ok contains s) s else nullString
                List(value)
            }
        }
    }

    private[this] case class RegularVec1[A](ok: Set[String]) extends (Option[A] => Seq[Option[A]]) {
        def apply(o: Option[A]): Seq[Option[A]] = {
            o.fold(List(Option.empty[A])){ v =>
                if (ok contains v.toString)
                    List(Option(v))
                else List(Option.empty[A])
            }
        }
    }


    override def csvHeadersForColumn(c: CsvColumn): Seq[String] = {
        c match {
            case SeqCsvColumnWithNoDefault(_, _, n)       => vectorHeaders(n, c.name)
            case OptionSeqCsvColumnWithNoDefault(_, _, n) => vectorHeaders(n, c.name)
            case d                                        => Seq(d.name)
        }
    }

    override def finalizer[A](sep: String, nullString: String, values: Seq[String]): Option[A] => String =
        Regular[A](nullString, values.toSet)

    override def columnarFinalizer[A](nullString: String, values: Seq[String]): Option[A] => Seq[String] =
        RegularVec[A](nullString, values.toSet)

    override def anyColumnarFinalizer[A](values: Seq[String]): Option[A] => Seq[Option[Any]] =
        RegularVec1[A](values.toSet)
}
