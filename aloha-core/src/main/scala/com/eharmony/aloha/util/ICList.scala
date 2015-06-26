package com.eharmony.aloha.util

import scala.language.implicitConversions

/** A class that acts like an immutable list but allows disparate element types to be inserted as long as an implicit
  * function from the input type to the list type exists in the implicit scope.
  *
  * Typical usage will be like:
  *
  * {{{
  * import java.io.InputStream
  * def getInputStream(i: Int) = new java.io.ByteArrayInputStream(i.toString.getBytes)
  *
  * implicit val stringToInt = (_:String).toInt
  * implicit val isToInt = (is: InputStream) => stringToInt(io.Source.fromInputStream(is).getLines.mkString("\n"))
  *
  * // Construct a list of Int from things we can convert to Ints from translation functions that are in scope.
  * // NOTICE: Because of implicitConversionListToList, we get implicit conversion to lists.
  * val lst: List[Int] = (1 :: getInputStream(2) :: "3" :: ICList.empty[Int])
  *
  * require(lst.isInstanceOf[List[Int]] && (1 to 3) == lst, "lst should be List(1,2,3)")
  * }}}
  *
  * '''Note''': We use implicit functions rather than view bounds because of conflicts with ''method conforms in object
  * Predef of type [A]=> <:<[A,A]'' for certain types.
  *
  * @param list a contained list backing the ICList.
  * @tparam A the list type
  */
case class ICList[A](list: List[A]) {

    /** Construct a new ICList with ''f(b)'' prepended.
      * @param b a value to prepend to the ICList
      * @param f a function to convert the input to a value of type ''A''
      * @tparam B the input type to convert to type ''A''
      * @return a new ICList of type ''A'' with ''f(b)'' prepended.
      */
    def ::[B](b: B)(implicit f: B => A) = ICList(f(b) :: list)

    /** Construct a new ICList with ''f(b)'' prepended.
      * @param b a value to prepend to the ICList
      * @param f a function to convert the input to a value of type ''A''
      * @tparam B the input type to convert to type ''A''
      * @return a new ICList of type ''A'' with ''f(b)'' prepended.
      */
    def prepend[B](b: B)(implicit f: B => A) = this.::(b)

    /** Construct a new ICList with ''f(b)'' appended.
      * @param b a value to append to the ICList
      * @param f a function to convert the input to a value of type ''A''
      * @tparam B the input type to convert to type ''A''
      * @return a new ICList of type ''A'' with ''f(b)'' appended.
      */
    def append[B](b: B)(implicit f: B => A) = ICList(list :+ f(b))

    /** Construct a new ICList with ''a'' prepended.
      * @param a a value to prepend to the ICList
      * @return a new ICList of type ''A'' with ''a'' prepended.
      */
    def prepend(a: A) = prepend[A](a)(identity)

    /** Construct a new ICList with ''a'' appended.
      * @param a a value to append to the ICList
      * @return a new ICList of type ''A'' with ''a'' appended.
      */
    def append(a: A) = append[A](a)(identity)

    /** Explicitly convert to a list.
      * @return
      */
    def toList = list
}


/**
  */
object ICList {

    private[this] val nil = ICList(Nil)

    /** Get an empty ICList of type ''A''
      * @tparam A empty of the empty list.
      * @return an empty ICList.
      */
    def empty[A]: ICList[A] = nil.asInstanceOf[ICList[A]]

    /** Implicitly convert to a list.  This allows for the example where we can construct an ICList and store in a List.
      *
      * {{{
      * import java.io.InputStream
      * def getInputStream(i: Int) = new java.io.ByteArrayInputStream(i.toString.getBytes)
      *
      * implicit val stringToInt = (_:String).toInt
      * implicit val isToInt = (is: InputStream) => stringToInt(io.Source.fromInputStream(is).getLines.mkString("\n"))
      *
      * // Construct a list of Int from things we can convert to Ints from translation functions that are in scope.
      * // NOTICE: Because of implicitConversionListToList, we get implicit conversion to lists.
      * val lst: List[Int] = (1 :: getInputStream(2) :: "3" :: ICList.empty[Int])
      *
      * require(lst.isInstanceOf[List[Int]] && (1 to 3) == lst, "lst should be List(1,2,3)")
      * }}}
      *
      * @param l an ICList to convert
      * @tparam A the type of the list
      * @return a List of type ''A''.
      */
    implicit def implicitConversionListToList[A](l: ICList[A]): List[A] = l.list
}
