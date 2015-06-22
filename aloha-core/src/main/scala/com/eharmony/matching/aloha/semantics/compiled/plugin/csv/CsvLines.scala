package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import scala.annotation.varargs
import scala.collection.{TraversableLike, GenTraversableLike}
import scala.collection.generic.CanBuildFrom

/** A class capable of efficiently creating CsvLine objects.
  * @param indices a mapping from field name to field index (0-based)
  * @param enums a mapping from field name to emulated Enum type.
  * @param fs field separator (between fields)
  * @param ifs intra-field separator (within fields, for use in sequence data)
  * @param missingData a function that determines if the data in an optional field is considered missing
  * @param errorOnOptMissingField should an error occur when an optional field is request for a non-existent column name.
  * @param errorOnOptMissingEnum should an error occur when an optional enum field is request for a column not
  *                              associated with any enum.
  */
case class CsvLines(
        indices: Map[String, Int],
        enums: Map[String, Enum] = Map.empty,
        fs: String = "\t",
        ifs: String = ",",
        missingData: String => Boolean = _ == "",
        errorOnOptMissingField: Boolean = false,
        errorOnOptMissingEnum: Boolean = false) {

    private[this] val optEnumFunc: String => Option[(String) => EnumConstant] =
        if (errorOnOptMissingField) (s: String) => if (missingData(s)) None else Option(enums(s).valueOf)
        else (s: String) => if (missingData(s)) None else enums.get(s).map(_.valueOf)

    private[this] val optHandler = if (errorOnOptMissingField) FailFastOptionalHandler(indices) else GracefulOptionalHandler(indices)

    /** Generate a
      * @param t
      * @param cbf
      * @tparam Repr
      * @tparam That
      * @return
      */
    def apply[Repr, That](t: GenTraversableLike[String, Repr])(implicit cbf: CanBuildFrom[Repr, CsvLine, That]) =
        t.map(CsvLineImpl(_, indices, enums, fs, ifs, missingData, optEnumFunc, optHandler))(cbf)

    /**
     * A non-strict version of the GenTraversableLike method for TraversableLike values.
     * @param t
     * @param cbf
     * @tparam Repr
     * @tparam That
     * @return
     */
    def nonStrict[Repr, That](t: TraversableLike[String, Repr])(implicit cbf: CanBuildFrom[Repr, CsvLine, That]) =
        t.view.map(CsvLineImpl(_, indices, enums, fs, ifs, missingData, optEnumFunc, optHandler))

    /**
     *
     * @param t
     * @return
     */
    def apply(t: Iterator[String]) = t.map(CsvLineImpl(_, indices, enums, fs, ifs, missingData, optEnumFunc, optHandler))

    /**
     *
     * @param s
     * @return
     */
    def apply(s: String) = CsvLineImpl(s, indices, enums, fs, ifs, missingData, optEnumFunc, optHandler)

    /**
     *
     * @param first
     * @param rest
     * @return
     */
    @varargs def apply(first: String, rest: String*) =
        (first +: rest.toVector).map(CsvLineImpl(_, indices, enums, fs, ifs, missingData, optEnumFunc, optHandler))
}
