package com.eharmony.aloha.reflect

import scala.language.higherKinds
import scala.util.matching.Regex
import scalaz.{ Validation, ValidationNel }
import scalaz.syntax.validation.ToValidationV // scalaz.syntax.validation.ToValidationOps for latest scalaz

import java.lang.reflect.Modifier
import com.eharmony.aloha.semantics.func.GenAggFunc
import scala.reflect.ManifestFactory

import com.eharmony.aloha.util.EitherHelpers
import scala.collection.{immutable => sci}
/**
 * A facade layer on top of the scala reflection APIs to avoid bugs currently in the reflection implementation.
 * For more information, see [[https://issues.scala-lang.org/browse/SI-7555 SI-7555]].
 * @tparam RefInfoType The reflection meta-information container.
 */
sealed trait RefInfoOps[RefInfoType[_]] {

  def typeParams[A](implicit a: RefInfoType[A]): List[RefInfoType[_]]

  /**
    * Attempt to determine if `possibleIterable` is a `scala.collection.immutable.Iterable[A]` but not
    * a `scala.collection.Map`.
    *
    * This is kind of a stopgap solution because the subtype operator `<:<` doesn't really work well
    * for Manifests.  This is an issue because currently, Aloha uses Manifests rather than
    * `TypeTag`s for type checking.  Comments in ClassManifestDeprecatedApis says:
    *
    * "''this part is wrong for punting unless the rhs has no type arguments, but it's better
    * than a blindfolded pinata swing.''"
    *
    * @param a RefInfo instance of element type
    * @param possibleIterable a possible Iterable
    * @tparam A element type
    * @tparam Iter possible iterable type.
    * @return true if `possibleIterable` is a `scala.collection.immutable.Iterable[A]`
    *         but not a `scala.collection.Map[_, _]`.
    */
  def isImmutableIterableButNotMap[A, Iter](implicit a: RefInfoType[A], possibleIterable: RefInfoType[Iter]): Boolean

  /**
   * Is Sub a subtype of Super
   * @param sub RefInfo instance of type A
   * @param sup RefInfo instance of type A
   * @tparam Sub subtype in test
   * @tparam Super supertype in test
   */
  def isSubType[Sub, Super](implicit sub: RefInfoType[Sub], sup: RefInfoType[Super]): Boolean

  /**
   * Provides a generic way of lifting A into a container for which a RefInfoType instance can be retrieved.
   * @tparam A type to be lifted.
   */
  trait Lift1[A] {
    def in[M[_]](implicit w: RefInfoType[M[A]]): RefInfoType[M[A]] = w
  }

  private[this] class C1[A: RefInfoType] extends Lift1[A]

  /**
   * Provides a generic way of lifting A and B into a container for which a RefInfoType instance can be retrieved.
   * @tparam A first type to be lifted.
   * @tparam B second type to be lifted.
   */
  trait Lift2[A, B] {
    def in[M[_, _]](implicit w: RefInfoType[M[A, B]]): RefInfoType[M[A, B]] = w
  }

  private[this] class C2[A: RefInfoType, B: RefInfoType] extends Lift2[A, B]

  /**
   * Get the RefInfoType instance for type A.
   * @tparam A type for which reflection information should be retrieved.
   * @return
   */
  def refInfo[A: RefInfoType] = implicitly[RefInfoType[A]]

  /**
   * Get a Lift1 instance to wrap type A in a RefInfoType.
   * {{{
   * val w = RefInfoOps.wrap[String]
   * val o1 = w.in[Option]
   * val o2 = RefInfoOps.option[String]
   * assert(o1 == o2)
   * }}}
   * @tparam A type being lifted
   * @return
   */
  def wrap[A: RefInfoType] = new C1[A].asInstanceOf[Lift1[A]]

  /**
   * Provides a generic mechanism to wrap the types A and B inside a container taking two parameters.
   *
   * {{{
   * val w = RefInfoOps.wrap[String, Double]
   * // pass around w to some other object ...
   * val e: RefInfo[Either[String, Double]] = w.in[Either]
   *
   * import scalaz.Validation
   * val v1 = RefInfoOps.wrap[String, Double].in[Validation]
   * val v2 = RefInfoOps.validation[String, Double]
   * assert(v1 == v2)
   * }}}
   *
   * @tparam A first type being lifted.
   * @tparam B second type being lifted.
   * @return a Lift2 instance containing information about A and B that can be used to later construct a
   *         RefInfoType from a kind passed to the in function.
   */
  def wrap[A: RefInfoType, B: RefInfoType] = new C2[A, B].asInstanceOf[Lift2[A, B]]

