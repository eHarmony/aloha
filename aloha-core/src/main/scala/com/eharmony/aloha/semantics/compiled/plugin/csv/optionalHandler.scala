package com.eharmony.aloha.semantics.compiled.plugin.csv

import scala.collection.immutable


sealed trait OptionalHandler {
    def produceOption[A](fieldName: String, f: Option[String => A], fields: Array[String]): Option[A]
    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]]
}

case class GracefulOptionalHandler(indices: Map[String, Int]) extends OptionalHandler {
    def produceOption[A](fieldName: String, f: Option[String => A], fields: Array[String]): Option[A] =
        f.flatMap(g => indices.get(fieldName).flatMap(i => try { Some(g(fields(i))) } catch { case e: IllegalArgumentException => None }))

    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]] = {
        f.map(g => vals.map(v => if (missing(v)) None else try { Some(g(v)) } catch { case e: IllegalArgumentException => None }).toIndexedSeq).
            getOrElse(RepeatedIndexedSeq.fill(vals.length)(None))
    }
}

case class FailFastOptionalHandler(indices: Map[String, Int]) extends OptionalHandler {
    def produceOption[A](fieldName: String, f: Option[String => A], fields: Array[String]): Option[A] =
        f.flatMap(g => Option(g(fields(indices(fieldName)))))


    def produceOptions[A](vals: Array[String], f: Option[String => A], missing: String => Boolean): immutable.IndexedSeq[Option[A]] = {
        f.map(g => vals.map(v => if (missing(v)) None else Some(g(v))).toIndexedSeq).
            getOrElse(RepeatedIndexedSeq.fill(vals.length)(None))
    }
}

case class RepeatedIndexedSeq[+A](length: Int, a: A) extends immutable.IndexedSeq[A] {
    def apply(i: Int) = if (0 <= i && i < length) a else throw new ArrayIndexOutOfBoundsException(s"index $i not in range 0 ... ${length - 1}")
}

object RepeatedIndexedSeq {
    def fill[A](n: Int)(a: A) = RepeatedIndexedSeq(n, a)
}
