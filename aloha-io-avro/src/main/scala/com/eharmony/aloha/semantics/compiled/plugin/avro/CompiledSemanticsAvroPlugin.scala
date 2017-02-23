package com.eharmony.aloha.semantics.compiled.plugin.avro

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemanticsPlugin
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.SchemaBasedSemanticsPlugin
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord


/**
  * Created by ryan
  */
case class CompiledSemanticsAvroPlugin[A <: GenericRecord](avroSchema: Schema,
                                                           dereferenceAsOptional: Boolean = true)
                                                          (implicit val refInfoA: RefInfo[A])
   extends CompiledSemanticsPlugin[A]
      with SchemaBasedSemanticsPlugin[A] {

  protected[this] def schema = AvroSchema(avroSchema)
  protected[this] def codeGenerators = AvroCodeGenerators
}
