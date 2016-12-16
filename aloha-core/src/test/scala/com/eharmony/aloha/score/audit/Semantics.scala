package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo

case class Semantics[A](implicit val refInfo: RefInfo[A])
