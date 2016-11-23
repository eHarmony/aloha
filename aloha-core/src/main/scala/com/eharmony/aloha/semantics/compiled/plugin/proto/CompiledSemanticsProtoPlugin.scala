package com.eharmony.aloha.semantics.compiled.plugin.proto

import com.eharmony.aloha.semantics.compiled.plugin.MorphableCompiledSemanticsPlugin

import scalaz.ValidationNel
import scalaz.syntax.validation.ToValidationV // scalaz.syntax.validation.ToValidationOps for latest scalaz

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.Descriptors.{ FieldDescriptor, Descriptor }

import com.eharmony.aloha.semantics.compiled.{ OptionalAccessorCode, RequiredAccessorCode, VariableAccessorCode, CompiledSemanticsPlugin }
import com.eharmony.aloha.semantics.compiled.plugin.proto.accessor._
import com.eharmony.aloha.semantics.compiled.plugin.proto.codegen.CodeGenerators
import com.eharmony.aloha.semantics.compiled.plugin.proto.codegen.MapType._
import com.eharmony.aloha.reflect.{ RefInfo, RefInfoOps }
import com.eharmony.aloha.util.EitherHelpers

/**
 *
 * {{{
 * // Idiomatic scala construction.  Implicitly inject manifest.
 * val scala = ProtoSemantics[UserProto]
 *
 * // idiomatic java construction.  Construct manifest from Class object.
 * Semantics<UserProto> java = new ProtoSemantics<UserProto>(UserProto.class);
 *
 * // Java construction via prototype. Construct manifest from Class object extracted from prototype instance.
 * Semantics<UserProto> via = new ProtoSemantics<UserProto>(UserProto.getDefaultInstance());
 * }}}
 *
 * Note, there are a bunch of equivalent ways to construct the semantics.  See below:
 *
 * {{{
 * scala> val lst = Seq(
 *   |   ProtoSemantics[UserProto],
 *   |   new ProtoSemantics[UserProto],
 *   |   ProtoSemantics[UserProto](true),
 *   |   new ProtoSemantics[UserProto](true),
 *   |   new ProtoSemantics[UserProto](classOf[UserProto], true),
 *   |   new ProtoSemantics(classOf[UserProto], true),
 *   |   new ProtoSemantics(UserProto.getDefaultInstance, true)
 *   | )
 * lst: Seq[ProtoSemantics[com.eharmony.matching.common.value.UserProtoBuffs.UserProto]] =
 *         List(ProtoSemantics(true), ProtoSemantics(true), ProtoSemantics(true), ProtoSemantics(true),
 *              ProtoSemantics(true), ProtoSemantics(true), ProtoSemantics(true))
 *
 * scala> lst sameElements Seq.fill(lst.size)(lst.head)
 * res132: Boolean = true
 * }}}
 * @param dereferenceAsOptional Whether to treat dereferenced list variables as an Option.  If '''true''' treat the
 *                              return value of a dereference operation as an Option.  This removes the possibility
 *                              of a [[http://docs.oracle.com/javase/7/docs/api/java/lang/IndexOutOfBoundsException.html java.lang.IndexOutOfBoundsException]]
 *                              being thrown.  Instead, it will silently return None.  If '''false''', treat the
 *                              returned value as a required field and don't do any index checking.  The default
 *                              value is '''true'''.
 * @param refInfoA
 * @tparam A a type of generated protocol buffer message.
 */
