package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

/** The string values here match the function names in CsvLine.
  * @author R. M. Deak
  */
object CsvTypes extends Enumeration {
    type CsvTypes = Value with CsvTypeExtensions

    val EnumType    = new Val("e") with RequiredCsvType
    val BooleanType = new Val("b") with RequiredCsvType
    val IntType     = new Val("i") with RequiredCsvType
    val LongType    = new Val("l") with RequiredCsvType
    val FloatType   = new Val("f") with RequiredCsvType
    val DoubleType  = new Val("d") with RequiredCsvType
    val StringType  = new Val("s") with RequiredCsvType

    val EnumOptionType    = new Val("oe") with OptionalCsvType
    val BooleanOptionType = new Val("ob") with OptionalCsvType
    val IntOptionType     = new Val("oi") with OptionalCsvType
    val LongOptionType    = new Val("ol") with OptionalCsvType
    val FloatOptionType   = new Val("of") with OptionalCsvType
    val DoubleOptionType  = new Val("od") with OptionalCsvType
    val StringOptionType  = new Val("os") with OptionalCsvType

    val EnumVectorType    = new Val("ve") with RequiredCsvType
    val BooleanVectorType = new Val("vb") with RequiredCsvType
    val IntVectorType     = new Val("vi") with RequiredCsvType
    val LongVectorType    = new Val("vl") with RequiredCsvType
    val FloatVectorType   = new Val("vf") with RequiredCsvType
    val DoubleVectorType  = new Val("vd") with RequiredCsvType
    val StringVectorType  = new Val("vs") with RequiredCsvType

    val EnumOptionVectorType    = new Val("voe") with RequiredCsvType
    val BooleanOptionVectorType = new Val("vob") with RequiredCsvType
    val IntOptionVectorType     = new Val("voi") with RequiredCsvType
    val LongOptionVectorType    = new Val("vol") with RequiredCsvType
    val FloatOptionVectorType   = new Val("vof") with RequiredCsvType
    val DoubleOptionVectorType  = new Val("vod") with RequiredCsvType
    val StringOptionVectorType  = new Val("vos") with RequiredCsvType

    /** Adds additional information to
      */
    sealed trait CsvTypeExtensions {
        def isRequired: Boolean
    }

    sealed trait RequiredCsvType extends CsvTypeExtensions {
        override def isRequired: Boolean = true
    }

    sealed trait OptionalCsvType extends CsvTypeExtensions {
        override def isRequired: Boolean = false
    }

    /** Like withName but gives back [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes.CsvTypes]]
      * rather than [[com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvTypes#Value]]
      * @param s a string representation of the type.
      * @return
      */
    def withNameExtended(s: String) = withName(s).asInstanceOf[Value with CsvTypeExtensions]
}
