package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.google.protobuf.GeneratedMessage

import scala.util.Try
import scala.concurrent.{ Future, Await, ExecutionContext }
import scala.concurrent.duration._
import com.eharmony.aloha.NoEvictionCache
import com.eharmony.aloha.io.ContainerReadable
import com.eharmony.aloha.semantics.{MorphableSemantics, ErrorEnrichingSemantics, Semantics}
import com.eharmony.aloha.semantics.func.{ GeneratedAccessor, OptionalFunc, GenFunc, GenAggFunc }
import com.eharmony.aloha.util.EitherHelpers
import com.eharmony.aloha.reflect.{ RefInfoOps, RefInfo }
import java.io.{ PrintWriter, StringWriter }
import com.eharmony.aloha.util.Logging

/**
 * A semantics that can interpret complicated expressions by compiling the expressions.  This semantics constructs
 * actual working scala code (see the format below).
 *
 * Notice from the code that is constructed that it calls a function
 * in [[com.eharmony.aloha.semantics.func.GenFunc]].  The function that is called is based on the input arity.
 * Aside from the f0 function, f,,i,, has ''i'' arguments in the first argument list and two arguments in the second
 * argument list.  The first argument list represents the definition of the accessor functions.  These are the
 * functions created to extract data from the domain object.
 * [[com.eharmony.aloha.semantics.compiled.CompiledSemanticsPlugin]] objects are responsible for constructing
 * the function bodies.  For instance, `(_:Map[String, Long]).get("user.match.TaxBrackets")` in the first example.  The
 * CompiledSemantics class is responsible for wrapping this code appropriately, constructing the aggregation function
 * and finally putting everything together and compiling it.
 *
 * = Examples =
 *
 * '''E.g. 1''':  Two variables are both optional (not necessarily in the input type passed to the model):
 * - user.match.TaxBrackets
 * - cand.taxBracket
 *
 * {{{
 * val spec1 = "Seq(${user.match.TaxBrackets}) contains ${cand.taxBracket}"
 * val f1 = s.createFunction[Boolean](spec1, Some(false)).right.get
 *
 * // Equivalent to:
 * //    val f1a =
 * //      com.eharmony.aloha.semantics.compiled.GenFunc.f2(           // f2 b/c 2 variables.
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(  // Optional Accessor:
 * //          "user.match.TaxBrackets",
 * //          (_:Map[String, Long]).get("user.match.TaxBrackets"),             // <-- get produces Option[Long]
 * //          Some("(_:Map[String, Long]).get(\"user.match.TaxBrackets\")")),
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(  // Optional Accessor:
 * //          "cand.taxBracket",
 * //          (_:Map[String, Long]).get("cand.taxBracket"),                    // <-- get produces Option[Long]
 * //          Some("(_:Map[String, Long]).get(\"cand.taxBracket\")"))
 * //      )(
 * //        "Seq(${user.match.TaxBrackets}) contains ${cand.taxBracket}",
 * //        (o0, o1) => for {
 * //          _user__match__TaxBrackets <- o0                                  // Optional fields in for comprehension
 * //          _cand__taxBracket <- o1
 * //        } yield {
 * //          Seq(_user__match__TaxBrackets) contains _cand__taxBracket        // Transformed function spec
 * //        }
 * //      )
 * //
 * //    val f1 = com.eharmony.aloha.semantics.compiled.OptionalFunc(f1a, false)
 * }}}
 *
 * '''E.g. 2''': Two variables (one required, one optional):
 * - user.inboundComm (required)
 * - user.pageViews   (optional)
 *
 * {{{
 * val spec2 = "${user.inboundComm} / ${user.pageViews}.toDouble"
 * val f2 = s.createFunction[Double](spec2, Some(Double.NaN)).right.get
 *
 * // Equivalent to:
 * //    val f2a =
 * //      com.eharmony.aloha.semantics.compiled.GenFunc.f2(           // f2 b/c 2 variables.
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(  // Optional Accessor:
 * //          "user.pageViews",
 * //          (_:Map[String, Long]).get("user.pageViews"),                     // <-- get produces Option[Long]
 * //          Some("(_:Map[String, Long]).get(\"user.pageViews\")")),
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(  // Required Accessor:
 * //          "user.inboundComm",
 * //          (_:Map[String, Long]).apply("user.inboundComm"),                 // <-- apply produces Long
 * //          Some("(_:Map[String, Long]).apply(\"user.inboundComm\")"))
 * //      )(
 * //        "${user.inboundComm} / ${user.pageViews}.toDouble",
 * //        (o0, _user__inboundComm) => for {
 * //          _user__pageViews <- o0                                           // Optional field in for comprehension
 * //        } yield {
 * //          _user__inboundComm / _user__pageViews.toDouble                   // Transformed function spec
 * //        }
 * //      )
 * //
 * //    val f2 = com.eharmony.aloha.semantics.compiled.OptionalFunc(f2a, Double.NaN)
 * }}}
 *
 * '''E.g. 3''': Three required variables (one, two, three):
 *
 * {{{
 * val spec3 = "List(${one}, ${two}, ${three}).sum.toInt"
 * val f3 = s.createFunction[Int](spec3).right.get
 *
 * // Equivalent to:
 * //    val f3 =
 * //      com.eharmony.aloha.semantics.compiled.GenFunc.f3(           // f3 b/c 3 variables.
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(
 * //          "one",
 * //          (_:Map[String, Long]).apply("one"),
 * //          Some("(_:Map[String, Long]).apply(\"one\")")),
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(
 * //          "two",
 * //          (_:Map[String, Long]).apply("two"),
 * //          Some("(_:Map[String, Long]).apply(\"two\")")),
 * //        com.eharmony.aloha.semantics.compiled.GeneratedAccessor(
 * //          "three",
 * //          (_:Map[String, Long]).apply("three"),
 * //          Some("(_:Map[String, Long]).apply(\"three\")"))
 * //      )(
 * //        "List(${one}, ${two}, ${three}).sum.toInt",
 * //        (_one, _two, _three) => {                  // Notice simplified expression when all params are required.
 * //          List(_one, _two, _three).sum.toInt       // Variable names are in the param list so no need for temp
 * //        }                                          // variables.
 * //      )
 * }}}
 *
 * '''E.g. 4''': Invariant function (not dependent on any input variables).
 *
 * {{{
 * val spec4 = "new util.Random(System.nanoTime).nextLong"
 * val f = s.createFunction[Long](spec4).right.get         // f(null) is OK b/c not reliant on input value.
 *
 * // Equivalent to:
 * //    val f4 =
 * //      com.eharmony.aloha.semantics.compiled.GenFunc.f0( // f0 b/c input arity is 0
 * //        "new util.Random(System.nanoTime).nextLong",
 * //        (_:Any) => new util.Random(System.nanoTime).nextLong     // Notice input is Any
 * //      )                                                          // OK b/c of Function1 1st arg contravariance.
 * }}}
 * @param compiler a compiler capable of compiling real scala code and generating real instances that work at full speed.
 * @param plugin a plugin that can make sense of the variable specifications and generate code to extract data from them.
 * @param imports a list of imports.
 * @param provideSemanticsUdfException whether we should provide [[com.eharmony.aloha.semantics.SemanticsUdfException]]
 *                                     instead of raw exceptions in the case that a feature produces an exception.  The
 *                                     perceived benefit of this seems to drastically outweigh the performance hit, so
 *                                     this defaults to true (as this is suggested).
 * @param ec an execution context in which to run the Futures that are used in the cache.
 * @tparam A the input type to the functions that are to be generated.  Said another way, this is domain type of the
 *           models that will be generated by the ModelFactory to which this
 *           [[com.eharmony.aloha.semantics.Semantics]] will be passed.
 */
