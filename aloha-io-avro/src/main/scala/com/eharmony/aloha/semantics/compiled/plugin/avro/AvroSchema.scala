package com.eharmony.aloha.semantics.compiled.plugin.avro

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.Schema.FieldRetrievalError
import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema._
import org.apache.avro
import org.apache.avro.Schema.Type._
import org.apache.avro.generic.GenericRecord

import scala.collection.JavaConversions.asScalaBuffer

/**
  * Created by ryan.
  */
case class AvroSchema(rootSchema: avro.Schema, schema: avro.Schema) extends Schema {
  private[this] type Result = Either[FieldRetrievalError, FieldDesc]

  override def field(name: String): Either[FieldRetrievalError, FieldDesc] = {
    Option(schema.getField(name)) map { f => extract(f.name, f.pos, f.schema, nullable = false) } getOrElse {
      Left(FieldRetrievalError(s"field '$name' not found in schema."))
    }
  }

  protected[avro] def extract(name: String, index: Int, fieldSchema: avro.Schema, nullable: Boolean): Result = {
    fieldSchema.getType match {
      case ARRAY   => arrayField(name, index, fieldSchema, nullable)
      case BOOLEAN => Right(BooleanField(name, index, nullable))
      case DOUBLE  => Right(DoubleField(name, index, nullable))
      case FLOAT   => Right(FloatField(name, index, nullable))
      case INT     => Right(IntField(name, index, nullable))
      case LONG    => Right(LongField(name, index, nullable))
      case RECORD  => recordField(name, index, fieldSchema, nullable)
      case STRING  => Right(StringField(name, index, nullable))
      case UNION   => unionField(name, index, fieldSchema, nullable)

      // CURRENTLY UNSUPPORTED

      case BYTES   => Left(FieldRetrievalError("BYTES fields not allowed"))
      case ENUM    => Left(FieldRetrievalError("ENUM fields not allowed"))
      case FIXED   => Left(FieldRetrievalError("FIXED fields not allowed"))
      case MAP     => Left(FieldRetrievalError("MAP fields not allowed"))
      case NULL    => Left(FieldRetrievalError("NULL fields not allowed outside of a UNION type field or as the sole type in a UNION."))
    }
  }

  protected[avro] def recordField(name: String, index: Int, fieldSchema: avro.Schema, nullable: Boolean): Result = {
    Right(RecordField(name, index, AvroSchema(rootSchema, fieldSchema), RefInfo[GenericRecord], nullable))
  }

  protected[avro] def arrayField(name: String, index: Int, fieldSchema: avro.Schema, nullable: Boolean): Result = {
    val elementType = extract("", 0, fieldSchema.getElementType, nullable = false)
    elementType.fold(
      e  => Left(FieldRetrievalError("ARRAY field creation failed.  Error getting element type: " + e.error)),
      el => Right(ListField(name, index, el, nullable))
    )
  }

  /**
    * Only unions two types of unions are supported:
    1. Unions of size 1 where the underlying type is supported in the `extract` function above (case 1).
    1. Unions of size 2 where one type is `null` and the other falls under ''case 1''.
    * @param fieldSchema a field schema used for the analysis.
    * @return
    */
  protected[avro] def unionField(name: String, index: Int, fieldSchema: avro.Schema, reqField: Boolean): Result = {
    val union = fieldSchema.getTypes

    // If there's only one item in the union, treat the union as if it didn't exist.
    if (1 == union.size)
      extract(name, index, union.head, reqField)
    else {
      val nonNull = union.filter(t => t.getType != NULL)
      if (1 == nonNull.size)
        extract(name, index, nonNull.head, nullable = true)
      else
        Left(FieldRetrievalError("Only UNION fields of one type or two types where one is NULL are allowed."))
    }
  }
}

object AvroSchema {
  def apply(rootSchema: avro.Schema): AvroSchema = new AvroSchema(rootSchema, rootSchema)
}
