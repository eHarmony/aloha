package com.eharmony.aloha.semantics.compiled.plugin.schemabased

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor._
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen.CodeGenerators
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen.MapType.{FLAT_MAP, MAP, NONE}
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.Schema.FieldRetrievalError
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.{FieldDesc, ListField, RecordField, Schema}
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.tokenization.{Field, Index, SchemaBasedTokenizer, Token}
import com.eharmony.aloha.semantics.compiled.{CompiledSemanticsPlugin, OptionalAccessorCode, RequiredAccessorCode, VariableAccessorCode}

import scala.annotation.tailrec
import scalaz.ValidationNel
import scalaz.syntax.validation.ToValidationV

/**
  * Created by ryan.deak on 2/21/17.
  */
trait SchemaBasedSemanticsPlugin[A] { self: CompiledSemanticsPlugin[A] =>

  private[this]type FieldAccessorPartition = (List[FieldAccessor], Option[Repeated], List[FieldAccessor])
  private[this]type MappingPartition = (List[(Seq[Req], Opt)], Option[(Seq[Req], Opt)], List[Req])

  private[schemabased] val functionParamList = "(_0: " + inputTypeString + ") => "

  def dereferenceAsOptional: Boolean
  protected[this] def schema: Schema
  protected[this] def codeGenerators: CodeGenerators

  def accessorFunctionCode(spec: String): Either[Seq[String], VariableAccessorCode] = {
    val function = for {
      tokens <- SchemaBasedTokenizer.getTokens(spec)
      subfields <- fieldChain(fieldNames(tokens), schema)
      fap <- getFieldAccessorPartition(subfields, tokens)
    } yield generateFunction(fap)

    function.toEither.left.map(l => l.head :: l.tail) // Don't want to rely on scalaz for outward facing APIs.
  }

  private[schemabased] def generateFunction(p: FieldAccessorPartition) = {
    val (before, repeated, after) = p
    val b = determineMappingPartition(before)
    val a = determineMappingPartition(after)
    generateFunctionHelper(b, repeated, a)
  }

  /**
    * Get a list of lines in the function.  This includes function signature but not the name or return type.
    * @param before
    * @param repeated
    * @param after
    * @return
    */
  private[schemabased] def generateFunctionHelper(before: MappingPartition, repeated: Option[Repeated], after: MappingPartition): VariableAccessorCode = {
    val cg = codeGenerators
    import cg.{optionalCodeGen, repeatedCodeGen}

    val (bfm, bm, br) = before
    val (afm, am, ar) = after

    // Generate any flat mapped elements before the appearance of the repeated element.
    val bfms = (1 to bfm.size).zip(bfm) map { case (i, (r, o)) => cg.containerCodeGen(r, o, i, FLAT_MAP) }

    // Generate the 0 or 1 mapped element before the appearance of the repeated element.
    val bms = bm map { case (r, o) => cg.containerCodeGen(r, o, bfms.size + 1, MAP) }

    // Generate the 0 or 1 repeated elements.
    val mapList = Seq(afm, am.toSeq, ar).exists(s => s.nonEmpty)
    val rep = repeated map { r => cg.containerCodeGen(br, r, bfms.size + bms.size + 1, if (mapList) MAP else NONE) }

    // Generate any mapped elements appearing after the repeated element.
    val afmi = afm.indices map { _ + bfms.size + bms.size + rep.size + 1 }
    val afms = afmi.zip(afm) map { case (i, (r, o)) => cg.containerCodeGen(r, o, i, FLAT_MAP) }

    // Generate the 0 or 1 mapped elements appearing after the repeated element.
    val ami = 0 until am.size map { _ + bfms.size + bms.size + rep.size + afm.size + 1 }
    val ams = ami.zip(am) map { case (i, (r, o)) => cg.containerCodeGen(r, o, i, if (ar.nonEmpty) MAP else NONE) }

    // Generate the required elements appearing after the repeated element.
    val ari = bfms.size + bms.size + rep.size + afm.size + ams.size + 1
    val ars = Option(ar.nonEmpty) collect { case true => cg.NoSuffixCodeGen.unit(ar, ari) }

    // Assemble all the generated lines of code.  Determine the appropriate number of right parentheses
    // (number of generated lines minus 1) and add to the last line.  Finally, if the code produces optional
    // data before the repeated element, then it would result in one of the following types:
    // Option[Seq[A]] or Option[Seq[Option[A]]].  Because we want to avoid the outermost Option, we map None
    // to an empty sequence.
    val r = bfms ++ bms ++ rep ++ afms ++ ams ++ ars

    // The second clause accomodates that repeated fields can be nullable.
    val optBeforeList = hasOptionalStuff(before) || repeated.fold(false)(r => r.field.nullable)
    val optional = repeated.isEmpty && hasOptionalStuff(after)
    val lastLineSuffix = rightParenthesize(r) + (if (optBeforeList) ".getOrElse(Nil)" else "")
    val lines = r.dropRight(1) ++ r.lastOption.map(_ + lastLineSuffix)

    // Make sure that we have the implicit function imported for converting a Java List to a Scala Buffer.
    val finalLines =
      if (repeated.nonEmpty)
        Seq(
          functionParamList + "{",
          "  import scala.collection.JavaConversions.asScalaBuffer;"
        ) ++ lines ++ Seq("}")
      else Seq(functionParamList) ++ lines

    if (optional) OptionalAccessorCode(finalLines) else RequiredAccessorCode(finalLines)
  }