// TODO: Add an argument of whether to save the strings containing the generated code (this could be the result of "isDebugEnabled" or an explicit constructor param)
// TODO: The reason this is important is that under java 6, it may be the case that it will blow up perm gen

case class CompiledSemantics[A](
  compiler: ContainerReadable[Try],
  plugin: CompiledSemanticsPlugin[A],
  imports: Seq[String],
  override val provideSemanticsUdfException: Boolean = true)(implicit protected val ec: ExecutionContext)
  extends CompiledSemanticsLike[A]
  with MorphableSemantics[CompiledSemantics, A]
  with ErrorEnrichingSemantics[A]
  with Logging {

  /**
   * A java (Spring) friendly constructor.
   * @param compiler a compiler capable of compiling real scala code and generating real instances that work at full speed.
   * @param plugin a plugin that can make sense of the variable specifications and generate code to extract data from them.
   * @param imports an array of imports.  Will be cloned before being stored to avoid caller changing array values.
   * @param provideSemanticsUdfException whether we should provide [[com.eharmony.aloha.semantics.SemanticsUdfException]]
   *                                     instead of raw exceptions in the case that a feature produces an exception.  The
   *                                     perceived benefit of this seems to drastically outweigh the performance hit, so
   *                                     this defaults to true (as this is suggested).
   * @param ec an execution context in which to run the Futures that are used in the cache.
   * @return
   */
  def this(compiler: ContainerReadable[Try],
    plugin: CompiledSemanticsPlugin[A],
    imports: Array[String],
    provideSemanticsUdfException: Boolean,
    ec: ExecutionContext) =
    this(compiler, plugin, imports.clone(), provideSemanticsUdfException)(ec)

  /**
   * A java (Spring) friendly constructor.
   * @param compiler a compiler capable of compiling real scala code and generating real instances that work at full speed.
   * @param plugin a plugin that can make sense of the variable specifications and generate code to extract data from them.
   * @param imports an array of imports.  Will be cloned before being stored to avoid caller changing array values.
   * @param ec an execution context in which to run the Futures that are used in the cache.
   * @return
   */
  def this(compiler: ContainerReadable[Try],
    plugin: CompiledSemanticsPlugin[A],
    imports: Array[String],
    ec: ExecutionContext) =
    this(compiler, plugin, imports.clone())(ec)

  /**
   * Generated code will only be retained in the Generated code when debug logging is enabled.
   * @return
   */
  def retainGeneratedCode(): Boolean = isDebugEnabled

  def semantics: CompiledSemantics[A] = this

  /**
    * Attempt to create a new [[CompiledSemantics]] with a different type parameter.
    * @param ri reflection information that may be necessary to determine whether to create
    *           the [[MorphableSemantics]] that was requested.
    * @tparam B input type for the new [[MorphableSemantics]] instance that might be created.
    *           A [[MorphableSemantics]] instance may choose not allow morphing to all `B`.
    *           In that case, a `None` will be returned.
    * @return
    */
  def morph[B](implicit ri: RefInfo[B]): Option[CompiledSemantics[B]] = {
    protoSemantics[B] // orElse ... orElse ...
  }

  private[compiled] def protoSemantics[B](implicit ri: RefInfo[B]): Option[CompiledSemantics[B]] = {
    // The filter works because only the TYPE / structure of the plugin needs to be confirmed.
    // Since the type parameter of the plugin is the same as the type parameter of semantics,
    // it isn't necessary to check the type parameter.  But this still seems like a suboptimal
    // way to do this check.
    // TODO: Find a better way to do the check in second term of the filter.
    Option(plugin) filter { p =>
      RefInfoOps.isSubType(ri, RefInfo[GeneratedMessage]) &&
      p.isInstanceOf[CompiledSemanticsProtoPlugin[_]]
    } collect { case CompiledSemanticsProtoPlugin(deref) =>
      // TODO: Attempt to remove these horrible casts.
      // It's known by the first clause of the filter that this is true.
      // Can implicit evidence somehow be provided?
      val castedRefInfo = ri.asInstanceOf[RefInfo[GeneratedMessage]]
      val newPlugin = CompiledSemanticsProtoPlugin(deref)(castedRefInfo)

      // TODO: Test whether `compiler` can be reused.
      copy(plugin = newPlugin).asInstanceOf[CompiledSemantics[B]]
    }
  }
}

