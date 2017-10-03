package com.eharmony.aloha.reflect

import com.eharmony.aloha
import org.reflections.Reflections

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Created by ryan.deak on 9/6/17.
  */
trait RuntimeClasspathScanning {

  private[this] val objectSuffix = "$"

  /**
    * Determine if the class is a Scala object by looking at the class name.  If it
    * ends in the `objectSuffix`
    * @param c a `Class` instance.
    * @tparam A type of Class.
    * @return
    */
  private[this] def isObject[A](c: Class[A]) = c.getCanonicalName.endsWith(objectSuffix)

  /**
    * Scan the classpath within subpackages of `packageToSearch` to find Scala objects that
    * contain extend `OBJ` and contain method called `methodName` that should return an `A`.
    * Call the method and get the result.  If the result is indeed an `A`, add to it to
    * the resulting sequence.
    *
    * This function makes some assumptions.  It assumes that Scala objects have classes
    * ending with '$'.  It also assumes that methods in objects have static forwarders.
    *
    * @param methodName the name of a method in an object that should be found and called.
    * @param packageToSearch the package to search for candidates
    * @tparam OBJ the super type of the objects the search should find
    * @tparam A the output type of elements that should be returned by the method named
    *           `methodName`
    * @return a sequence of `A` instances that could be found.
    */
  protected[this] def scanObjects[OBJ: ClassTag, A: ClassTag](
      methodName: String,
      packageToSearch: String = aloha.pkgName
  ): Seq[A] = {
    val reflections = new Reflections(aloha.pkgName)
    import scala.collection.JavaConversions.asScalaSet
    //    val classA = implicitly[ClassTag[A]].runtimeClass
    val objects = reflections.getSubTypesOf(implicitly[ClassTag[OBJ]].runtimeClass).toSeq

    val suffixLength = objectSuffix.length

    objects.flatMap {
      case o if isObject(o) =>
        Try {
          // This may have some classloading issues.
          val classObj = Class.forName(o.getCanonicalName.dropRight(suffixLength))
          classObj.getMethod(methodName).invoke(null) match {
            case a: A => a
            case _ => throw new IllegalStateException()
          }
        }.toOption
      case _ => None
    }
  }
}

