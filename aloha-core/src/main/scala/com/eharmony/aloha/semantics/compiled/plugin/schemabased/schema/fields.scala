package com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema

import com.eharmony.aloha.reflect.RefInfo

import scala.language.existentials

// RECORD, ENUM, ARRAY, MAP, UNION, FIXED, STRING, BYTES, INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL;

sealed trait FieldDesc {
  def name: String
  def index: Int
  def nullable: Boolean
}

// TODO: Add additional types as necessary.

case class RecordField(name: String, index: Int, schema: Schema, refInfo: RefInfo[_], nullable: Boolean) extends FieldDesc
case class EnumField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class ListField(name: String, index: Int, elementType: FieldDesc, nullable: Boolean) extends FieldDesc
case class StringField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class IntField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class LongField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class FloatField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class DoubleField(name: String, index: Int, nullable: Boolean) extends FieldDesc
case class BooleanField(name: String, index: Int, nullable: Boolean) extends FieldDesc
