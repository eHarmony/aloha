package com.eharmony.aloha.factory.avro

import java.io.File

import org.apache.commons.{vfs => vfs1, vfs2}
import com.eharmony.aloha.io.vfs.{Vfs1, Vfs2}
import com.eharmony.aloha.audit.impl.avro.Score
import com.eharmony.aloha.factory.ModelFactory
import org.apache.avro.generic.GenericRecord

import scala.util.Try



/**
  * Created by deak on 3/2/17.
  */
object StdAvroModelFactory {

  /**
    * Provides a standard way to create Avro ModelFactory instances for producing
    * models that take `GenericRecord`s and return `Score`s.
    * @param conf a factory configuration.
    *
    * @return a Try of a ModelFactory.
    */
  def fromConfig(conf: FactoryConfig): Try[ModelFactory[GenericRecord, Score]] = conf()

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
  @deprecated(message = "Prefer StdAvroModelFactory.fromConfig(conf: FactoryConfig)", since = "4.0.1")
  def apply(modelDomainSchemaVfsUrl: String,
            modelCodomainRefInfoStr: String,
            imports: Seq[String] = Nil,
            classCacheDir: Option[File] = None,
            dereferenceAsOptional: Boolean = true,
            useVfs2: Boolean = true): Try[ModelFactory[GenericRecord, Score]] = {

    val vfs = url(modelDomainSchemaVfsUrl, useVfs2)

    vfs.flatMap { u =>
      UrlConfig(
        u,
        modelCodomainRefInfoStr,
        imports,
        classCacheDir,
        dereferenceAsOptional
      )()
    }
  }

  private[this] def url(modelDomainSchemaVfsUrl: String, useVfs2: Boolean) = {
    val u =
      if (useVfs2)
        Try { Vfs2(vfs2.VFS.getManager.resolveFile(modelDomainSchemaVfsUrl)) }
      else Try { Vfs1(vfs1.VFS.getManager.resolveFile(modelDomainSchemaVfsUrl)) }
    FactoryConfig.wrapException(u)
  }
}