  private[schemabased] def hasOptionalStuff(pm: MappingPartition) = Seq(pm._1, pm._2.toSeq).foldLeft(0)(_ + _.size) > 0

  private[schemabased] def rightParenthesize(a: Seq[_]) = Seq.fill(a.size - 1)(")").mkString("")

  private[schemabased] def determineMappingPartition(accessors: List[FieldAccessor]): MappingPartition = {
    // At the conclusion of the fold, each item in chains contains a chain of required variables and one
    // optional variable.  req contains the sequence of required variables following the last chain.  Note that
    // both of these variables are in reverse order and need to be reversed prior to returning.
    val (chains, req) = accessors.foldLeft((List.empty[(Seq[Req], Opt)], List.empty[Req])) {
      case ((chains, req), r: Req) => (chains, r :: req)
      case ((chains, req), o: Opt) => ((req.reverse, o) :: chains, Nil)
      case (p, _) => p
    }

    // chains to be flat mapped, optional chain to mapped, and sequence of required variables to be
    // added to the end of the operation.
    (chains.drop(1).reverse, chains.headOption, req.reverse)
  }



  private[schemabased] def partition(fa: List[FieldAccessor]): FieldAccessorPartition = {
    @tailrec def g(l: List[FieldAccessor],
                   before: List[FieldAccessor],
                   repeated: Option[Repeated],
                   after: List[FieldAccessor]): FieldAccessorPartition = l match {
      case Nil => (before, repeated, after)
      case (h: Repeated) :: t => (t.reverse, Some(h), after)
      case h :: t => g(t, before, repeated, h :: after)
    }
    g(fa, Nil, None, Nil)
  }

  private[schemabased] def err(consumed: List[Token], addlMsg: String = ""): ValidationNel[String, Nothing] = {
    val problem = consumed.reverse.map({ case Field(f) => f; case Index(i) => "[" + i + "]" }).mkString(".").replaceAll("""\.\[""", "[")
    ("Problem found at: '" + problem + "'. " + addlMsg).trim.failNel
  }

  private[schemabased] def dereferencedRepeatedField(field: ListField, index: Int): Dereference =
    if (field.nullable)
      OptDerefOpt(field, index)
    else if (dereferenceAsOptional || field.elementType.nullable)
      ReqDerefOpt(field, index)
    else ReqDerefReq(field, index)

  private[schemabased] def convertLeadingFieldAccessors(fa: List[FieldAccessor]) =
    if (dereferenceAsOptional) fa
    else fa map {
      case d: ReqDerefReq => d.toOpt
      case f => f
    }

  private[schemabased] def directlyAccessedField(fd: FieldDesc, fas: List[FieldAccessor]): List[FieldAccessor] = {
    fd match {
      case lf: ListField => Repeated(lf) :: convertLeadingFieldAccessors(fas)
      case f =>
        val fa =
          if (f.nullable)
            Optional(fd)
          else Required(fd)
        fa :: fas
    }
  }


  private[schemabased] def getFieldAccessorPartition(descriptors: List[FieldDesc], tokens: List[Token]): ValidationNel[String, FieldAccessorPartition] = {
    @tailrec def g(d: List[FieldDesc],
                   remaining: List[Token],
                   consumed: List[Token],
                   numLists: Int,
                   fa: List[FieldAccessor]): ValidationNel[String, FieldAccessorPartition] = d match {
      case Nil => partition(fa).success
      case dh :: dt => remaining match {
        case (f: Field) :: (i: Index) :: t =>
          dh match {
            case dh: ListField =>
              val deref = dereferencedRepeatedField(dh, i.index)
              g(dt, t, i :: f :: consumed, numLists, deref :: fa)
            case _ => err(i :: f :: consumed, "The field is not repeated so it cannot be dereferenced.")
          }
        case (f: Field) :: t =>
          val nl = numLists + (if (dh.isInstanceOf[ListField]) 1 else 0)
          if (nl > 1) err(f :: consumed, "Too many list levels produced. Limit 1.")
          else g(dt, t, f :: consumed, nl, directlyAccessedField(dh, fa))
        case _ => err(remaining.headOption.toList ::: consumed, "This should never happen!")
      }
    }
    g(descriptors, tokens, Nil, 0, Nil)
  }

  def fieldNames(tokens: Seq[Token]): Seq[String] = tokens.collect{ case Field(f) => f }

  def fieldChain(fields: Seq[String], schema: Schema): ValidationNel[String, List[FieldDesc]] = {
    @tailrec def helper(fns: List[String], fds: List[FieldDesc]): ValidationNel[String, List[FieldDesc]] = {
      fns match {
        case Nil => fds.reverse.successNel
        case name :: names => fds match {
          case (r: RecordField) :: fs =>
            r.schema.field(name) match {
              case Right(f) => helper(names, f :: r :: fs)
              case Left(FieldRetrievalError(err)) => err.failNel
            }
          case (lf@ListField(_, _, r: RecordField, _)) :: fs =>
            r.schema.field(name) match {
              case Right(f) => helper(names, f :: lf :: fs)
              case Left(FieldRetrievalError(err)) => err.failNel
            }
          case f => ("Expected a Record or List when encountering: " + f).failNel
        }
      }
    }

    fields.toList match {
      case name :: t =>
        schema.field(name) match {
          case Right(fd) => helper(t, List(fd))
          case Left(FieldRetrievalError(err)) => err.failNel
        }
      case _ => "fields is empty.".failNel
    }
  }
}
