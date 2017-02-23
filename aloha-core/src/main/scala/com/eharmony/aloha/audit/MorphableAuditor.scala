package com.eharmony.aloha.audit

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 1/18/17.
  */
trait MorphableAuditor[U, -N, +B <: U] extends Auditor[U, N, B] {
  def changeType[M: RefInfo]: Option[MorphableAuditor[U, M, U]]
}
