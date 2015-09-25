package com.eharmony.aloha.models.h2o

import java.io.File
import java.nio.charset.Charset
import java.util.Locale

import com.eharmony.aloha.reflect.RefInfo

class Compiler2[B: RefInfo](code: String, locale: Locale = Locale.getDefault, charSet: Charset = Charset.defaultCharset, classDir: String = ) {

}

object Compiler2 {
  def tmpDir = {

  }
}

//
///**
// * Created by deak on 9/21/15.
// */
//class Compiler[B: RefInfo](code: String)(implicit locale: Locale, codec: Codec) {
//  val compiler = ToolProvider.getSystemJavaCompiler
//
//  // make these more explicit; especially locale and charset.
//  val fileManager = compiler.getStandardFileManager(null, locale, codec.charSet)
//
//  val compilationUnit = InMemoryJavaSource.fromString[B](code).get
//
//  val diagnostics = new DiagnosticCollector[JavaFileObject]
//
//  new File("/Users/deak/git/aloha/aloha-h2o/target/test-classes/generated-classes").mkdir()
//
//  val genClassDir = "/Users/deak/git/aloha/aloha-h2o/target/test-classes/generated-classes"
//
//  val locations = asJavaIterable(Seq("-d", genClassDir))
//
//  val compilerTask = compiler.getTask(null, null, diagnostics, locations, null, Iterable(compilationUnit))
//
//  lazy val x: Try[B] = if (compilerTask.call()) {
//    diagnostics.getDiagnostics.foreach { diagnostic =>
//      println(diagnostic.getCode)
//      println(diagnostic.getKind)
//      println(diagnostic.getPosition)
//      println(diagnostic.getStartPosition)
//      println(diagnostic.getEndPosition)
//      println(diagnostic.getSource)
//      println(diagnostic.getMessage(null))
//    }
//
//    Try {
//      val classLoader = new URLClassLoader(Array(new File(genClassDir).toURI.toURL))
//      val clazz = classLoader.loadClass(compilationUnit.className)
////      ToolProvider.getSystemToolClassLoader.loadClass(compilationUnit.className).newInstance()
////      val x = Class.forName(compilationUnit.className)
//      val inst = clazz.newInstance()
//      inst
//    } flatMap {
//      case b: B => Try(b)
//      case d => Failure(new IllegalArgumentException(s"Expected ${RefInfoOps.toString[B]}.  Found: ${d.getClass.getCanonicalName}"))
//    }
//  }
//  else {
//    Failure(new Exception(s"Compilation failed for ${compilationUnit.className}."))
//  }
//
//  lazy val value = x.transform(s => Try(Right(s)), f => Try(Left(f.getClass.getCanonicalName + ": " + f.getMessage))).get
//}
//
//case class InMemoryJavaSource[B](code: String, className: String)(implicit baseClassInfo: RefInfo[B])
//extends SimpleJavaFileObject(InMemoryJavaSource.classNameToUri(className), Kind.SOURCE) {
//  override def getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
//}
//
//object InMemoryJavaSource {
//  val pkgRe = """package\s+([a-z_][a-zA-Z0-9_]*(\.[a-z_][a-zA-Z0-9_]*)*)\s*;""".r
//
//  def fromString[B](code: String)(implicit baseClass: RefInfo[B]): Option[InMemoryJavaSource[B]] = {
//    val classNameRe = classNameRegex[B](code)
//    val className = determineClassName(code, classNameRegex[B](code)) map determineCanonicalClassName(code)
//    className map (cn => new InMemoryJavaSource[B](code, cn))
//  }
//
//  def classNameRegex[B](code: String)(implicit baseClass: RefInfo[B]): Regex = {
//    val ext = if (RefInfoOps.isJavaInterface[B]) "implements" else "extends"
//    val re = RefInfoOps.classRegex[B].toString()
//    ("""public\s+class\s+([A-Za-z_][0-9A-Za-z_]*)\s+""" + ext + """\s+""" + re + """[\s<\{]""").r
//  }
//
//  def determineClassName(code: String, classNameRegex: Regex): Option[String] = classNameRegex.findFirstMatchIn(code) map { _.group(1) }
//
//  def determineCanonicalClassName(code: String)(className: String) = (pkgRe.findFirstIn(code) getOrElse "") + className
//
//  def classNameToUri(className: String): URI = URI.create("string:///" + className.replace(".", "/") + Kind.SOURCE.extension)
//}
