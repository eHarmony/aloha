package com.eharmony.aloha.models.h2o.compiler

import java.net.URI
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject

import com.eharmony.aloha.io.{ReadableByString, AlohaReadable, ContainerReadableByString, ContainerReadable}
import com.eharmony.aloha.reflect.{RefInfoOps, RefInfo}

import scala.util.matching.Regex

private[h2o] case class InMemoryJavaSource[B](code: String, className: String)(implicit baseClassInfo: RefInfo[B])
extends SimpleJavaFileObject(InMemoryJavaSource.classNameToUri(className), Kind.SOURCE) {
  override def getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
}

private[h2o] object InMemoryJavaSource {
  val pkgRe = """package\s+([a-z_][a-zA-Z0-9_]*(\.[a-z_][a-zA-Z0-9_]*)*)\s*;""".r

  def fromString[B](code: String)(implicit baseClass: RefInfo[B]): Option[InMemoryJavaSource[B]] = {
    val classNameRe = classNameRegex[B](code)
    val className = determineClassName(code, classNameRegex[B](code)) map determineCanonicalClassName(code)
    className map (cn => new InMemoryJavaSource[B](code, cn))
  }

  def classNameRegex[B](code: String)(implicit baseClass: RefInfo[B]): Regex = {
    val ext = if (RefInfoOps.isJavaInterface[B]) "implements" else "extends"
    val re = RefInfoOps.classRegex[B].toString()
    ("""public\s+class\s+([A-Za-z_][0-9A-Za-z_]*)\s+""" + ext + """\s+""" + re + """[\s<\{]""").r
  }

  def determineClassName(code: String, classNameRegex: Regex): Option[String] =
    classNameRegex.findFirstMatchIn(code) map { _.group(1) }

  def determineCanonicalClassName(code: String)(className: String) =
    (pkgRe.findFirstIn(code) map { case pkgRe(p, _) => s"$p." } getOrElse "") + className

  def classNameToUri(className: String): URI =
    URI.create("string:///" + className.replace(".", "/") + Kind.SOURCE.extension)
}
