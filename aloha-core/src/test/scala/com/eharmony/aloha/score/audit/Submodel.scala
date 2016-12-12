package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

/**
  * Created by ryan on 12/12/16.
  */
trait Submodel[-A, I, +B] extends Model[A, B] {
  def auditor: Auditor[ModelId, I, B]
}
