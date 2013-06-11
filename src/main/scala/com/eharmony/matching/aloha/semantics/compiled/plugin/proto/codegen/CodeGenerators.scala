package com.eharmony.matching.aloha.semantics.compiled.plugin.proto.codegen

import com.eharmony.matching.aloha.semantics.compiled.plugin.proto.accessor._
import MapType._


private[proto] object CodeGenerators {
//    private[this] def toCamelCase(s: String) = s.split("_+").map(_.toLowerCase.capitalize) mkString ""
    private[this] def toCamelCase(s: String) = s.split("_+").map(_.capitalize) mkString ""

    implicit object RequiredCodeGenerator extends RequiredAccessorCodeGenerator[Required] {
        override def generateGet(r: Required) = "get" + toCamelCase(r.field.getName)
    }

    implicit object DerefReqCodeGenerator extends RequiredAccessorCodeGenerator[DerefReq] {
        override def generateGet(d: DerefReq) = "get" + toCamelCase(d.field.getName) + "(" + d.index + ")"
    }

    implicit object RepeatedCodeGenerator extends RequiredAccessorCodeGenerator[Repeated] {
        override def generateGet(r: Repeated) = "get" + toCamelCase(r.field.getName) + "List"
    }

    implicit object OptionalCodeGenerator extends OptionalAccessorCodeGenerator[Optional] {
        override def generateGet(o: Optional) = "get" + toCamelCase(o.field.getName)
        override def generateHas(o: Optional) = "has" + toCamelCase(o.field.getName)
    }

    implicit object DerefOptCodeGenerator extends OptionalAccessorCodeGenerator[DerefOpt] {
        override def generateGet(d: DerefOpt) = "get" + toCamelCase(d.field.getName) + "(" + d.index + ")"
        override def generateHas(d: DerefOpt) = "get" + toCamelCase(d.field.getName) + "Count > " + d.index
    }

    implicit object ReqCodeGenerator extends RequiredAccessorCodeGenerator[Req] {
        override def generateGet(r: Req) = r match {
            case r: Repeated => RepeatedCodeGenerator.generateGet(r)
            case r: Required => RequiredCodeGenerator.generateGet(r)
            case r: DerefReq => DerefReqCodeGenerator.generateGet(r)
        }
    }

    implicit object OptCodeGenerator extends OptionalAccessorCodeGenerator[Opt] {
        override def generateGet(o: Opt) = o match {
            case o: Repeated => RepeatedCodeGenerator.generateGet(o)
            case o: Required => RequiredCodeGenerator.generateGet(o)
            case o: DerefReq => DerefReqCodeGenerator.generateGet(o)
            case o: Optional => OptionalCodeGenerator.generateGet(o)
            case o: DerefOpt => DerefOptCodeGenerator.generateGet(o)
        }

        override def generateHas(o: Opt) = o match {
            case o: Optional => OptionalCodeGenerator.generateHas(o)
            case o: DerefOpt => DerefOptCodeGenerator.generateHas(o)
        }
    }

    trait UnitCodeGen[A] {
        def unit(req: Seq[Req], i: Int, fa: A): String
        def indent(i: Int) = Seq.fill(i * 2)(" ").mkString("")
        def arg(i: Int) = "_" + i
        def reqPrefix(i: Int, req: Seq[Req]) =
            if (req.isEmpty) arg(i - 1) else req.map(generateGet(_)).mkString(arg(i - 1) + ".", ".", "")
    }

    object NoSuffixCodeGen extends UnitCodeGen[Any] {
        def unit(req: Seq[Req], i: Int, fa: Any = None): String = indent(i) + reqPrefix(i, req)
    }

    trait ContainerCodeGen[A <: FieldAccessor] { self: UnitCodeGen[A] =>
        def mapOp(unitCode: String, i: Int, mapType: MapType): String = {
            val mapOp = if (mapType != NONE) ("." + mapType.toString +"(" + arg(i) + " => ") else ""
            unitCode + mapOp
        }
    }

    implicit object OptionalCodeGen extends ContainerCodeGen[Opt] with UnitCodeGen[Opt] {
        def unit(req: Seq[Req], i: Int, fa: Opt): String = {
            val prefix = reqPrefix(i, req) + "."
            indent(i) + "(if (" + prefix + generateHas(fa) + ") Option(" + prefix + generateGet(fa) + ") else None)"
        }
    }

    implicit object RepeatedCodeGen extends ContainerCodeGen[Repeated] with UnitCodeGen[Repeated] {
        def unit(req: Seq[Req], i: Int, fa: Repeated): String =
            indent(i) + reqPrefix(i, req) + "." + generateGet(fa) + ".toSeq"
    }

    def containerCodeGen[A <: FieldAccessor](req: Seq[Req], fa: A, i: Int, mapType: MapType)(implicit ccg: ContainerCodeGen[A], ucg: UnitCodeGen[A]) =
        ccg.mapOp(ucg.unit(req, i, fa), i, mapType)

    def generateGet[A <: FieldAccessor](a: A)(implicit g: RequiredAccessorCodeGenerator[A]) = g.generateGet(a)
    def generateHas[A <: FieldAccessor](a: A)(implicit g: OptionalAccessorCodeGenerator[A]) = g.generateHas(a)
}

