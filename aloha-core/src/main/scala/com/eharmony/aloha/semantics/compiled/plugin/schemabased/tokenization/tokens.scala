package com.eharmony.aloha.semantics.compiled.plugin.schemabased.tokenization

sealed trait Token
case class Field(name: String) extends Token
case class Index(index: Int) extends Token
