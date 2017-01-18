package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 1/18/17.
  */
trait MorphableAuditor[U, N, B <: U] extends Auditor[U, N, B] {
    type OutputType[+X] <: U

    /**
      * Should this be done via implicit resolution?
      * {{{
      * def changeType[M](implicit auditor: Auditor[U, M, OutputType[M]]): Auditor[U, M, OutputType[M]] = auditor
      * }}}
      * @tparam M a new natural type.
      * @return
      */
    // TODO: Figure out if we should do this via implicit score resolution.
    def changeType[M: RefInfo]: Option[MorphableAuditor[U, M, OutputType[M]]]
}