case class CompiledSemanticsProtoPlugin[A <: GeneratedMessage](dereferenceAsOptional: Boolean = true)(implicit val refInfoA: RefInfo[A])
  extends CompiledSemanticsPlugin[A]
     with MorphableCompiledSemanticsPlugin
     with EitherHelpers {

  /**
   * This type represents the separation of field accessors into 3 values:
   * 1 the sequence of accessors before a repeated field accessor
   * 1 the repeated (list) field accessor
   * 1 the sequence of accessors after a repeated field accessor
   * If no repeated accessor is present, then the sequence before is empty.
   */
  private[this]type FieldAccessorPartition = (List[FieldAccessor], Option[Repeated], List[FieldAccessor])

  /**
   * This is a further breakdown of the List[FieldAccessor] values in FieldAccessorPartition.
   * The first field is for flat maps, the second for maps and the last for required fields.
   */
  private[this]type MappingPartition = (List[(Seq[Req], Opt)], Option[(Seq[Req], Opt)], List[Req])

  /**
   * Construct the semantics, given the message's Class object.
   * @param classValue A Class object of the protocol buffer generated message.  For instance, for Java
   * {{{
   * final Semantics<UserProto> semantics =
   *   new ProtoSemantics<UserProto>(UserProto.class, false);
   * }}}
   * @param dereferenceAsOptional Whether to treat dereferenced list variables as an Option.
   */
  def this(classValue: Class[A], dereferenceAsOptional: Boolean) =
    this(dereferenceAsOptional)(RefInfoOps fromSimpleClass classValue)

  /**
   * Construct the semantics, given a prototypical instance of the message type.
   * @param prototype a prototype instance of the type of message to create.  For instance, from Java:
   * {{{
   * final Semantics<UserProto> semantics =
   *   new ProtoSemantics<UserProto>(UserProto.getDefaultInstance(), false);
   * }}}
   * @param dereferenceAsOptional Whether to treat dereferenced list variables as an Option.
   */
  def this(prototype: A, dereferenceAsOptional: Boolean) = this(dereferenceAsOptional)({
    require(prototype != null, "prototype value cannot be null.")
    RefInfoOps fromSimpleClass prototype.getClass
  })

  /**
   * Construct the semantics, given the message's Class object.
   * @param classValue A Class object of the protocol buffer generated message.  For instance, for Java
   * {{{
   * final Semantics<UserProto> semantics =
   *   new ProtoSemantics<UserProto>(UserProto.class);
   * }}}
   */
  def this(classValue: Class[A]) = this()(RefInfoOps fromSimpleClass classValue)

  /**
   * Construct the semantics, given a prototypical instance of the message type.
   * @param prototype a prototype instance of the type of message to create.  For instance, from Java:
   * {{{
   * final Semantics<UserProto> semantics =
   *   new ProtoSemantics<UserProto>(UserProto.getDefaultInstance());
   * }}}
   */
  def this(prototype: A) = this()({
    require(prototype != null, "prototype value cannot be null.")
    RefInfoOps fromSimpleClass prototype.getClass
  })

  require(refInfoA != null, "Implicit RefInfo refInfoA cannot be null.")

  /**
   * The descriptor associated with type A.  This class needs to be serialized to work with Spark.  Because
   * of this descriptor must be serializable, however it is not in protobuf (outside of our code).  This will
   * say "memoize this variable when possible, but if it's not possible don't worry about it."
   */
  @transient private lazy val descriptor: ValidationNel[String, Descriptor] =
    toValidationNel(RefInfoOps.execStaticNoArgFunc[A]("getDescriptor")).map(_.asInstanceOf[Descriptor])

  //def descriptor() = toValidationNel(RefInfoOps.execStaticNoArgFunc[A]("getDescriptor")).map(_.asInstanceOf[Descriptor])
  /**
   * The string representation of the function arguments.
   */
  private[proto] val functionParamList = "(_0: " + inputTypeString + ") => "

  /**
   * Generate the function body given a spec.
   * @param spec a String specification of the feature for which function code should be generated
   * @return
   */
  def accessorFunctionCode(spec: String): Either[Seq[String], VariableAccessorCode] = {
    import DescriptorPimpz.PimpedDescriptor
    val function = for {
      d <- descriptor
      tokens <- ProtobufTokenizer.getTokens(spec)
      subfields <- d.subfields(createSpec(tokens))
      fap <- getFieldAccessorPartition(subfields, tokens)
    } yield generateFunction(fap)

    function.toEither.left.map(l => l.head :: l.tail) // Don't want to rely on scalaz for outward facing APIs.
  }

  /**
   * Create a spec for use in extraction of Protocol Buffer Descriptor objects.  This is just the subsequence
   * of Field tokens converted into their name in the Protocol Buffer descriptor.
   * @param tokens a list of tokens
   * @return
   */
  private[proto] def createSpec(tokens: Seq[Token]) = tokens collect { case Field(f) => f } mkString "."

  /**
   * Convert, when appropriate, the leading dereferenced repeated fields preceding a non-dereferenced repeated field.
   * If we don't want to treat dereferenced fields as optional, then we aren't already treating the field accessors
   * before the repeated field as optional.  Since we always want to an non-dereferenced repeated field to produce
   * a list, we don't want to err due to an out-of-bounds exception.  So we turn leading dereferenced repeated
   * fields to optional, even if ''dereferenceAsOptional'' is false.
   *
   * @param fa a list of field accessors that precede a non-dereferenced repeated field.
   * @return
   */
  private[proto] def convertLeadingFieldAccessors(fa: List[FieldAccessor]) =
    if (dereferenceAsOptional) fa
    else fa map {
      case d: DerefReq => d.toOpt
      case f => f
    }

  /**
   * Produce a field accessor that is used to dereference a repeated field.
   * @param field a protocol buffer field descriptor representing a repeated field.
   * @param index an index into the repeated field's list.
   * @return
   */
  private[proto] def dereferencedRepeatedField(field: FieldDescriptor, index: Int) =
    if (dereferenceAsOptional) DerefOpt(field, index) else DerefReq(field, index)

  /**
   * Add fd to fa after optionally transforming fa.
   * @param fd a field descriptor
   * @param fa a list of field accessors
   * @return
   */
  private[proto] def directlyAccessedField(fd: FieldDescriptor, fa: List[FieldAccessor]) =
    if (fd.isRequired) Required(fd) :: fa
    else if (fd.isOptional) Optional(fd) :: fa
    else Repeated(fd) :: convertLeadingFieldAccessors(fa)

  /**
   * Produce a FieldAccessorPartition.  This is:
   * 1 the list of fields preceding a non-dereferenced repeated field
   * 1 an optional non-dereferenced repeated field
   * 1 the list of fields following a non-dereferenced repeated field
   * For more information, see the documentation for the type declaration.
   * @param fa list of field accessors in reverse order.
   */
  private[proto] def partition(fa: List[FieldAccessor]) = {
    def g(l: List[FieldAccessor], before: List[FieldAccessor], repeated: Option[Repeated], after: List[FieldAccessor]): FieldAccessorPartition = l match {
      case Nil => (before, repeated, after)
      case (h: Repeated) :: t => (t.reverse, Some(h), after)
      case h :: t => g(t, before, repeated, h :: after)
    }
    g(fa, Nil, None, Nil)
  }

  /**
   * Get the mapping partition.  This includes the chains of variables that need to be:
   * 1. flat mapped
   * 1. mapped
   * 1. added at the end of the statement
   * These correspond to the fields in the returned tuple.
   * @param accessors A list of field accessors
   */
  private[proto] def determineMappingPartition(accessors: List[FieldAccessor]): MappingPartition = {
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

  /**
   * Generate a function
   *
   * @param p
   * @return
   */
  private[proto] def generateFunction(p: FieldAccessorPartition) = {
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
  private[proto] def generateFunctionHelper(before: MappingPartition, repeated: Option[Repeated], after: MappingPartition) = {
    val (bfm, bm, br) = before
    val (afm, am, ar) = after

    // Generate any flat mapped elements before the appearance of the repeated element.
    val bfms = (1 to bfm.size).zip(bfm) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, FLAT_MAP) }

    // Generate the 0 or 1 mapped element before the appearance of the repeated element.
    val bms = bm map { case (r, o) => CodeGenerators.containerCodeGen(r, o, bfms.size + 1, MAP) }

    // Generate the 0 or 1 repeated elements.
    val mapList = Seq(afm, am.toSeq, ar).foldLeft(false)(_ || _.size > 0)
    val rep = repeated map { case r => CodeGenerators.containerCodeGen(br, r, bfms.size + bms.size + 1, if (mapList) MAP else NONE) }

    // Generate any mapped elements appearing after the repeated element.
    val afmi = Seq.range(0, afm.size) map { _ + bfms.size + bms.size + rep.size + 1 }
    val afms = afmi.zip(afm) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, FLAT_MAP) }

    // Generate the 0 or 1 mapped elements appearing after the repeated element.
    val ami = Seq.range(0, am.size) map { _ + bfms.size + bms.size + rep.size + afm.size + 1 }
    val ams = ami.zip(am) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, if (ar.nonEmpty) MAP else NONE) }

    // Generate the required elements appearing after the repeated element.
    val ari = bfms.size + bms.size + rep.size + afm.size + ams.size + 1
    val ars = Option(ar.nonEmpty).filter(identity).map(_ => CodeGenerators.NoSuffixCodeGen.unit(ar, ari))

    // Assemble all the generated lines of code.  Determine the appropriate number of right parentheses
    // (number of generated lines minus 1) and add to the last line.  Finally, if the code produces optional
    // data before the repeated element, then it would result in one of the following types:
    // Option[Seq[A]] or Option[Seq[Option[A]]].  Because we want to avoid the outermost Option, we map None
    // to an empty sequence.
    val r = bfms ++ bms ++ rep ++ afms ++ ams ++ ars
    val optBeforeList = hasOptionalStuff(before)
    val optional = repeated.isEmpty && hasOptionalStuff(after)
    val lastLineSuffix = rightParenthesize(r) + (if (optBeforeList) ".getOrElse(Nil)" else "")
    val lines = r.dropRight(1) ++ r.lastOption.map(_ + lastLineSuffix)

    // Make sure that we have the implicit function imported for converting a Java List to a Scala Buffer.
    val finalLines =
      if (repeated.nonEmpty) Seq(functionParamList + "{", "  import scala.collection.JavaConversions.asScalaBuffer;") ++ lines ++ Seq("}")
      else Seq(functionParamList) ++ lines

    if (optional) OptionalAccessorCode(finalLines) else RequiredAccessorCode(finalLines)
  }

  /**
   * Given a mapping partition, determine if it will generate optional (Option) data types.
   * @param pm
   * @return
   */
  private[proto] def hasOptionalStuff(pm: MappingPartition) = Seq(pm._1, pm._2.toSeq).foldLeft(0)(_ + _.size) > 0

  /**
   * Generate the appropriate number of right parentheses for the function body.  This is equal to the number of
   * lines minus 1
   *
   * @param a
   * @return
   */
  private[proto] def rightParenthesize(a: Seq[_]) = Seq.fill(a.size - 1)(")").mkString("")

  /**
   * Find errors in the specification of the feature.
   * @param descriptors the sequence of protocol buffer field descriptors that represents the subsequence of
   *                    all Field tokens in the tokens variable.
   * @param tokens a list of tokens
   * @return
   */
  private[proto] def getFieldAccessorPartition(descriptors: List[FieldDescriptor], tokens: List[Token]) = {
    def g(d: List[FieldDescriptor], remaining: List[Token], consumed: List[Token], numLists: Int, fa: List[FieldAccessor]): ValidationNel[String, FieldAccessorPartition] = d match {
      case Nil => partition(fa).success
      case dh :: dt => remaining match {
        case (f: Field) :: (i: Index) :: t =>
          if (!dh.isRepeated) err(i :: f :: consumed, "The field is not repeated so it cannot be dereferenced.")
          else g(dt, t, i :: f :: consumed, numLists, dereferencedRepeatedField(dh, i.index) :: fa)
        case (f: Field) :: t =>
          val nl = numLists + (if (dh.isRepeated) 1 else 0)
          if (nl > 1) err(f :: consumed, "Too many list levels produced. Limit 1.")
          else g(dt, t, f :: consumed, nl, directlyAccessedField(dh, fa))
        case _ => err(remaining.headOption.toList ::: consumed, "This should never happen!")
      }
    }
    g(descriptors, tokens, Nil, 0, Nil)
  }

  /**
   * Produce an error message for use in the getFieldAccessorPartition function.
   * @param consumed the tokens already consumed
   * @param addlMsg any additional message to add to the error that will be returned.
   * @return
   */
  private[proto] def err(consumed: List[Token], addlMsg: String = ""): ValidationNel[String, Nothing] = {
    val problem = consumed.reverse.map({ case Field(f) => f; case Index(i) => "[" + i + "]" }).mkString(".").replaceAll("""\.\[""", "[")
    ("Problem found at: '" + problem + "'. " + addlMsg).trim.failNel
  }

  override def morph[B](implicit ri: RefInfo[B]): Option[CompiledSemanticsPlugin[B]] = {
    Option(this) collect {
      case CompiledSemanticsProtoPlugin(deref) if RefInfoOps.isSubType(ri, RefInfo[GeneratedMessage]) =>
        // TODO: Attempt to remove these horrible casts.
        // It's known by the IF condition above that this is true.
        // Can implicit evidence somehow be provided instead?
        val castedRefInfo = ri.asInstanceOf[RefInfo[GeneratedMessage]]
        // TODO: Remove commented code after getting SBT build working.
//        CompiledSemanticsProtoPlugin(deref)(castedRefInfo).asInstanceOf[CompiledSemanticsProtoPlugin[B]]
        CompiledSemanticsProtoPlugin(deref)(castedRefInfo).asInstanceOf[CompiledSemanticsPlugin[B]]
    }
  }
}