  /**
   * Get reflection info from a simple Class.  Will throw an exception if we find that the type is parametrized.
   * @param clazz a Class object
   * @tparam A the type of the RefInfoType to create
   * @return
   */
  @throws[IllegalArgumentException]("If clazz is null RefInfoType's string representation contains brackets")
  def fromSimpleClass[A](clazz: Class[_ <: A]): RefInfoType[A]

  def execStaticNoArgFunc[A: RefInfo](name: String): Either[Seq[String], AnyRef]

  /**
   * Get reflection information about scalaz.Validation.
   * @tparam A left type parameter of Validation
   * @tparam B right type parameter of Validation
   * @return reflection information about scalaz.Validation
   */
  def validation[A: RefInfoType, B: RefInfoType]: RefInfoType[Validation[A, B]]

  /**
   * Get reflection information about scalaz.ValidationNel.
   * @tparam A left type parameter of ValidationNel
   * @tparam B right type parameter of ValidationNel
   * @return reflection information about scalaz.ValidationNel
   */
  def validationNel[A: RefInfoType, B: RefInfoType]: RefInfoType[ValidationNel[A, B]]

  /**
   * Get reflection information about [[com.eharmony.aloha.semantics.func.GenAggFunc]]
   * @tparam A function input type
   * @tparam B function output type
   * @return reflection information about [[com.eharmony.aloha.semantics.func.GenAggFunc]]
   */
  def genAggFunc[A: RefInfoType, B: RefInfoType]: RefInfoType[GenAggFunc[A, B]]

  /**
   * Get reflection information about scala.util.Either.
   * @tparam A left type parameter of Either
   * @tparam B right type parameter of Either
   * @return reflection information about scala.util.Either
   */
  def either[A: RefInfoType, B: RefInfoType]: RefInfoType[Either[A, B]]

  /**
   * Get reflection information about Option.
   * @tparam A type of Option
   * @return reflection information about Option.
   */
  def option[A: RefInfoType]: RefInfoType[Option[A]]

  /**
   * Get a string representation of the RefInfoType.  This should be adequate to full characterize the type
   * and any type parameters (i.e., should contain the erased type information)
   * @tparam A type of the reflected object whose string-based representation the function is retrieving.
   * @return string-based representation of A
   */
  def toString[A: RefInfoType]: String
}

/**
 * Concrete implementations for retrieving and manipulating reflection meta information.  All reflection
 * in the library should use this facade and should not use Manifests or TypeTags.
 */
object RefInfoOps extends RefInfoOps[RefInfo] with EitherHelpers {

  def typeParams[A](implicit a: RefInfo[A]): List[RefInfo[_]] = a.typeArguments

  /**
    * Attempt to determine if `possibleIterable` is a `scala.collection.immutable.Iterable[A]` but not
    * a `scala.collection.Map`.
    *
    * This is kind of a stopgap solution because the subtype operator `<:<` doesn't really work well
    * for Manifests.  This is an issue because currently, Aloha uses Manifests rather than
    * `TypeTag`s for type checking.  Comments in ClassManifestDeprecatedApis says:
    *
    * "''this part is wrong for punting unless the rhs has no type arguments, but it's better
    * than a blindfolded pinata swing.''"
    *
    * This implementation covers many simple cases in the intersection of scala 2.10
    * and 2.11 instances.  See test for coverage.
    *
    * @param a RefInfo instance of element type
    * @param possibleIterable a possible Iterable
    * @tparam A element type
    * @tparam Iter possible iterable type.
    * @return true if `possibleIterable` is a `scala.collection.immutable.Iterable[A]`
    *         but not a `scala.collection.Map[_, _]`.
    */
  def isImmutableIterableButNotMap[A, Iter](implicit a: RefInfo[A], possibleIterable: RefInfo[Iter]): Boolean = {
    classOf[scala.collection.immutable.Iterable[_]].isAssignableFrom(possibleIterable.runtimeClass) &&
    !classOf[scala.collection.Map[_, _]].isAssignableFrom(possibleIterable.runtimeClass) &&
    (List(a) == possibleIterable.typeArguments ||
      (possibleIterable.typeArguments.isEmpty && (
        (a == RefInfo.Int &&
          (possibleIterable == RefInfo[sci.BitSet] ||
           possibleIterable == RefInfo[sci.BitSet.BitSet1] ||
           possibleIterable == RefInfo[sci.BitSet.BitSet2] ||
           possibleIterable == RefInfo[sci.BitSet.BitSetN] ||
           possibleIterable == RefInfo[sci.Range])) ||
        (a == RefInfo.Char &&
           possibleIterable == RefInfo[sci.WrappedString]))))
  }


