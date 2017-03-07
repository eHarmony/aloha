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

sealed abstract class FactoryConfig {
  /**
    * Attempt to create a factory.
    * @return
    */
  def apply(): Try[ModelFactory[GenericRecord, Score]]

  protected[this] def semantics(s: Schema,
                              imports: Seq[String],
                              classCacheDir: Option[File],
                              dereferenceAsOptional: Boolean): CompiledSemantics[GenericRecord] = {
    val p = CompiledSemanticsAvroPlugin[GenericRecord](s, dereferenceAsOptional)
    CompiledSemantics(TwitterEvalCompiler(classCacheDir = classCacheDir), p, imports)
  }

  protected[this] def auditor[A](refInfo: RefInfo[A]): Try[AvroScoreAuditor[A]] =
    AvroScoreAuditor(refInfo).map(Success.apply).getOrElse(
      Failure(new AlohaFactoryException(
        s"Couldn't create AvroScoreAuditor for ${RefInfoOps.toString(refInfo)}")))

  protected[this] def refInfo(refInfoStr: String): Try[RefInfo[Any]] =
    RefInfo.fromString(refInfoStr) match {
      case Left(err) => Failure(new AlohaFactoryException(err))
      case Right(success) => Success(success.asInstanceOf[RefInfo[Any]])
    }

  protected[this] def schemaInputStream(vfsUrl: String, useVfs2: Boolean): Try[InputStream] =
    Try {
      if (useVfs2)
        org.apache.commons.vfs2.VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream
      else org.apache.commons.vfs.VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream
    }

  protected[this] def schemaFromIs(is: InputStream): Try[Schema] =
    Try { new Schema.Parser().parse(is) }

  protected[this] def schema(vfsUrl: String, useVfs2: Boolean): Try[Schema] = {
    val isTry = schemaInputStream(vfsUrl, useVfs2)
    val inSchema = for {
      is <- isTry
      s <- schemaFromIs(is)
    } yield s
    isTry.foreach(IOUtils.closeQuietly)
    inSchema
  }
}

object FactoryConfig {

  /**
    * To enable the use of the magnet pattern, import `MagnetSyntax._`.
    * See Spray's [[http://spray.io/blog/2012-12-13-the-magnet-pattern/ Magnet pattern]]
    * article for more information.
    */
  object MagnetSyntax {
    implicit def fromSchemaCodomain(tuple: (Schema, String)): FactoryConfig = {
      val (s, cd) = tuple
      SchemaConfig(s, cd)
    }

    implicit def fromSchemaCodomainImports(tuple: (Schema, String, Seq[String])): FactoryConfig = {
      val (s, cd, imp) = tuple
      SchemaConfig(s, cd, imp)
    }

    implicit def fromUrlCodomain(tuple: (String, String)): FactoryConfig = {
      val (u, cd) = tuple
      UrlConfig(u, cd)
    }

    implicit def fromUrlCodomainImports(tuple: (String, String, Seq[String])): FactoryConfig = {
      val (u, cd, imp) = tuple
      UrlConfig(u, cd, imp)
    }
  }
}

/**
  * Provides an easy interface for creating ModelFactory instances that both take and
  * return Avro objects.
  *
  * This is especially useful for creating factories in generic services because the
  * `modelCodomainRefInfoStr` is a string rather than `RefInfo` so it can come from a
  * property file.
  *
  * @param schema an Avro Schema that represents the data passed to models created by this factory.
  * @param modelCodomainRefInfoStr A string representation of a `com.eharmony.aloha.reflect.RefInfo`.
  * @param imports imports to be injected into feature functions synthesized by the factory.
  * @param classCacheDir a cache directory on the local machine used to cache class files of
  *                      the created feature functions used in the models produced by the
  *                      factory.
  * @param dereferenceAsOptional whether to treat the dereferencing of repeated variables as
  *                              an optional type.  This avoids index out of bounds exceptions
  *                              and is safer but slightly slower.
  */
case class SchemaConfig(
    schema: Schema,
    modelCodomainRefInfoStr: String,
    imports: Seq[String] = Nil,
    classCacheDir: Option[File] = None,
    dereferenceAsOptional: Boolean = true
) extends FactoryConfig {

  /**
    * @return A Try of a ModelFactory that creates models taking `GenericRecord` instances as
    *         input and returns `com.eharmony.aloha.audit.impl.avro.Score` as output.
    */
  def apply(): Try[ModelFactory[GenericRecord, Score]] = {
    val f = for {
      ri <- refInfo(modelCodomainRefInfoStr)
      a <- auditor(ri)
      s = semantics(schema, imports, classCacheDir, dereferenceAsOptional)
      mf = ModelFactory.defaultFactory(s, a)(ri)
    } yield mf

    f match {
      case s@Success(_) => s
      case Failure(e) => Failure(new AlohaFactoryException("Problem creating Avro Factory.", e))
    }
  }
}

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
  */
case class UrlConfig(
    modelDomainSchemaVfsUrl: String,
    modelCodomainRefInfoStr: String,
    imports: Seq[String] = Nil,
    classCacheDir: Option[File] = None,
    dereferenceAsOptional: Boolean = true,
    useVfs2: Boolean = true
) extends FactoryConfig {

  /**
    * @return A Try of a ModelFactory that creates models taking `GenericRecord` instances as
    *         input and returns `com.eharmony.aloha.audit.impl.avro.Score` as output.
    */
  def apply(): Try[ModelFactory[GenericRecord, Score]] = {
    schema(modelDomainSchemaVfsUrl, useVfs2) flatMap (s =>
      SchemaConfig(s, modelCodomainRefInfoStr, imports, classCacheDir, dereferenceAsOptional)())
  }
}

