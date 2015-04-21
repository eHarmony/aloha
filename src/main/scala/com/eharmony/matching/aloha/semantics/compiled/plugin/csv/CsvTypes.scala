package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

/** The string values here match the function names in CsvLine.  This is important that the name passed to
  * each instance of Val is exactly the same as the corresponding function name in
  * [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvLine]].
  * @author R. M. Deak
  */
object CsvTypes extends Enumeration {

    val EnumType: CsvType    = new Required("e")
    val BooleanType: CsvType = new Required("b")
    val IntType: CsvType     = new Required("i")
    val LongType: CsvType    = new Required("l")
    val FloatType: CsvType   = new Required("f")
    val DoubleType: CsvType  = new Required("d")
    val StringType: CsvType  = new Required("s")

    val EnumOptionType: CsvType    = new Optional("oe")
    val BooleanOptionType: CsvType = new Optional("ob")
    val IntOptionType: CsvType     = new Optional("oi")
    val LongOptionType: CsvType    = new Optional("ol")
    val FloatOptionType: CsvType   = new Optional("of")
    val DoubleOptionType: CsvType  = new Optional("od")
    val StringOptionType: CsvType  = new Optional("os")

    val EnumVectorType: CsvType    = new Required("ve")
    val BooleanVectorType: CsvType = new Required("vb")
    val IntVectorType: CsvType     = new Required("vi")
    val LongVectorType: CsvType    = new Required("vl")
    val FloatVectorType: CsvType   = new Required("vf")
    val DoubleVectorType: CsvType  = new Required("vd")
    val StringVectorType: CsvType  = new Required("vs")

    val EnumOptionVectorType: CsvType    = new Required("voe")
    val BooleanOptionVectorType: CsvType = new Required("vob")
    val IntOptionVectorType: CsvType     = new Required("voi")
    val LongOptionVectorType: CsvType    = new Required("vol")
    val FloatOptionVectorType: CsvType   = new Required("vof")
    val DoubleOptionVectorType: CsvType  = new Required("vod")
    val StringOptionVectorType: CsvType  = new Required("vos")

    sealed abstract class CsvType(name: String) extends Val(name) {
        def isRequired: Boolean
    }

    private[this] final class Required(name: String) extends CsvType(name) {
        override val isRequired: Boolean = true
    }

    private[this] final class Optional(name: String) extends CsvType(name) {
        override val isRequired: Boolean = false
    }

    /** Like withName but gives back [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes.CsvType]]
      * rather than [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes#Value]]
      * @param s a string representation of the type.
      * @return
      */
    def withNameExtended(s: String): CsvType = withName(s).asInstanceOf[CsvType]
}

//
//object CsvTypes extends Enumeration {
//    type CsvTypes = Value with CsvTypeExtensions
//
//    val EnumType    = new Val("e") with RequiredCsvType
//    val BooleanType = new Val("b") with RequiredCsvType
//    val IntType     = new Val("i") with RequiredCsvType
//    val LongType    = new Val("l") with RequiredCsvType
//    val FloatType   = new Val("f") with RequiredCsvType
//    val DoubleType  = new Val("d") with RequiredCsvType
//    val StringType  = new Val("s") with RequiredCsvType
//
//    val EnumOptionType    = new Val("oe") with OptionalCsvType
//    val BooleanOptionType = new Val("ob") with OptionalCsvType
//    val IntOptionType     = new Val("oi") with OptionalCsvType
//    val LongOptionType    = new Val("ol") with OptionalCsvType
//    val FloatOptionType   = new Val("of") with OptionalCsvType
//    val DoubleOptionType  = new Val("od") with OptionalCsvType
//    val StringOptionType  = new Val("os") with OptionalCsvType
//
//    val EnumVectorType    = new Val("ve") with RequiredCsvType
//    val BooleanVectorType = new Val("vb") with RequiredCsvType
//    val IntVectorType     = new Val("vi") with RequiredCsvType
//    val LongVectorType    = new Val("vl") with RequiredCsvType
//    val FloatVectorType   = new Val("vf") with RequiredCsvType
//    val DoubleVectorType  = new Val("vd") with RequiredCsvType
//    val StringVectorType  = new Val("vs") with RequiredCsvType
//
//    val EnumOptionVectorType    = new Val("voe") with RequiredCsvType
//    val BooleanOptionVectorType = new Val("vob") with RequiredCsvType
//    val IntOptionVectorType     = new Val("voi") with RequiredCsvType
//    val LongOptionVectorType    = new Val("vol") with RequiredCsvType
//    val FloatOptionVectorType   = new Val("vof") with RequiredCsvType
//    val DoubleOptionVectorType  = new Val("vod") with RequiredCsvType
//    val StringOptionVectorType  = new Val("vos") with RequiredCsvType
//
//    /** Adds additional information to
//      */
//    sealed trait CsvTypeExtensions {
//        def isRequired: Boolean
//    }
//
//    sealed trait RequiredCsvType extends CsvTypeExtensions {
//        override def isRequired: Boolean = true
//    }
//
//    sealed trait OptionalCsvType extends CsvTypeExtensions {
//        override def isRequired: Boolean = false
//    }
//
//    /** Like withName but gives back [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes.CsvTypes]]
//      * rather than [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes#Value]]
//      * @param s a string representation of the type.
//      * @return
//      */
//    def withNameExtended(s: String): CsvTypes.Value with CsvTypeExtensions = withName(s).asInstanceOf[Value with CsvTypeExtensions]
//}
