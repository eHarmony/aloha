package com.eharmony.aloha.factory.avro

import java.io.File

import com.eharmony.aloha.audit.impl.avro.Score
import com.eharmony.aloha.factory.ModelFactory
import org.apache.avro.generic.GenericRecord

import scala.util.Try

/**
  * Created by deak on 3/2/17.
  */
object StdAvroModelFactory {

  /**
    * Provides an easy interface for creating ModelFactory instances that both take and
    * return Avro objects.
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

    Try { throw new UnsupportedOperationException }
  }
}
