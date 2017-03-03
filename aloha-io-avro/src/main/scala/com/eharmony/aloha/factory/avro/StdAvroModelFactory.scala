package com.eharmony.aloha.factory.avro

import java.io.{File, InputStream}

import com.eharmony.aloha.audit.impl.avro.{AvroScoreAuditor, Score}
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.factory.ex.AlohaFactoryException
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.avro.CompiledSemanticsAvroPlugin
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.commons.io.IOUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by deak on 3/2/17.
  */
object StdAvroModelFactory {

  /**
    * Provides an easy interface for creating ModelFactory instances that both take and
    * return Avro objects.
    *
    * This is especially useful for creating factories in generic services because the
    * `modelCodomainRefInfoStr` is a string rather than `RefInfo` so it can come from a
    * property file.
    *
    * @param modelDomainSchemaVfsUrl an Apache VFS URL pointing to a JSON Avro Schema that
    *                                represents the data passed to models created by this factory.
    * @param modelCodomainRefInfoStr A string representation of a `com.eharmony.aloha.reflect.RefInfo`.
    * @param imports imports to be injected into feature functions synthesized by the factory.
    * @param classCacheDir a cache directory on the local machine used to cache class files of
    *                      the created feature functions used in the models produced by the
    *                      factory.
    * @param dereferenceAsOptional whether to treat the dereferencing of repeated variables as
    *                              an optional type.  This avoids index out of bounds exceptions
    *                              and is safer but slightly slower.
    * @param useVfs2 use Apache VFS2 to locate the domain schema (true) or use VFS1 (false.
    * @return A Try of a ModelFactory that creates models taking `GenericRecord` instances as
    *         input and returns `com.eharmony.aloha.audit.impl.avro.Score` as output.
    */
  def apply(modelDomainSchemaVfsUrl: String,
            modelCodomainRefInfoStr: String,
            imports: Seq[String] = Nil,
            classCacheDir: Option[File] = None,
            dereferenceAsOptional: Boolean = true,
            useVfs2: Boolean = true): Try[ModelFactory[GenericRecord, Score]] = {

    val f = for {
      s <- semantics(modelDomainSchemaVfsUrl, useVfs2, imports, classCacheDir, dereferenceAsOptional)
      ri <- refInfo(modelCodomainRefInfoStr)
      a <- auditor(ri)
      mf = ModelFactory.defaultFactory(s, a)(ri)
    } yield mf

    f match {
      case s@Success(_) => s
      case Failure(e) => Failure(new AlohaFactoryException("Problem creating Avro Factory.", e))
    }
  }

  private[this] def semantics(vfsUrl: String,
                              useVfs2: Boolean,
                              imports: Seq[String],
                              classCacheDir: Option[File],
                              dereferenceAsOptional: Boolean) = {
    for {
      s <- schema(vfsUrl, useVfs2)
      p = CompiledSemanticsAvroPlugin[GenericRecord](s, dereferenceAsOptional)
      cs = CompiledSemantics(TwitterEvalCompiler(classCacheDir = classCacheDir), p, imports)
    } yield cs
  }

  private[this] def auditor[A](refInfo: RefInfo[A]) =
    AvroScoreAuditor(refInfo).map(Success.apply).getOrElse(
      Failure(new AlohaFactoryException(
        s"Couldn't create AvroScoreAuditor for ${RefInfoOps.toString(refInfo)}")))


  private[this] def refInfo(refInfoStr: String) =
    RefInfo.fromString(refInfoStr) match {
      case Left(err) => Failure(new AlohaFactoryException(err))
      case Right(success) => Success(success.asInstanceOf[RefInfo[Any]])
    }

  private[this] def schemaInputStream(vfsUrl: String, useVfs2: Boolean) =
    Try {
      if (useVfs2)
        org.apache.commons.vfs2.VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream
      else org.apache.commons.vfs.VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream
    }

  private[this] def schemaFromIs(is: InputStream) =
    Try { new Schema.Parser().parse(is) }

  private[this] def schema(vfsUrl: String, useVfs2: Boolean) = {
    val isTry = schemaInputStream(vfsUrl, useVfs2)
    val inSchema = for {
      is <- isTry
      s <- schemaFromIs(is)
    } yield s
    isTry.foreach(IOUtils.closeQuietly)
    inSchema
  }

}