object CompiledSemanticsProtoPlugin {
  def apply[A <: GeneratedMessage: RefInfo]: CompiledSemanticsProtoPlugin[A] = new CompiledSemanticsProtoPlugin
  object Implicits {
    implicit def protoSemantics[A <: GeneratedMessage: RefInfo]: CompiledSemanticsProtoPlugin[A] = apply[A]
  }
}

//case class ProtoSemantics[A <: GeneratedMessage](dereferenceAsOptional: Boolean = true)(implicit m: Manifest[A]) extends Semantics[A] {
//    /** This type represents the separation of field accessors into 3 values:
//      1 the sequence of accessors before a repeated field accessor
//      1 the repeated (list) field accessor
//      1 the sequence of accessors after a repeated field accessor
//      * If no repeated accessor is present, then the sequence before is empty.
//      */
//    private[this] type FieldAccessorPartition = (List[FieldAccessor], Option[Repeated], List[FieldAccessor])
//
//    /** This is a further breakdown of the List[FieldAccessor] values in FieldAccessorPartition.
//      * The first field is for flat maps, the second for maps and the last for required fields.
//      */
//    private[this] type MappingPartition = (List[(Seq[Req], Opt)], Option[(Seq[Req], Opt)], List[Req])
//
//    /** Construct the semantics, given the message's Class object.
//      * @param classValue A Class object of the protocol buffer generated message.  For instance, for Java
//      * {{{
//      * final Semantics<UserProto> semantics =
//      *   new ProtoSemantics<UserProto>(UserProto.class, false);
//      * }}}
//      * @param dereferenceAsOptional Whether to treat dereferenced list variables as an Option.
//      */
//    def this(classValue: Class[A], dereferenceAsOptional: Boolean) = this(dereferenceAsOptional)({
//        require(classValue != null, "Class object 'classValue' cannot be null.")
//        Manifest.classType[A](classValue)
//    })
//
//    /** Construct the semantics, given a prototypical instance of the message type.
//      * @param prototype a prototype instance of the type of message to create.  For instance, from Java:
//      * {{{
//      * final Semantics<UserProto> semantics =
//      *   new ProtoSemantics<UserProto>(UserProto.getDefaultInstance(), false);
//      * }}}
//      * @param dereferenceAsOptional Whether to treat dereferenced list variables as an Option.
//      */
//    def this(prototype: A, dereferenceAsOptional: Boolean) = this(dereferenceAsOptional)({
//        require(prototype != null, "prototype value cannot be null.")
//        Manifest.classType[A](prototype.getClass)
//    })
//
//    /** Construct the semantics, given the message's Class object.
//      * @param classValue A Class object of the protocol buffer generated message.  For instance, for Java
//      * {{{
//      * final Semantics<UserProto> semantics =
//      *   new ProtoSemantics<UserProto>(UserProto.class);
//      * }}}
//      */
//    def this(classValue: Class[A]) = this()({
//        require(classValue != null, "Class object 'classValue' cannot be null.")
//        Manifest.classType[A](classValue)
//    })
//
//    /** Construct the semantics, given a prototypical instance of the message type.
//      * @param prototype a prototype instance of the type of message to create.  For instance, from Java:
//      * {{{
//      * final Semantics<UserProto> semantics =
//      *   new ProtoSemantics<UserProto>(UserProto.getDefaultInstance());
//      * }}}
//      */
//    def this(prototype: A) = this()({
//        require(prototype != null, "prototype value cannot be null.")
//        Manifest.classType[A](prototype.getClass)
//    })
//
//    require(m != null, "Implicit manifest m cannot be null.")
//
//    /** The descriptor associated with type A.
//      */
//    private val descriptor = try {
//        val methods = m.erasure.getMethods
//        val filtered = methods.filter(m => m.getName == "getDescriptor" && Modifier.isStatic(m.getModifiers) && 0 == m.getParameterTypes.size)
//        val head = filtered.headOption.map(_.invoke(null).asInstanceOf[Descriptor]).toRight("Couldn't get descriptor for  " + inputTypeString)
//        head
//    }
//    catch {
//        case e: Exception => Left("Couldn't get descriptor for " + inputTypeString + ": " + e.getMessage)
//    }
//
//    /** The string representation of the function arguments.
//      */
//    private[proto] val functionParamList = "(_0: " + inputTypeString + ") => "
//
//    /** The manifest of the input type.
//      * @return
//      */
//    def manifest = m
//
//    /** Generate the function body given a spec.
//      * @param spec a String specification of the feature for which function code should be generated
//      * @return
//      */
//    def accessorFunctionCode(spec: String): Either[List[String], VariableAccessor] = {
//        import DescriptorPimpz.Implicits.descriptor2pimpedDescriptor
//        val function = for {
//            d <- descriptor.right
//            tokens <- ProtobufTokenizer.getTokens(spec).right
//            subfields <- d.subfields(createSpec(tokens)).right
//            fap <- getFieldAccessorPartition(subfields, tokens).right
//        } yield generateFunction(fap)
//
//        // Convert single error message to a list.
//        function.fold(msg => Left(List(msg)), Right(_))
//    }
//
//    /** Create a spec for use in extraction of Protocol Buffer Descriptor objects.  This is just the subsequence
//      * of Field tokens converted into their name in the Protocol Buffer descriptor.
//      * @param tokens a list of tokens
//      * @return
//      */
//    private[proto] def createSpec(tokens: Seq[Token]) = tokens collect {case Field(f) => f} mkString "."
//
//    /** Convert, when appropriate, the leading dereferenced repeated fields preceding a non-dereferenced repeated field.
//      * If we don't want to treat dereferenced fields as optional, then we aren't already treating the field accessors
//      * before the repeated field as optional.  Since we always want to an non-dereferenced repeated field to produce
//      * a list, we don't want to err due to an out-of-bounds exception.  So we turn leading dereferenced repeated
//      * fields to optional, even if ''dereferenceAsOptional'' is false.
//      *
//      * @param fa a list of field accessors that precede a non-dereferenced repeated field.
//      * @return
//      */
//    private[proto] def convertLeadingFieldAccessors(fa: List[FieldAccessor]) =
//        if (dereferenceAsOptional) fa
//        else fa map {
//            case d: DerefReq => d.toOpt
//            case f => f
//        }
//
//    /** Produce a field accessor that is used to dereference a repeated field.
//      * @param field a protocol buffer field descriptor representing a repeated field.
//      * @param index an index into the repeated field's list.
//      * @return
//      */
//    private[proto] def dereferencedRepeatedField(field: FieldDescriptor, index: Int) =
//        if (dereferenceAsOptional) DerefOpt(field, index) else DerefReq(field, index)
//
//    /** Add fd to fa after optionally transforming fa.
//      * @param fd a field descriptor
//      * @param fa a list of field accessors
//      * @return
//      */
//    private[proto] def directlyAccessedField(fd: FieldDescriptor, fa: List[FieldAccessor]) =
//        if (fd.isRequired) Required(fd) :: fa
//        else if (fd.isOptional) Optional(fd) :: fa
//        else Repeated(fd) :: convertLeadingFieldAccessors(fa)
//
//    /** Produce a FieldAccessorPartition.  This is:
//      1 the list of fields preceding a non-dereferenced repeated field
//      1 an optional non-dereferenced repeated field
//      1 the list of fields following a non-dereferenced repeated field
//      * For more information, see the documentation for the type declaration.
//      * @param fa list of field accessors in reverse order.
//      */
//    private[proto] def partition(fa: List[FieldAccessor]) = {
//        def g(l: List[FieldAccessor], before: List[FieldAccessor], repeated: Option[Repeated], after: List[FieldAccessor]): FieldAccessorPartition = l match {
//            case Nil => (before, repeated, after)
//            case (h: Repeated) :: t => (t.reverse, Some(h), after)
//            case h :: t => g(t, before, repeated, h :: after)
//        }
//        g(fa, Nil, None, Nil)
//    }
//
//    /** Get the mapping partition.  This includes the chains of variables that need to be:
//      1. flat mapped
//      1. mapped
//      1. added at the end of the statement
//      * These correspond to the fields in the returned tuple.
//      * @param accessors A list of field accessors
//      */
//    private[proto] def determineMappingPartition(accessors: List[FieldAccessor]): MappingPartition = {
//        // At the conclusion of the fold, each item in chains contains a chain of required variables and one
//        // optional variable.  req contains the sequence of required variables following the last chain.  Note that
//        // both of these variables are in reverse order and need to be reversed prior to returning.
//        val (chains, req) = accessors.foldLeft((List.empty[(Seq[Req], Opt)], List.empty[Req])) {
//            case ((chains, req), r: Req) => (chains, r :: req)
//            case ((chains, req), o: Opt) => ((req.reverse, o) :: chains, Nil)
//            case (p, _) => p
//        }
//
//        // chains to be flat mapped, optional chain to mapped, and sequence of required variables to be
//        // added to the end of the operation.
//        (chains.drop(1).reverse, chains.headOption, req.reverse)
//    }
//
//    /** Generate a function
//      *
//      * @param p
//      * @return
//      */
//    private[proto] def generateFunction(p: FieldAccessorPartition) = {
//        val (before, repeated, after) = p
//        val b = determineMappingPartition(before)
//        val a = determineMappingPartition(after)
//        generateFunctionHelper(b, repeated, a)
//    }
//
//    /** Get a list of lines in the function.  This includes function signature but not the name or return type.
//      * @param before
//      * @param repeated
//      * @param after
//      * @return
//      */
//    private[proto] def generateFunctionHelper(before: MappingPartition, repeated: Option[Repeated], after: MappingPartition) = {
//        val (bfm, bm, br) = before
//        val (afm, am, ar) = after
//
//        // Generate any flat mapped elements before the appearance of the repeated element.
//        val bfms = (1 to bfm.size).zip(bfm) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, FLAT_MAP) }
//
//        // Generate the 0 or 1 mapped element before the appearance of the repeated element.
//        val bms = bm map { case (r, o) => CodeGenerators.containerCodeGen(r, o, bfms.size + 1, MAP) }
//
//        // Generate the 0 or 1 repeated elements.
//        val mapList = Seq(afm, am.toSeq, ar).foldLeft(false)(_ || _.size > 0)
//        val rep = repeated map { case r => CodeGenerators.containerCodeGen(br, r, bfms.size + bms.size  + 1, if (mapList) MAP else NONE) }
//
//        // Generate any mapped elements appearing after the repeated element.
//        val afmi = Seq.range(0, afm.size) map {_ + bfms.size + bms.size + rep.size + 1}
//        val afms = afmi.zip(afm) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, FLAT_MAP) }
//
//        // Generate the 0 or 1 mapped elements appearing after the repeated element.
//        val ami = Seq.range(0, am.size) map {_ + bfms.size + bms.size + rep.size + afm.size + 1}
//        val ams = ami.zip(am) map { case (i, (r, o)) => CodeGenerators.containerCodeGen(r, o, i, if (ar.nonEmpty) MAP else NONE) }
//
//        // Generate the required elements appearing after the repeated element.
//        val ari = bfms.size + bms.size + rep.size + afm.size + ams.size + 1
//        val ars = Option(ar.nonEmpty).filter(identity).map(_ => CodeGenerators.NoSuffixCodeGen.unit(ar, ari))
//
//        // Assemble all the generated lines of code.  Determine the appropriate number of right parentheses
//        // (number of generated lines minus 1) and add to the last line.  Finally, if the code produces optional
//        // data before the repeated element, then it would result in one of the following types:
//        // Option[Seq[A]] or Option[Seq[Option[A]]].  Because we want to avoid the outermost Option, we map None
//        // to an empty sequence.
//        val r = bfms ++ bms ++ rep ++ afms ++ ams ++ ars
//        val optBeforeList = hasOptionalStuff(before)
//        val optional = optBeforeList || (repeated.isEmpty && hasOptionalStuff(after))
//        val lastLineSuffix = rightParenthesize(r) + (if (optBeforeList) ".getOrElse(Nil)" else "")
//        val lines = r.dropRight(1) ++ r.lastOption.map(_ + lastLineSuffix)
//
//        // Make sure that we have the implicit function imported for converting a Java List to a Scala Buffer.
//        val finalLines =
//            if (repeated.nonEmpty) Seq(functionParamList + "{", "  import scala.collection.JavaConversions.asScalaBuffer;") ++ lines ++ Seq("}")
//            else Seq(functionParamList) ++ lines
//
//        if (optional) OptionalAccessor(finalLines) else RequiredAccessor(finalLines)
//    }
//
//    /** Given a mapping partition, determine if it will generate optional (Option) data types.
//      * @param pm
//      * @return
//      */
//    private[proto] def hasOptionalStuff(pm: MappingPartition) = Seq(pm._1, pm._2.toSeq).foldLeft(0)(_ + _.size) > 0
//
//    /** Generate the appropriate number of right parentheses for the function body.  This is equal to the number of
//      * lines minus 1
//      *
//      * @param a
//      * @return
//      */
//    private[proto] def rightParenthesize(a: Seq[_]) = Seq.fill(a.size - 1)(")").mkString("")
//
//    /** Find errors in the specification of the feature.
//      * @param descriptors the sequence of protocol buffer field descriptors that represents the subsequence of
//      *                    all Field tokens in the tokens variable.
//      * @param tokens a list of tokens
//      * @return
//      */
//    private[proto] def getFieldAccessorPartition(descriptors: List[FieldDescriptor], tokens: List[Token]) = {
//        def g(d: List[FieldDescriptor], remaining: List[Token], consumed: List[Token], numLists: Int, fa: List[FieldAccessor]): Either[String, FieldAccessorPartition] = d match {
//            case Nil => Right(partition(fa))   // Success
//            case dh :: dt => remaining match {
//                case (f: Field) :: (i: Index) :: t =>
//                    if (!dh.isRepeated) err(i :: f :: consumed, "The field is not repeated so it cannot be dereferenced.")
//                    else g(dt, t, i :: f :: consumed, numLists, dereferencedRepeatedField(dh, i.index) :: fa)
//                case (f: Field) :: t =>
//                    val nl = numLists + (if (dh.isRepeated) 1 else 0)
//                    if (nl > 1) err(f :: consumed, "Too many list levels produced. Limit 1.")
//                    else g(dt, t, f :: consumed, nl, directlyAccessedField(dh, fa))
//                case _ => err(remaining.headOption.toList ::: consumed, "This should never happen!")
//            }
//        }
//        g(descriptors, tokens, Nil, 0, Nil)
//    }
//
//    /** Produce an error message for use in the getFieldAccessorPartition function.
//      * @param consumed the tokens already consumed
//      * @param addlMsg any additional message to add to the error that will be returned.
//      * @return
//      */
//    private[proto] def err(consumed: List[Token], addlMsg: String = "") = {
//        val problem = consumed.reverse.map({case Field(f) => f; case Index(i) => "[" + i + "]"}).mkString(".").replaceAll("""\.\[""", "[")
//        Left(("Problem found at: '" + problem + "'. " + addlMsg).trim)
//    }
//}
//
//object ProtoSemantics {
//
//    /** This is the standard way of creating an instance of ProtoSemantics.  There is also an implicit version in the
//      * [[com.eharmony.matching.model.v3.semantics.Semantics]] module.
//      * {{{
//      * val semantics = ProtoSemantics[UserProto]
//      * }}}
//      * @tparam A
//      * @return
//      */
//    def apply[A <: GeneratedMessage : Manifest]: ProtoSemantics[A] = new ProtoSemantics
//
//    object Implicits {
//
//        /** Default ProtoSemantics.  Treat indexed values in the default way.  For more information, see
//          * [[com.eharmony.matching.model.v3.semantics.proto.ProtoSemantics]].
//          * @tparam A
//          * @return
//          */
//        implicit def protoSemantics[A <: GeneratedMessage : Manifest]: ProtoSemantics[A] = apply[A]
//    }
//}
