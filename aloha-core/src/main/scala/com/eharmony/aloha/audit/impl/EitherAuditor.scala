package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.audit.impl.EitherAuditor.EitherAuditorError

/**
  * Created by ryan.deak on 10/2/17.
  */
final case class EitherAuditor[A](aggregateDiagnostics: Boolean = true)
extends MorphableAuditor[Either[EitherAuditorError, _], A, Either[EitherAuditorError, A]] {

  override def changeType[M: RefInfo]: Option[EitherAuditor[M]] =
    Some(EitherAuditor[M](aggregateDiagnostics))

  override def failure(
      modelId: ModelIdentity,
      errorMsgs: => Seq[String] = Nil,
      missingVarNames: => Set[String] = Set.empty,
      subvalues: Seq[Either[EitherAuditorError, _]] = Nil
  ): Either[EitherAuditorError, A] = {

    // TODO: Fill in the blanks.
    Left(EitherAuditorError())
  }

  override def success(
      modelId: ModelIdentity,
      valueToAudit: A,
      errorMsgs: => Seq[String] = Nil,
      missingVarNames: => Set[String] = Set.empty,
      subvalues: Seq[Either[EitherAuditorError, _]] = Nil,
      prob: => Option[Float] = None
  ): Either[EitherAuditorError, A] = Right(valueToAudit)
}

object EitherAuditor {
  case class EitherAuditorError()
}
