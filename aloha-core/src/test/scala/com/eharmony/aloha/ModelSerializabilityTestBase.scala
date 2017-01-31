package com.eharmony.aloha

import scala.language.existentials
import com.eharmony.aloha
import com.eharmony.aloha.models.Model
import org.junit.Assert._
import org.junit.Test
import org.reflections.Reflections

import scala.collection.JavaConversions.asScalaSet
import scala.util.Try

import java.lang.reflect.{Method, Modifier}

/**
  * Created by ryan on 12/7/15.
  */
abstract class ModelSerializabilityTestBase(pkgs: Seq[String], outFilters: Seq[String]) {
  def this() = this(pkgs = Seq(aloha.pkgName), Seq.empty)

  @Test def testSerialization(): Unit = {
    val modelClasses = new Reflections(pkgs:_*).
      getSubTypesOf(classOf[Model[Any, Nothing]]).toSeq.view.
      filterNot{ _.isInterface }.
      filterNot { c =>
        val name = c.getName
        outFilters.exists(name.matches)
      }

    modelClasses.foreach { c =>
      val m = for {
        testClass  <- getTestClass(c.getCanonicalName)
        testMethod <- getTestMethod(testClass)
        method     <- ensureTestMethodIsTest(testMethod)
      } yield method

      m.left foreach fail
    }
  }

  private[this] implicit class RightMonad[L, R](e: Either[L, R]) {
    def flatMap[R1](f: R => Either[L, R1]) = e.right.flatMap(f)
    def map[R1](f: R => R1) = e.right.map(f)
  }

  private[this] def getTestClass(modelClassName: String) = {
    val testName = modelClassName + "Test"
    Try {
      Class.forName(testName)
    } map {
      Right(_)
    } getOrElse Left("No test class exists for " + modelClassName)
  }

  private[this] def getTestMethod(testClass: Class[_]) = {
    val testMethodName = "testSerialization"
    lazy val msg = s"$testMethodName doesn't exist in ${testClass.getCanonicalName}."
    Try {
      Option(testClass.getMethod(testMethodName))
    } map {
      case Some(m) => Right(m)
      case None => Left(msg)
    } getOrElse Left(msg)
  }

  private[this] def ensureTestMethodIsTest(method: Method) = {
    if (!Modifier.isPublic(method.getModifiers))
      Left(s"testSerialization in ${method.getDeclaringClass.getCanonicalName} is not public")
    if (!method.getDeclaredAnnotations.exists(_.annotationType() == classOf[Test]))
      Left(s"testSerialization in ${method.getDeclaringClass.getCanonicalName} does not have a @org.junit.Test annotation.")
    else if (method.getReturnType != classOf[Void] && method.getReturnType != classOf[Unit])
      Left(s"testSerialization in ${method.getDeclaringClass.getCanonicalName} is not a void function. It returns: ${method.getReturnType}")
    else Right(method)
  }
}