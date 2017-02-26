package com.eharmony.aloha.semantics.compiled.plugin.avro

import com.eharmony.aloha.reflect.RefInfoOps
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor._
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen.{AccessorCodeGenerator, CodeGenerators}
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema._


/**
  * '''Note''': We use the named extractors rather than intege-based extractors to extract fields because
  * of the note in the [[https://avro.apache.org/docs/current/spec.html#Schema+Resolution Schema Resolution]]
  * section of the Avro specification which states:
  *
  * "''the ordering of fields may be different: fields are matched by name.''"
  *
  * This is probably slower but should be safer.
  */
private[avro] object AvroCodeGenerators extends CodeGenerators {
  private[this] val faithful = (_: FieldDesc).nullable
  private[this] def forced(nullable: Boolean) = (_: FieldDesc) => nullable

  def faithfulNullableStrategy: Stream[FieldDesc => Boolean] =
    Stream.continually(faithful)

  def optionalThenFaithfulNullableStrategy(i: Int, subsequent: Boolean): Stream[FieldDesc => Boolean] = {
    val f = forced(subsequent) // variable b/c of call by name
    forced(true) #:: Stream.fill(i)(faithful) ++ Stream.continually(f)
  }

  def fieldTypeString(fd: FieldDesc, nullableStrategy: Seq[FieldDesc => Boolean]): String = {
    val typeStr = fd match {
      case f: RecordField => RefInfoOps.toString(f.refInfo)
      case f: StringField => "org.apache.avro.util.Utf8"  // Special, b/c it's mutable (unlike String)!!!
      case f: IntField => "Int"
      case f: LongField => "Long"
      case f: FloatField => "Float"
      case f: DoubleField => "Double"
      case f: BooleanField => "Boolean"
      case f: ListField => "java.util.List[" + fieldTypeString(f.elementType, nullableStrategy.tail) + "]" // OK, Avro implements List.
      case f: EnumField => throw new IllegalStateException("enum not supported")
    }

    if (nullableStrategy.head(fd))
      s"Option[$typeStr]"
    else typeStr
  }

  def castCode(fd: FieldDesc, nullableStrategy: Seq[FieldDesc => Boolean] = faithfulNullableStrategy): String =
    s".asInstanceOf[${fieldTypeString(fd, nullableStrategy)}]"

  def isStringField(f: FieldDesc): Boolean = f.isInstanceOf[StringField]

  def accountForString(code: String, f: FieldDesc, map: Boolean = false): String =
    if (isStringField(f))
      if (map)
        s"$code.map(_.toString)"
      else s"$code.toString"
    else code

  implicit object reqCodeGenerator extends AccessorCodeGenerator[Req] {
    override def generateGet(r: Req): String = r match {
      case Required(f) =>
        accountForString(s"""get("${f.name}")${castCode(f)}""", f)
      case ReqDerefReq(f, i) =>
        accountForString(s"""get("${f.name}")${castCode(f)}.get($i)""", f)
    }
  }

  implicit object optionalCodeGen extends ContainerCodeGen[Opt] with UnitCodeGen[Opt] {
    def unit(req: Seq[Req], i: Int, fa: Opt): String = {
      fa match {
        case Optional(f) =>
          accountForString(s"""${indent(i)}Option(${reqPrefix(i, req)}.get("${f.name}"))${castCode(fa.field)}""", f, map = true)
        case ReqDerefOpt(f, j) =>
          gen(req, i, f, j)
        case OptDerefOpt(f, j) =>
          gen(req, i, f, j)
      }
    }

    def gen(req: Seq[Req], i: Int, f: ListField, j: Int): String = {
      val ns = optionalThenFaithfulNullableStrategy(0, subsequent = false)
      val v = accountForString(s"Option(l.get($j)", f.elementType) + ")"
      s"""${indent(i)}Option(${reqPrefix(i, req)}.get("${f.name}"))${castCode(f, ns)}.flatMap{ case l if l.size > $j => $v; case _ => None }"""
    }
  }

  implicit object repeatedCodeGen extends ContainerCodeGen[Repeated] with UnitCodeGen[Repeated] {
    def unit(req: Seq[Req], i: Int, fa: Repeated): String = {
      // Avro's array fields can be nullable and this requires additional work.
      // Additionally, since Avro string data doesn't use java.lang.String but avro.util.Utf8,
      // we do additional work to map to Strings.
      if (fa.field.nullable) {
        val c = s"""${indent(i)}Option(${reqPrefix(i, req)}.get("${fa.field.name}"))${castCode(fa.field)}.map(_.toSeq"""
        if (fa.field.elementType.isInstanceOf[StringField])
          s"$c.map(_.toString))"
        else s"$c)"
      }
      else {
        val c = s"""${indent(i)}${reqPrefix(i, req)}.get("${fa.field.name}")${castCode(fa.field)}.toSeq"""
        if (fa.field.elementType.isInstanceOf[StringField])
          s"$c.map(_.toString)"
        c
      }
    }
  }
}
