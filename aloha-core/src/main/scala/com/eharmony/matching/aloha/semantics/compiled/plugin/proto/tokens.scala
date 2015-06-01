package com.eharmony.matching.aloha.semantics.compiled.plugin.proto

private[this] sealed trait Token
private[this] case class Field(name: String) extends Token
private[this] case class Index(index: Int) extends Token
