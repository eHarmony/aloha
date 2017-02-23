package com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor.{FieldAccessor, Opt, Repeated, Req}
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen.MapType.{MapType, NONE}

trait CodeGenerators {

  trait UnitCodeGen[A] {
    def unit(req: Seq[Req], i: Int, fa: A): String
    def indent(i: Int): String = Seq.fill(i * 2)(" ").mkString("")
    def arg(i: Int): String = "_" + i
    def reqPrefix(i: Int, req: Seq[Req]): String =
      if (req.isEmpty) arg(i - 1) else req.map(r => generateGet(r)).mkString(arg(i - 1) + ".", ".", "")
  }

  object NoSuffixCodeGen extends UnitCodeGen[Any] {
    def unit(req: Seq[Req], i: Int, fa: Any = None): String = indent(i) + reqPrefix(i, req)
  }

  trait ContainerCodeGen[A <: FieldAccessor] { self: UnitCodeGen[A] =>
    def mapOp(unitCode: String, i: Int, mapType: MapType): String = {
      val mapOp =
        if (mapType != NONE)
          "." + mapType.toString + "(" + arg(i) + " => "
        else ""
      unitCode + mapOp
    }
  }

  def containerCodeGen[A <: FieldAccessor](req: Seq[Req], fa: A, i: Int, mapType: MapType)(implicit ccg: ContainerCodeGen[A], ucg: UnitCodeGen[A]): String =
    ccg.mapOp(ucg.unit(req, i, fa), i, mapType)

  protected[this] def generateGet[A <: FieldAccessor](a: A)(implicit g: AccessorCodeGenerator[A]): String = g.generateGet(a)

  protected[aloha] implicit def reqCodeGenerator: AccessorCodeGenerator[Req]

  protected[aloha] implicit def optionalCodeGen: ContainerCodeGen[Opt] with UnitCodeGen[Opt]

  protected[aloha] implicit def repeatedCodeGen: ContainerCodeGen[Repeated] with UnitCodeGen[Repeated]
}
