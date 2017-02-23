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
  def castCode(fd: FieldDesc): String = {
    def fieldTypeString(fd: FieldDesc): String = {
      val typeStr = fd match {
        case f: RecordField => RefInfoOps.toString(f.refInfo)
        case f: StringField => "org.apache.avro.util.Utf8"  // Special, b/c it's mutable (unlike String)!!!
        case f: IntField => "Int"
        case f: LongField => "Long"
        case f: FloatField => "Float"
        case f: DoubleField => "Double"
        case f: BooleanField => "Boolean"
        case f: ListField => "java.util.List[" + fieldTypeString(f.elementType) + "]" // OK, Avro implements List.
        case f: EnumField => throw new IllegalStateException("enum not supported")
      }

      if (fd.nullable)
        s"Option[$typeStr]"
      else typeStr
    }

    s".asInstanceOf[${fieldTypeString(fd)}]"
  }

  def isStringField(fa: FieldAccessor) = fa.field.isInstanceOf[StringField]

  def accountForString(code: String, fa: FieldAccessor, map: Boolean = false) =
    if (isStringField(fa))
      if (map)
        s"$code.map(_.toString)"
      else s"$code.toString"
    else code

  implicit object RequiredCodeGenerator extends AccessorCodeGenerator[Required] {
    override def generateGet(r: Required) =
      accountForString(s"""get("${r.field.name}")${castCode(r.field)}""", r)
  }

  implicit object DerefReqCodeGenerator extends AccessorCodeGenerator[DerefReq] {
    override def generateGet(d: DerefReq) =
      accountForString(s"""get("${d.field.name}")${castCode(d.field)}.get(${d.index})""", d)
  }

  implicit object OptionalCodeGenerator extends AccessorCodeGenerator[Optional] {
    override def generateGet(o: Optional) = s"""get("${o.field.name}")"""
  }

  implicit object DerefOptCodeGenerator extends AccessorCodeGenerator[DerefOpt] {
    override def generateGet(d: DerefOpt) = s"""get("${d.field.name}")"""
  }

  implicit object reqCodeGenerator extends AccessorCodeGenerator[Req] {
    override def generateGet(r: Req) = r match {
      case r: Required => RequiredCodeGenerator.generateGet(r)
      case r: DerefReq => DerefReqCodeGenerator.generateGet(r)
    }
  }

  implicit object OptCodeGenerator extends AccessorCodeGenerator[Opt] {
    override def generateGet(o: Opt) = o match {
      case o: Required => RequiredCodeGenerator.generateGet(o)
      case o: DerefReq => DerefReqCodeGenerator.generateGet(o)
      case o: Optional => OptionalCodeGenerator.generateGet(o)
      case o: DerefOpt => DerefOptCodeGenerator.generateGet(o)
    }
  }

  implicit object optionalCodeGen extends ContainerCodeGen[Opt] with UnitCodeGen[Opt] {
    def unit(req: Seq[Req], i: Int, fa: Opt): String = {
      accountForString(s"Option(${reqPrefix(i, req)}.${generateGet(fa)})${castCode(fa.field)}", fa, map = true)
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
