package com.eharmony.aloha.semantics.compiled.plugin.csv

import scala.collection.immutable

trait CsvLine {
    def e(s: String): EnumConstant
    def b(s: String): Boolean
    def i(s: String): Int
    def l(s: String): Long
    def f(s: String): Float
    def d(s: String): Double
    def s(s: String): String

    def oe(s: String): Option[EnumConstant]
    def ob(s: String): Option[Boolean]
    def oi(s: String): Option[Int]
    def ol(s: String): Option[Long]
    def of(s: String): Option[Float]
    def od(s: String): Option[Double]
    def os(s: String): Option[String]

    def ve(s: String): immutable.IndexedSeq[EnumConstant]
    def vb(s: String): immutable.IndexedSeq[Boolean]
    def vi(s: String): immutable.IndexedSeq[Int]
    def vl(s: String): immutable.IndexedSeq[Long]
    def vf(s: String): immutable.IndexedSeq[Float]
    def vd(s: String): immutable.IndexedSeq[Double]
    def vs(s: String): immutable.IndexedSeq[String]

    def voe(s: String): immutable.IndexedSeq[Option[EnumConstant]]
    def vob(s: String): immutable.IndexedSeq[Option[Boolean]]
    def voi(s: String): immutable.IndexedSeq[Option[Int]]
    def vol(s: String): immutable.IndexedSeq[Option[Long]]
    def vof(s: String): immutable.IndexedSeq[Option[Float]]
    def vod(s: String): immutable.IndexedSeq[Option[Double]]
    def vos(s: String): immutable.IndexedSeq[Option[String]]

    // ove, ..., ovs
    // ovoe, ..., ovos
}
