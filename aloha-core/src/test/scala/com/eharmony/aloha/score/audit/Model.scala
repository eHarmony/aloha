package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity

// TODO: BIG DEAL!  Determine whether we want the self annotation.
// If we do include this, then all Models need to be AuditedModels.
// This seems a little draconian.  However, this is not included at the
// Java interface level, just in the Scala signature block in the interface.
trait Model[-A, +B] extends (A => B) { self: AuditedModel[A, _, B] =>
  def modelId: ModelIdentity
}
