package com.eharmony.aloha.io.multiple

import java.{util => ju}
import scala.collection.JavaConversions.{collectionAsScalaIterable, asJavaCollection}
import scala.language.higherKinds
import scalaz.Functor
import com.eharmony.aloha.io.sources.ReadableSource

/** Allows a way to read multiple readable types at once.  This is especially useful because we can use this in
  * spring with a model factory that is prototype scoped so that we can create the model factory once, it reads all of
  * the models in bulk and then the factory can be thrown away.  This way, we don't have large unnecessary objects
  * lying around that are only useful at initialization time.
  *
  * @tparam A the readable sources.  This allows objects that mix in only a subset of Readable interfaces to have type
  *           safety when trying to parse multiple sources.  This allows the programmer to use the type system instead
  *           of needing to deal with cases that shouldn't exists.  For instance, imagine implementing just
  *           NonFileReadable.  Then, we shouldn't have to test for cases where the readable source refers to a File.
  *           The type hierarchy of ReadableSource allows this.
  * @tparam B the output type of the objects produced by consuming the Readables.
  */
trait MultipleReadable[A <: ReadableSource, B] {

    /** Parse a list of readable sources and return the results.
      * @param readableTypes a list of readable sources containing the necessary information to parse the resource and
      *                      produce the proper outputs.
      * @return a list of objects produced by parsing the resources in the readable types.
      */
    def fromMultipleSources(readableTypes: List[A]): List[B] = fromMultipleSources[List](readableTypes)(scalaz.std.list.listInstance)

    /** Parse a bunch of readable sources from a java collection and return the results.
      * @param readableTypes a collection of sources from which
      * @return
      */
    def fromMultipleSources(readableTypes: ju.Collection[A]): ju.Collection[B] =
        asJavaCollection(fromMultipleSources[Vector](collectionAsScalaIterable(readableTypes).toVector)(scalaz.std.vector.vectorInstance))

    /** Parse a bunch of readable sources given a Functor (see [[http://thedet.wordpress.com/2012/04/28/functors-monads-applicatives-can-be-so-simple/ Functors, Monads, Applicatives – can be so simple]]).
      * @param readableTypes a container of instances to convert into type the output type
      * @param f a function instance
      * @tparam F the functor type
      * @return a container ''F'' of the output type ''B''
      */
    def fromMultipleSources[F[_]](readableTypes: F[A])(implicit f: Functor[F]) = f.map(readableTypes)(mapper)

    /** A function that maps from the Readable type to the output type.
      * @return a function.
      */
    def mapper: A => B
}