  /**
   * Is Sub a subtype of Super
   * @param sub RefInfo instance of type A
   * @param sup RefInfo instance of type A
   * @tparam Sub subtype in test
   * @tparam Super supertype in test
   */
  @SuppressWarnings(Array("deprecation"))
  def isSubType[Sub, Super](implicit sub: RefInfo[Sub], sup: RefInfo[Super]): Boolean = sub <:< sup

  def isJavaInterface[A](implicit a: RefInfo[A]) = a.runtimeClass.isInterface

  def classRegex[A](implicit a: RefInfo[A]): Regex = {
    val erasure = a.runtimeClass
    val pkg = erasure.getPackage.getName
    val simpleName = erasure.getSimpleName
    val canonicalName = erasure.getCanonicalName
    val name = canonicalName.drop(pkg.length)
    Seq(canonicalName, name, simpleName).distinct.mkString("(", "|", ")").replace(".", "\\.").r
  }

  /**
   * Get reflection info from a simple Class.  Will throw an exception if we find that the type is parametrized.
   * @param clazz a Class object
   * @tparam A the type of the RefInfoType to create
   * @return reflection information about simple type represented by clazz.
   */
  @throws[IllegalArgumentException]("If clazz is null RefInfoType's string representation contains brackets")
  def fromSimpleClass[A](clazz: Class[_ <: A]): RefInfo[A] = {
    require(clazz != null, "clazz must not be null")
    val ri = ManifestFactory.classType[A](clazz)
    val s = RefInfoOps.toString[A](ri)
    require(s.find(c => c == '[' || c == ']').isEmpty, s"clazz appears to be parametrized: $s")
    ri
  }

  def execStaticNoArgFunc[A: RefInfo](name: String): Either[Seq[String], AnyRef] = {
    // See http://stackoverflow.com/a/15096862/189964 when changing over to TypeTag-based reflection.

    val v: ValidationNel[String, AnyRef] =
      try {
        val r = for {
          methods <- implicitly[RefInfo[A]].runtimeClass.getMethods.successNel
          filtered <- methods.filter(m => m.getName == name && Modifier.isStatic(m.getModifiers) && 0 == m.getParameterTypes.size).successNel
          f <- filtered.headOption map { _.successNel } getOrElse s"Couldn't find method $name in ${RefInfoOps.toString[A]}".failNel
          y <- f.invoke(null).successNel
        } yield y
        r
      } catch {
        case e: Throwable => s"Couldn't execute ${RefInfoOps.toString[A]}.$name.  Error: ${e.getMessage}".failNel
      }
    fromValidationNel(v)
  }

  /**
   * Get reflection information about scalaz.Validation.
   * @tparam A left type parameter of Validation
   * @tparam B right type parameter of Validation
   * @return reflection information about scalaz.Validation
   */
  def validation[A: RefInfo, B: RefInfo] = wrap[A, B].in[Validation]

  /**
   * Get reflection information about scalaz.ValidationNel.
   * @tparam A left type parameter of ValidationNel
   * @tparam B right type parameter of ValidationNel
   * @return reflection information about scalaz.ValidationNel
   */
  def validationNel[A: RefInfo, B: RefInfo] = wrap[A, B].in[ValidationNel]

  /**
   * Get reflection information about [[com.eharmony.aloha.semantics.func.GenAggFunc]]
   * @tparam A function input type
   * @tparam B function output type
   * @return reflection information about [[com.eharmony.aloha.semantics.func.GenAggFunc]]
   */
  def genAggFunc[A: RefInfo, B: RefInfo] = wrap[A, B].in[GenAggFunc]

  /**
   * Get reflection information about scala.util.Either.
   * @tparam A left type parameter of Either
   * @tparam B right type parameter of Either
   * @return reflection information about scala.util.Either
   */
  def either[A: RefInfo, B: RefInfo] = wrap[A, B].in[Either]

  /**
   * Get reflection information about Option.
   * @tparam A type of Option
   * @return reflection information about Option.
   */
  def option[A: RefInfo] = wrap[A].in[Option]

  /**
   * Get a string representation of the RefInfoType.  This should be adequate to full characterize the type
   * and any type parameters (i.e., should contain the erased type information)
   * @tparam A type of the reflected object whose string-based representation the function is retrieving.
   * @return string-based representation of A
   */
  def toString[A: RefInfo] = implicitly[RefInfo[A]].toString().replace("$", ".")
}
