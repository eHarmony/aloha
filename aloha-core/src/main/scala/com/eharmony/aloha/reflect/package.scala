package com.eharmony.aloha

/** A facade layer on top of the scala reflection APIs to avoid bugs currently in the TypeTag
  * implementation.
  *
  * Because of [[https://issues.scala-lang.org/browse/SI-7555 SI-7555]], we want to insulate
  * ourselves from reflection bugs. We do want to make the switch painless when we finally go
  * from scala.reflect.Manifest to scala.reflect.runtime.universe.TypeTag based reflection.  To
  * accomplish this, we add a layer of indirection and later, we'll only have to modify the facade
  * assuming that all of the code using reflection refers to the facade.  Once everything has
  * settled down on the scala reflection landscape, we'll refer directly to the reflection APIs.
  */
package object reflect {

    /** Type alias for Reflection meta-information type.  Manifest, TypeTag, etc.
      * Currently, this is Manifest.  Once [https://issues.scala-lang.org/browse/SI-7555 SI-7555] is
      * addressed, we can move over to a scala.reflect.runtime.universe.TypeTag based implementation.
      *
      * '''NOTE''': If all goes well, we should be able to just swap this one type definition and
      * everything will still work (once scala 2.10-based reflection is fixed).  If this isn't the
      * case, we need to add and reference additional functionality in the facade layer.
      */
    type RefInfo[A] = Manifest[A]
}
