package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import scala.language.dynamics

/** This class is designed to emulate Java enum values created by Google's Protocol Buffer library.
  * {{{
  * val GenderProto = Enum("com.eharmony.matching.common.value.ProfileProtoBuffs.ProfileProto.GenderProto", "MALE" -> 1, "FEMALE" -> 2)
  * assert(1 == GenderProto.valueOf("MALE").getNumber)
  * assert(0 == GenderProto.valueOf("MALE").ordinal)
  *
  * try { GenderProto.valueOf("HERMAPHRODITE");  throw new Exception("fail") }
  * catch { case e: IllegalArgumentException => assert(e.getMessage == s"No enum const class ${GenderProto.className}.HERMAPHRODITE") }
  *
  * }}}
  * @param className The canonical class name of the enum being emulated
  * @param constantsAndNumbers the enum constant names and their number (returned by get number).
  */
case class Enum(className: String, private val constantsAndNumbers: (String, Int)*) extends Dynamic {
    private[this] val _values: Array[EnumConstant] = constantsAndNumbers.zipWithIndex.map{ case((s, n), o) => EnumConstantImpl(s, o, n) }.toArray

    private[this] val valueMap: Map[String, EnumConstant] = _values.map(v => v.name -> v).toMap

    /** Emulates the static values() function of a java enum.
      * @return an array of enum constants.
      */
    def values() = _values.clone()

    /** Eumlates the static valueOf(..) function of a java enum. Get the enum constant for the provided enum
      * constant name.
      * @param enumConstant the name of an enum constant to retrieve
      * @return an enum constant value.
      */
    @throws[IllegalArgumentException](cause = "When no enum constant with the provided name exists.")
    def valueOf(enumConstant: String) = valueMap.getOrElse(enumConstant, throw new IllegalArgumentException(s"No enum const class $className.$enumConstant"))

    /** This function is the dynamic function allowing the caller to get an enum constant.  Unlike with an actual enum,
      * trying to access a non-existent enum constant cannot throw create a compile-time error.  It will however
      * generate the same IllegalArgumentException that valueOf does when the enum constant cannot be found.
      *
      * '''NOTE''': ''This function, while exposed publicly, is not intended to be called directly.''
      *
      * {{{
      * val MyEnum = Enum("test.MyEnum", "V1" -> 1, "V2" -> 2)
      * val enumConstant = MyEnum.V1                          // GOOD!
      * val sameConstant = MyEnum.selectDynamic("V1")         // BAD!  Instead use MyEnum.valueOf("V1")
      * }}}
      * @param enumConstant the enum constant.
      * @return
      */
    def selectDynamic(enumConstant: String) = valueOf(enumConstant)
}

object Enum {

    /** A convenience method to produce an Enum instance.
      * @param className the canonical class name of the emulated enum.
      * @param enumConstantNames The names of the enum constants ('''note''': order matters because ordinal values
      *                          are determined by the order).
      * @return an Enum.
      */
    def withNoNumbers(className: String, enumConstantNames: String*) = Enum(className, enumConstantNames.zipWithIndex:_*)
}

/** A representation of an emulated enum constant.
  */
sealed trait EnumConstant {
    def name(): String
    def getNumber(): Int
    def ordinal(): Int
}

/** An emulated enum constant.
  * @param name Name of the enum constant.
  * @param ordinal the ordinal value of the enum constant (this is the order in which the constant appears in the list
  *                of constants)
  * @param getNumber a number (added to allow emulated of protocol buffer generated enums)
  */
case class EnumConstantImpl(name: String, ordinal: Int, getNumber: Int) extends EnumConstant {
    override def toString = name
}
