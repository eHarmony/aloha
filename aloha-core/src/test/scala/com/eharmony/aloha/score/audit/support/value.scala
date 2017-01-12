package com.eharmony.aloha.score.audit.support

sealed trait Value

case class IntValue(value: Int) extends Value
case class StringValue(value: String) extends Value
