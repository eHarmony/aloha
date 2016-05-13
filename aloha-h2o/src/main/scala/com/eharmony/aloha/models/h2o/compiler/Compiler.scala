package com.eharmony.aloha.models.h2o.compiler

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.util.Locale
import javax.tools.JavaCompiler.CompilationTask
import javax.tools.{DiagnosticCollector, JavaFileObject, ToolProvider}

import com.eharmony.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging
import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions.{asJavaIterable, collectionAsScalaIterable}
import scala.util.{Failure, Success, Try}


/**
 * A compiler that compiles and instantiates the code.
 * @param classDir If supplied, an attempt to create the directory but no attempt to delete the
 *                 directory will be made.
 * @param locale A Locale
 * @param charSet A CharSet
 * @param riB reflection info about the output type B.
 * @tparam B the output type.  This should be an interface or base class that the compiled class extends.
 */
private[h2o] class Compiler[B](classDir: Option[File] = None,
                               locale: Locale   = Locale.getDefault,
                               charSet: Charset = Charset.defaultCharset)
                              (implicit riB: RefInfo[B])
extends AlohaReadable[Try[B]]
   with ReadableByString[Try[B]] {

  /** Read from a String.
    * @param code a String to read.
    * @return the result
    */
  override def fromString(code: String): Try[B] = {
    val diagnosticCollector = new DiagnosticCollector[JavaFileObject]

    for {
      dirAndDelete         <- getCompileDir(classDir)
      compileDir           = dirAndDelete._1  // Avoids: `withFilter' method does not yet exist on
      delete               = dirAndDelete._2  //         Try[(File, Boolean)], using `filter' method instead
      _                    <- mkDir(compileDir)
      compilationUnit      <- getCompilationUnit(code)
      compileTask          <- getCompileTask(compilationUnit, compileDir, diagnosticCollector)
      diagnostics          <- compile(compileTask, diagnosticCollector)
      untypedInstance      <- instantiate(compilationUnit, compileDir)
      instance             <- cast(untypedInstance)
      _                    <- rmDir(compileDir, delete)
    } yield instance
  }

  def getCompileDir(dir: Option[File]) = dir.map(d => Try{(d, false)}) getOrElse Compiler.tmpDir.map(d => (d, true))

  def getCompileTask(compilationUnit: InMemoryJavaSource[B],
                     compileDir: File,
                     diagnosticCollector: DiagnosticCollector[JavaFileObject]) = Try {
    val locations = asJavaIterable(Seq("-d", compileDir.getCanonicalPath))
    val compiler = ToolProvider.getSystemJavaCompiler
    compiler.getTask(null, null, diagnosticCollector, locations, null, Iterable(compilationUnit))
  }

  def compile(compileTask: CompilationTask,
              diagnosticCollector: DiagnosticCollector[JavaFileObject]) =
    Try {
      compileTask.call()
    } transform (
      b => {
        val diagnostics = collectionAsScalaIterable(diagnosticCollector.getDiagnostics)
        if (b) Success(diagnostics)
        else   Failure(CompilationError(diagnostics))
      },
      f => Failure(f)
    )

  def instantiate(compilationUnit: InMemoryJavaSource[B], compileDir: File) = Try[Any] {
    val classLoader = new URLClassLoader(Array(compileDir.toURI.toURL))

    val clazz = classLoader.loadClass(compilationUnit.className)

    // Use the package from clazz to get the proper subdirectory.  Ends with '.' if
    // clazz is in a package.  Otherwise empty string.
    val pkg = Try { clazz.getPackage.getName } map { p => s"$p."} getOrElse ""

    // This potential dot at the end of pkg is OK because of the filterNot.
    val dir = pkg.split('.').filterNot(0 == _.length).foldLeft(compileDir)(new File(_, _))

    // Eagerly load all classes.  This is done because H2o can generate POJOs with
    // auxiliary classes outside the main GenModel POJO.  If there are auxiliary
    // non inner classes and the classes weren't loaded, the Model will compile and
    // instantiate but at prediction time, the model will emit a ClassNotFound error
    // when trying to access the auxiliary classes.

    // We assume that because all classes appear in the same generated h2o model file,
    // they have the same package which is the same package as the package in clazz.
    dir.listFiles().filter { _.getCanonicalPath.endsWith(".class") }.foreach { f =>
      // Drop the extension for a class file to form the class simple names.
      // Notice the '$' at the end.  That is important!
      val className = pkg + f.getName.replaceFirst("""\.class$""", "")
      classLoader.loadClass(className)
    }

    // Assume empty constructor for GenModel.
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

  def rmDir(f: File, delete: Boolean) = Try[Unit] {
    if(delete && f.exists() && f.isDirectory)
      FileUtils.deleteDirectory(f)
  }
}

private[h2o] object Compiler extends Logging {
  def tmpDir = Try[File] {
    val f = File.createTempFile("javacompiler", "classdir")
    debug(s"creating temp class directory: ${f.getCanonicalPath}")
    f.delete()
    f.mkdir
    f
  }
}
