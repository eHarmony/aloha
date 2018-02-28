package com.eharmony.aloha.semantics.compiled.plugin.csv

import scala.collection.immutable
import scala.util.Try


sealed trait OptionalHandler {
    def produceOption[A](fieldName: String, f: Option[String => A], fields: Array[String], missing: String => Boolean): Option[A]
    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]]
}

case class GracefulOptionalHandler(indices: Map[String, Int]) extends OptionalHandler {
    def produceOption[A](
            fieldName: String,
            f: Option[String => A],
            fields: Array[String],
            missing: String => Boolean
    ): Option[A] = {
        for {
            g <- f
            i <- indices.get(fieldName)
            field = fields(i) if !missing(field)
            x <- Try { g(field) }.toOption
        } yield x
    }

    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]] = {
        f.map( g =>
            vals.map( v =>
                if (missing(v))
                    None
                else Try { g(v) }.toOption
            )(scala.collection.breakOut)
        ) getOrElse RepeatedIndexedSeq.fill(vals.length)(None)
    }
}

case class FailFastOptionalHandler(indices: Map[String, Int]) extends OptionalHandler {
    def produceOption[A](
            fieldName: String,
            f: Option[String => A],
            fields: Array[String],
            missing: String => Boolean
    ): Option[A] = {
        f.flatMap{ g =>
            val field = fields(indices(fieldName))
            if (missing(field))
                None
            else Option(g(field))
        }
    }

    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]] = {
        f.map( g =>
            vals.map( v =>
                if (missing(v))
                    None
                else Some(g(v))
            )(scala.collection.breakOut)
        ) getOrElse RepeatedIndexedSeq.fill(vals.length)(None)
    }
}

case class RepeatedIndexedSeq[+A](length: Int, a: A) extends immutable.IndexedSeq[A] {
    def apply(i: Int): A =
        if (0 <= i && i < length)
            a
        else throw new ArrayIndexOutOfBoundsException(s"index $i not in range 0 ... ${length - 1}")
}

object RepeatedIndexedSeq {
    def fill[A](n: Int)(a: A) = RepeatedIndexedSeq(n, a)
}