/**
 * Companion class containing canonical class names for wrapper classes uses in compilation.
 */
object CompiledSemantics {

  /**
   * This won't be necessary once we can return a future rather than waiting on the future before returning.
   */
  private[compiled] val maxGenWaitInSec = 300
  private[compiled] val generatedAccessorClassName = GeneratedAccessor.getClass.getCanonicalName.replaceAll("""\$$""", "")
  private[compiled] val genFuncClassName = GenFunc.getClass.getCanonicalName.replaceAll("""\$$""", "")

  private[compiled] sealed trait FunctionContainer[-A, +B]
  private[compiled] case class RequiredFunctionContainer[-A, +B](f: GenAggFunc[A, B]) extends FunctionContainer[A, B]
  private[compiled] case class OptionalFunctionContainer[-A, +B](f: GenAggFunc[A, Option[B]]) extends FunctionContainer[A, B]
}

sealed trait CompiledSemanticsLike[A]
  extends Semantics[A]
  with EitherHelpers
  with Logging {

  def compiler: ContainerReadable[Try] // See comments in CompiledSemantics case class for documentation.
  def plugin: CompiledSemanticsPlugin[A] // See comments in CompiledSemantics case class for documentation.
  def imports: Seq[String] // See comments in CompiledSemantics case class for documentation.
  def retainGeneratedCode(): Boolean // See comments in CompiledSemantics case class for documentation.
  protected implicit val ec: ExecutionContext // See comments in CompiledSemantics case class for documentation.

  import CompiledSemantics.{ genFuncClassName, maxGenWaitInSec, FunctionContainer, RequiredFunctionContainer, OptionalFunctionContainer }

  private[this]type VarSpecAndDefault = (String, Option[String])

  /**
   * Provides implicit access to the reflection information available about A.
   * @return
   */
  private[this] implicit def riaImpl: RefInfo[A] = plugin.refInfoA

  def refInfoA: RefInfo[A] = riaImpl

  /**
   * The separator between a variable specification and the default value.  This was chosen because it is how
   * defaults are specified to variables in bash.  For more information, see:
   *
   * [[http://tldp.org/LDP/abs/html/parameter-substitution.html Advanced Bash-Scripting Guide, 10.2. Parameter Substitution]]
   */
  private[this] val specDefSep = ":-"

  /**
   * The regular expression that pulls out the variable specifications.  Uses the dollar sign, curly brace syntax.
   *
   * Reserved characters:
   *  - (white space)
   *  - $
   *  - {
   *  - }
   *  - :
   *  - |
   */
  // TODO: "2.0" :-) Change to this and emit an orElse in the GeneratedAccessor definitions.  (This will throw off reporting)
  private[this] val accessorStringRegEx = """\$\{([^\s\$\{\}:\|]+)(:\-([^\s\$\{\}:\|]+))?\}""".r

  /**
   * Non-blocking thread safe cache. This is lazy because we want to use that execution context that is inserted
   * by the implementation.  The nominal performance hit is not a big deal because the cache isn't really hit that
   * hard.
   */
  private[this] lazy val cache = new NoEvictionCache()

  /**
   * Create a function from A to B. Make sure that function compilation is memoized as this is a very expensive
   * operation (10s - 100s of milliseconds).
   * @param codeSpec specification of the function that will be created.
   * @param default a default value in the case that the generated function's return type is optional.
   * @tparam B the codomain of the generated function.
   * @return
   */
  def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): ENS[GenAggFunc[A, B]] = {
    // Future of return value of create function.  We try to cache if not present.
    val f = cache(codeSpec)(create(codeSpec, default)) // Try to pull from cache; otherwise create, cache, return.

    // f represents a future of either a function containing only required accessors or a function with optional
    // accessors.  If the function contains optional accessors, we use the default provided to this function.  That
    // way we can cache the underlying function and if only the default changes, then we don't have to recompile;
    // we just need ot supply the current default.
    val functionFuture = applyDefault(f, codeSpec, default)

    val v = Await.result(functionFuture, maxGenWaitInSec.seconds) // TODO: Remove this when API is changed.

    v.left.foreach { errMsgs => debug(s"Couldn't compile codeSpec: '$codeSpec': ${errMsgs.mkString("\n\t")}") }
    v.right.foreach { func => s"constructed spec '$codeSpec' to function: $func" }

    v // TODO: Change API to actually return the future, f.
  }

  private[this] def applyDefault[B](ftr: Future[ENS[FunctionContainer[A, B]]], codeSpec: String, default: Option[B]) = {
    ftr.map(_.right.flatMap {
      case RequiredFunctionContainer(fn) => Right(fn)
      case OptionalFunctionContainer(fn) =>
        default map { d => Right(OptionalFunc(fn, d)) } getOrElse {
          Left(Seq(s"No optional type provided for originally optional function with spec: $codeSpec", s"function: ${fn.toString()}"))
        }
    })
  }

  /**
   * This is the main entry point for the function construction.  Steps include:
   * 1 Check that we can construct a function with the arity determined by the # of distinct variables.
   * 1 Create the accessors (this is all or nothing). In the process, short circuit if any accessor requires optional data but no function default is specified.
   * 1 Construct the code from the accessors.
   * 1 Compile the function definition.
   *
   * The compile step will require lifting the codomain of the generated function if optional accessors exist.  In
   * such a case, wrap the [[com.eharmony.aloha.semantics.func.GenAggFunc]] [A, Option[B] ] in a
   * [[com.eharmony.aloha.semantics.func.OptionalFunc]].
   * @param codeSpec specification of the function that will be created.
   * @param default a default value in the case that the generated function's return type is optional.
   * @tparam B the codomain of the generated function.
   * @return
   */
  private[this] def create[B: RefInfo](codeSpec: String, default: Option[B]): ENS[FunctionContainer[A, B]] = {

    // Construct the function with the following steps:
    //  1. Check that we can construct a function with the arity determined by the # of distinct variables.
    //  2. Create the accessors.  (This is all or nothing)
    //  3. Construct the code from the accessors.
    //  4. Compile the function definition.
    val function = for {
      descriptors <- extractVariableDescriptors(codeSpec).right
      aok <- assertFunctionArityOk(codeSpec, descriptors.size).right
      acc <- createAccessors(codeSpec, descriptors, default.nonEmpty).right
      c <- constructFunctionCode[B](acc, codeSpec).right // c._1: code; c._2: function has optional accessors?
      //            f <- compile[B](c._1, default.collect{case d if c._2 => d}).right
      f <- compile[B](c._1, c._2).right
    } yield f

    function
  }

  def close() {}

  /**
   * Returns the string representations of all of the data "variables" used by functions created from this Semantics
   * object.
   * @return
   */
  def accessorFunctionNames = ???

  /**
   * Constructs the actual function code to be compiled.  The template for the generated code is illustrated
   * in the following:
   * {{{
   * {
   *   import statements ...
   *   identity[GenAggFunc[A, B] ](rawCode(codeSpec))
   * }
   * }}}
   *
   * We transform the codeSpec to actual code and it is (at this point untyped).  The code is wrapped in a call to
   * scala.Predef.identity with type parameters to necessitate proper typing.  Finally, we provide necessary imports
   *
   * @param accessors variables to be extracted from the domain object.
   * @param codeSpec specification of the function that will be created.
   * @tparam B the codomain of the generated function.
   * @return
   */
  private[this] def constructFunctionCode[B: RefInfo](accessors: Seq[Accessor], codeSpec: String): ENS[(String, Boolean)] = {
    val (code, hasOpt) = rawCode[B](accessors, codeSpec) // Generate the raw (untyped) code
    val typedCode = addType[B](code, hasOpt) // Add typing information
    val finalCode = addImports(typedCode) // Add imports if necessary.

    debug(s"""Creating function with${if (!hasOpt) "out" else ""} optional accessors: $finalCode""")

    success((finalCode, hasOpt)) // Return code and whether code has optional accessors.
  }

  /**
   * Add import statements if necessary.
   * @param code Code to which we add imports if imports are specified.
   * @return
   */
  private[this] def addImports(code: String) = {
    val codeWithPossibleImports =
      if (imports.nonEmpty) {
        val imp = imports.foldLeft(new StringBuilder)(_.append("import ").append(_).append("; "))
        s"{ ${imp}$code }"
      } else code
    codeWithPossibleImports
  }

  /**
   * Wrap code in a call identity with the output type to provide explicit typing information.
   * This output type is exactly GenAggFunc[A, B].
   * @param code the code to decorate with a type.
   * @param hasOpt whether the output type is an option
   * @tparam B the output type (not including option if it is an option).
   * @return
   */
  private[this] def addType[B: RefInfo](code: String, hasOpt: Boolean): String = {
    // TODO: Check this String very carefully.
    val typeStr = if (hasOpt) RefInfoOps.toString(RefInfoOps.wrap[A, Option[B]].in[GenAggFunc])
    else RefInfoOps.toString(RefInfoOps.wrap[A, B].in[GenAggFunc])

    s"identity[$typeStr]($code)"
  }

  /**
   * Get the raw code that will eventually be decorated with typing information and imports specified
   * @param accessors the variable accessors
   * @param codeSpec the code specification used to synthesize the source code to be compiled.
   * @param ri the reflection information about the final function type.
   * @tparam B the output type
   * @return
   */
  private[this] def rawCode[B](accessors: Seq[Accessor], codeSpec: String)(implicit ri: RefInfo[GenAggFunc[A, B]]): (String, Boolean) = {
    val opt = accessors.filter(_.optional)
    val req = accessors.filterNot(_.optional)

    val c = opt.size match {
      case 0 => req.size match {
        case 0 => invariantFunction(codeSpec) // Doesn't rely on any accessors.
        case _ => reqOnlyFunction(req, codeSpec) // Relies only on required accessors.
      }
      case _ => someOptFunction(req, opt, codeSpec) // Relies on at least one optional accessor.
    }

    (c, opt.nonEmpty)
  }

  /**
   * Generate code for a function requiring 0 parameters.  Note that this is not necessarily a constant function:
   * {{{
   * val codeSpec = "new util.Random(System.nanoTime).nextDouble"
   * val f = s.createFunction[Double](codeSpec, None)
   * val y1 = f(null)
   * val y2 = f(null)
   * assert(y1 != y2)
   * }}}
   * @param codeSpec specification of the function that will be created.
   * @return
   */
  private[this] def invariantFunction(codeSpec: String) = {
    val s = s"""$genFuncClassName.f0("${escape(codeSpec)}", (_:Any) => $codeSpec)"""
    s
  }

  /**
   * Generate code for a function requiring at least one optional parameter.
   * @param req Required accessors
   * @param opt Optional accessors
   * @param codeSpec specification of the function that will be created.
   * @return
   */
  private[this] def someOptFunction(req: Seq[Accessor], opt: Seq[Accessor], codeSpec: String) = {
    val allAcc = opt ++ req
    val arity = allAcc.size
    val fArgs = (opt.map(_.impl) ++ req.map(_.impl)).mkString(", ")
    val fArgList = ((0 until opt.size).map("o" + _) ++ req.map(_.varName)).mkString("(", ", ", ")")

    val f = fArgList + " => for (" +
      opt.zipWithIndex.map { case (o, i) => s"${o.varName} <- o$i" }.mkString("; ") +
      ") yield {" +
      expandVariableNames(codeSpec, allAcc) +
      "}"

    val s = genFuncStr(arity, fArgs, f, codeSpec)
    s
  }

  /**
   * Generate code for a function with all parameters being required.
   * @param req Required accessors
   * @param codeSpec specification of the function that will be created.
   * @return
   */
  private[this] def reqOnlyFunction(req: Seq[Accessor], codeSpec: String) = {
    val accArgs = req.map(_.impl).mkString(", ")
    val fArgList = req.map(_.varName).mkString("(", ", ", ")")
    val f = fArgList + " => {" + expandVariableNames(codeSpec, req) + "}"
    val s = genFuncStr(req.size, accArgs, f, codeSpec)
    s
  }

  /**
   * Replace all references of variables in code with the corresponding variable name.
   * @param codeSpec specification of the function that will be created.
   * @param acc accessors
   * @return
   */
  private[this] def expandVariableNames(codeSpec: String, acc: Seq[Accessor]) =
    acc.foldLeft(codeSpec) { case (s, x) => s.replace(x.accessorString, x.varName) }

  /**
   * @param arity arity of the function to be generated
   * @param fArgs the first argument list.  These are the code representation of the accessors.
   * @param f the actual function representation.
   * @param codeSpec specification of the function that will be created.
   * @return
   */
  private[this] def genFuncStr(arity: Int, fArgs: String, f: String, codeSpec: String) = {
    val s = s"""$genFuncClassName.f$arity($fArgs)("${escape(codeSpec)}", $f)"""
    s
  }

  /**
   * Get the distinct variable descriptors in from the function code block.
   * @param codeSpec specification of the function that will be created.
   * @return
   */
  private[this] def extractVariableDescriptors(codeSpec: String): ENS[List[VarSpecAndDefault]] = {
    val matches = accessorStringRegEx.findAllMatchIn(codeSpec).toIndexedSeq

    // Get a map from the name to a sequence of (name, default) pairs.
    val fMap = matches.map(_.subgroups.toList match {
      case List(name) => (name.trim, None)
      case List(name, _, default) => (name.trim, Option(default).map(_.trim)) // Need to map to avoid NPE.
    }).toSeq.groupBy(_._1)

    // Find all of the names where different defaults exist.
    val differentDefaults = fMap.filter(_._2.distinct.size > 1).map(_._1).toSeq.sorted

    // If there are any descriptors with multiple defaults, err.
    if (differentDefaults.isEmpty) success(fMap.values.map(_.head).toList)
    else fail("found the following variables with multiple default values: " + differentDefaults.mkString(", "))
  }

  /**
   * Make sure that there isn't an optional accessor when no default is supplied (unless there)
   * @param codeSpec  the specification of the function to be created.
   * @param descriptor  the accessor descriptor
   * @param vac the representation of the code generated by applying the
   *            [[com.eharmony.aloha.semantics.compiled.CompiledSemanticsPlugin]] to the descriptor.
   * @param defaultExists  Whether a default exists.
   * @return
   */
  private[this] def ensureReqIfNoDef(
    codeSpec: String,
    descriptor: VarSpecAndDefault,
    vac: VariableAccessorCode,
    defaultExists: Boolean): ENS[VariableAccessorCode] = {
    if (vac.isOptional && !defaultExists)
      fail(s"""Function optional accessor: "${descriptor._1}" but no default in code block: $codeSpec.""")
    else success(vac)
  }

  /**
   * Get all of the accessors or fail.  Fail fast when code for an accessor couldn't be produced or it is
   * determined that the an optional accessor would be produced when no default value exists for the function.
   * @param codeSpec a specification of the code to be synthesized.
   * @param descriptors variable specifications.
   * @param defaultExists whether a default for the variable exists
   * @return
   */
  private[this] def createAccessors(codeSpec: String, descriptors: Seq[VarSpecAndDefault], defaultExists: Boolean): ENS[Seq[Accessor]] = {
    mapSeq(descriptors)(d =>
      for {
        vac <- plugin.accessorFunctionCode(d._1).right
        newVac <- transformOptWithDef(vac, d).right
        a <- ensureReqIfNoDef(codeSpec, d, newVac, defaultExists).right
      } yield Accessor(d._1, d._2, newVac))
  }

  /**
   * Transform accessors with defaults.  Has the following cases:
   * 1   No default specified:  return the current code.
   * 1   Default specified for a required variable accessor:  return an error.
   * 1   Default specified for an optional variable accessor:  return a new required variable accessor with the default.
   * @param vac previous accessor
   * @param descriptor information about the variable name and the default.
   * @return
   */
  private[this] def transformOptWithDef(vac: VariableAccessorCode, descriptor: VarSpecAndDefault): ENS[VariableAccessorCode] = {
    val newVac =
      if (descriptor._2.isEmpty) success(vac)
      else if (!vac.isOptional) fail(s"Default ${descriptor._2.get} provided for required accessor ${descriptor._1}")
      else success(RequiredAccessorCode("(" +: vac.body :+ s").andThen(_.getOrElse(${descriptor._2.get}))"))
    newVac
  }

  /**
   * Compile the code.  hasDefault is true if and only if the code contains optional accessors.  If the code
   * contains optional accessors, then the output is also optional.  In such a case, we need to give the output
   * type of the compiled function Option[B].  Then we make note of the so that in createFunction, we can add in a
   * runtime default.
   * @param code code to be compiled.
   * @param hasDefault whether this has a default (meaning the function produces an optional type).
   * @tparam B the output type of the function that is generated.
   * @return a function A => B in a function container that tells whether it relies on optional data.
   */
  private[this] def compile[B](code: String, hasDefault: Boolean): ENS[FunctionContainer[A, B]] = {
    val f = if (hasDefault) compile[Option[B]](code).right.map(OptionalFunctionContainer.apply[A, B])
    else compile[B](code).right.map(RequiredFunctionContainer.apply[A, B])
    f
  }

  private[this] def compile[B](code: String): ENS[GenAggFunc[A, B]] = {
    compiler.fromString[GenAggFunc[A, B]](code).map(success).recover {
      case e: Exception =>
        error(s"Problem compiling code: $code", e)

        // Save stack trace too.
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        fail(e.getMessage, sw.toString)
    }.get
  }

  /**
   * Check that the arity of the desired function can be produced.  This needs to be lte
   * [[com.eharmony.aloha.semantics.func.GenFunc]].maxArity
   * @param codeBlock a block of code for which we check whether the function's arity is supported.
   * @param numVars number of variables in the function.
   * @return
   */
  private[this] def assertFunctionArityOk(codeBlock: String, numVars: Int): ENS[Boolean] =
    if (numVars <= GenFunc.maxArity) success(true)
    else fail(s"Cannot construct function of arity $numVars. Max arity: ${GenFunc.maxArity}.  Code: $codeBlock")

  /**
   * Escape back slashes ''and then'' quotes.
   * @param s a string to escape.
   * @return
   */
  private[this] def escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

  /**
   * Convenience method for producing failures.
   * @param s first (required) failure message
   * @param s1 subsequent failures.
   * @return
   */
  private[this] def fail(s: String, s1: String*): ENS[Nothing] = Left(s +: s1)

  /**
   * Convenience method for producing successes.
   * @param b a success
   * @tparam B success type
   * @return
   */
  private[this] def success[B](b: B): ENS[B] = Right(b)

  /**
   * @param descriptor the variable specification
   * @param default a default embedded at the end of the variable specification
   * @param vac the code for the accessor.
   */
  private[this] case class Accessor(descriptor: String, default: Option[String], vac: VariableAccessorCode) {
    import CompiledSemantics.{ generatedAccessorClassName => gac }
    val optional = vac.isOptional
    def accessorString = "${" + descriptor + default.map(specDefSep + _).getOrElse("") + "}"

    /**
     * The name of variable in the generated function.
     * Replace all non-alpha-numeric, non-underscore characters with double underscore.
     * @return
     */
    def varName = "_" + descriptor.replaceAll("""\W""", "__")

    /**
     * Generate the self-sufficient code that compiles down to one accessor.
     * '''NOTE''': Whether the code is retained is up to the code that extends the contain trait
     * ([[com.eharmony.aloha.semantics.compiled.CompiledSemanticsLike]]).  This is because we don't want
     * to necessarily keep it in production if we are not choosing to
     * @return
     */
    // TODO: Harden this code.  The escape and vac.pretty.
    def impl = {
      val codeStr = if (retainGeneratedCode()) s"""Some(\"\"\"${escape(vac.pretty)}\"\"\")""" else "None"
      s"""$gac("$descriptor", ${vac.compressed}, $codeStr)"""
    }
  }
}
