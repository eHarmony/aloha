package com.eharmony.aloha.semantics.compiled.plugin.csv

import scala.collection.immutable

/** A representation of one line of CSV data.
  * @param line The actual string line representing the data.
  * @param indices A mapping from column name to column index
  * @param enums A mapping from column name to enumerated type.  This allows java-like enum syntax to be used
  * @param fieldSeparator the between-column separator
  * @param intraFieldSeparator separator within a column that allows vector-based input for given columns
  * @param missingId a function that determines if the data in a given field or in a given position in a vector-based
  *                  field is missing.
  * @param optEnumFunc a function that given a field name optionally produces a function from field value to an
  *                    enumerated type.
  * @param optHandler an object responsible for the failure policy when an optional field or a field of vector
  *                   of optional is provided with bad data.
  */
case class CsvLineImpl(
        line: String,
        indices: Map[String, Int],
        enums: Map[String, Enum],
        fieldSeparator: String,
        intraFieldSeparator: String,
        missingId: String => Boolean,
        optEnumFunc: String => Option[String => EnumConstant],
        optHandler: OptionalHandler) extends CsvLine {

    import CsvLineImpl._
    private[this] val fields = line.split(fieldSeparator, -1)

    def e(fieldName: String) = reqField(fieldName, enums(fieldName).valueOf)
    def b(fieldName: String) = reqField(fieldName, fb)

    /** Currently, only base-10 integers are supported.
      * @param fieldName a field name of a field containing a string representation of a base-10 integer literal.
      * @return an integer
      */
    def i(fieldName: String) = reqField(fieldName, fi)

    /** Currently, only base-10 longs are supported.
      * @param fieldName a field name of a field containing a string representation of a base-10 long literal.  Note that
      * @return a long
      */
    def l(fieldName: String) = reqField(fieldName, fl)
    def f(fieldName: String) = reqField(fieldName, ff)
    def d(fieldName: String) = reqField(fieldName, fd)
    def s(fieldName: String) = reqField(fieldName, fs)

    private[this] def reqField[A](fieldName: String, f: String => A) = f(fields(indices(fieldName)))

    def oe(fieldName: String) = optField(fieldName, optEnumFunc(fieldName))
    def ob(fieldName: String) = optField(fieldName, fob)
    def oi(fieldName: String) = optField(fieldName, foi)
    def ol(fieldName: String) = optField(fieldName, fol)
    def of(fieldName: String) = optField(fieldName, fof)
    def od(fieldName: String) = optField(fieldName, fod)
    def os(fieldName: String) = optField(fieldName, fos)

    private[this] def optField[A](s: String, f: Option[String => A]): Option[A] = optHandler.produceOption(s, f, fields)

    def ve(fieldName: String) = vecField(fieldName, enums(fieldName).valueOf)
    def vb(fieldName: String) = vecField(fieldName, fb)
    def vi(fieldName: String) = vecField(fieldName, fi)
    def vl(fieldName: String) = vecField(fieldName, fl)
    def vf(fieldName: String) = vecField(fieldName, ff)
    def vd(fieldName: String) = vecField(fieldName, fd)
    def vs(fieldName: String) = vecField(fieldName, fs)

    private[this] def vecField[A](s: String, f: String => A): immutable.IndexedSeq[A] = {
        val fs = fields(indices(s))
        val v = if (fs.trim.isEmpty) Vector.empty[A]
                else fs.split(intraFieldSeparator, -1).map(f).toVector
        v
    }

    def voe(fieldName: String): immutable.IndexedSeq[Option[EnumConstant]] = vecOptField(fieldName, enums.get(fieldName).map(_.valueOf))
    def vob(fieldName: String): immutable.IndexedSeq[Option[Boolean]] = vecOptField(fieldName, fob)
    def voi(fieldName: String): immutable.IndexedSeq[Option[Int]] = vecOptField(fieldName, foi)
    def vol(fieldName: String): immutable.IndexedSeq[Option[Long]] = vecOptField(fieldName, fol)
    def vof(fieldName: String): immutable.IndexedSeq[Option[Float]] = vecOptField(fieldName, fof)
    def vod(fieldName: String): immutable.IndexedSeq[Option[Double]] = vecOptField(fieldName, fod)
    def vos(fieldName: String): immutable.IndexedSeq[Option[String]] = vecOptField(fieldName, fos)

    private[this] def vecOptField[A](s: String, f: Option[String => A]): immutable.IndexedSeq[Option[A]] = {
        val fs = fields(indices(s))
        val v = if (fs.trim.isEmpty) immutable.IndexedSeq.empty[Option[A]]
        else optHandler.produceOptions(fs.split(intraFieldSeparator, -1), f, missingId)
        v
    }
}

private object CsvLineImpl {
    private val fb = (_:String).toBoolean
    private val fi = (_:String).toInt
    private val fl = (_:String).toLong
    private val ff = (_:String).toFloat
    private val fd = (_:String).toDouble
    private val fs = (s: String) => s

    private val fob = Option(fb)
    private val foi = Option(fi)
    private val fol = Option(fl)
    private val fof = Option(ff)
    private val fod = Option(fd)
    private val fos = Option(fs)
}
