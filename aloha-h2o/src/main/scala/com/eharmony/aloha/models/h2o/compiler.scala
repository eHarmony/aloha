package com.eharmony.aloha.models.h2o

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.util.Locale
import javax.tools.JavaCompiler.CompilationTask
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}

import com.eharmony.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.aloha.models.h2o.compiler.{CompilationError, InMemoryJavaSource}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging

import scala.collection.JavaConversions.{asJavaIterable, collectionAsScalaIterable}
import scala.util.{Failure, Success, Try}


// TODO: Change classDir to an Option.  Delete dir when done when None supplied.
// make this AlohaReadableByString
private[h2o] class Compiler[B: RefInfo](classDir: File = Compiler.tmpDir,
                                        locale: Locale   = Locale.getDefault,
                                        charSet: Charset = Charset.defaultCharset)
extends AlohaReadable[Try[B]]
   with ReadableByString[Try[B]] {

  /** Read from a String.
    * @param code a String to read.
    * @return the result
    */
  override def fromString(code: String): Try[B] = {
    implicit val diagnosticCollector = new DiagnosticCollector[JavaFileObject]

    for {
      _               <- mkDir(classDir)
      compilationUnit <- getCompilationUnit(code)
      compileTask     <- getCompileTask(compilationUnit)
      diagnostics     <- compile(compileTask)
      untypedInstance <- instantiate(compilationUnit)
      instance        <- cast(untypedInstance)
    } yield instance
  }

  def getCompileTask(compilationUnit: InMemoryJavaSource[B])
                    (implicit diagnosticCollector: DiagnosticCollector[JavaFileObject]) = Try {
    val locations = asJavaIterable(Seq("-d", classDir.getCanonicalPath))
    val compiler = ToolProvider.getSystemJavaCompiler
    val fileManager = compiler.getStandardFileManager(null, locale, charSet)
    compiler.getTask(null, null, diagnosticCollector, locations, null, Iterable(compilationUnit))
  }

  def compile(compileTask: CompilationTask)
             (implicit diagnosticCollector: DiagnosticCollector[JavaFileObject]) =
    Try {
      compileTask.call()
    } transform (
      b => {
        val diagnostics = collectionAsScalaIterable(diagnosticCollector.getDiagnostics)
        if (b) Success(diagnostics)
        else   Failure(CompilationError(diagnostics))
      },
      // Failure(new Exception(s"Compilation failed for ${compilationUnit.className}."))
      f => Failure(f)
    )

  def instantiate(compilationUnit: InMemoryJavaSource[B]) = Try[Any] {
    val classLoader = new URLClassLoader(Array(classDir.toURI.toURL))
    val clazz = classLoader.loadClass(compilationUnit.className)
    clazz.newInstance()
  }

  def cast(instance: Any): Try[B] = instance match {
    case b: B => Try(b)
    case d    => Failure(new IllegalArgumentException(s"Expected ${RefInfoOps.toString[B]}.  Found: ${d.getClass.getCanonicalName}"))
  }

  def getCompilationUnit(code: String): Try[InMemoryJavaSource[B]] =
    InMemoryJavaSource.fromString[B](code).map(Success(_)) getOrElse {
      Failure(new IllegalArgumentException("Couldn't create InMemoryJavaSource."))
    }

  def mkDir(f: File) = Try[Unit] {
    if(!f.exists())
      f.mkdir()
  }
}

private[h2o] object Compiler extends Logging {
  def tmpDir: File = {
    val f = File.createTempFile("javacompiler", "classdir")
    debug(s"creating temp class directory: ${f.getCanonicalPath}")
    f.delete()
    f.mkdir
    f.deleteOnExit()
    f
  }
}
