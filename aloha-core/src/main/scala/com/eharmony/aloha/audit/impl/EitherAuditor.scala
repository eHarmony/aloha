package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

/**
  * An [[EitherAuditor]] encodes the idea of success of failure.  In the case of failure,
  * diagnostic information is encoded in the form of an [[EitherAuditorError]].  No
  * diagnostic information is emitted when a "''successful''" prediction is produced,
  * even if compromises were made in order to produce the prediction or problems like
  * missing data were encountered.
  *
  * If however, a model could not produce a prediction for some reason, diagnostic is
  * provided, including the model ID.
  *
  * Created by ryan.deak on 10/2/17.
  * @tparam A the type of value returned in the success case.
  */
sealed abstract class EitherAuditor[A]
extends MorphableAuditor[Either[EitherAuditorError, _], A, Either[EitherAuditorError, A]] {

  /**
    * Return an EitherAuditor for success type `M`.
    * @tparam M type of values returned on success
    * @return a new auditor. An EitherAuditor can be returned for any type `M`.
    */
  override def changeType[M: RefInfo]: Some[EitherAuditor[M]] = Some(changeTo[M])

  override def failure(
      modelId: ModelIdentity,
      errorMsgs: => Seq[String] = Nil,
      missingVarNames: => Set[String] = Set.empty,
      subvalues: Seq[Either[EitherAuditorError, _]] = Nil
  ): Either[EitherAuditorError, A] = {

    val (errors, missing) = errorAndMissing(errorMsgs, missingVarNames, subvalues)

//    val subErrs = subvalues.flatMap {
//      case Left(e)  => Option(e)
//      case Right(_) => None
//    }

//    Left(EitherAuditorError(modelId, errors, missing, subErrs))
    Left(EitherAuditorError(modelId, errors, missing))
  }

  override def success(
      modelId: ModelIdentity,
      valueToAudit: A,
      errorMsgs: => Seq[String] = Nil,
      missingVarNames: => Set[String] = Set.empty,
      subvalues: Seq[Either[EitherAuditorError, _]] = Nil,
      prob: => Option[Float] = None
  ): Either[EitherAuditorError, A] = Right(valueToAudit)


  protected[this] def changeTo[M]: EitherAuditor[M]

  protected[this] def errorAndMissing(
      errorMsgs: Seq[String],
      missingVarNames: Set[String],
      subvalues: Seq[Either[EitherAuditorError, _]]
  ): (Seq[String], Set[String])
}

object EitherAuditor {

  def apply[A]: EitherAuditor[A] = apply[A](aggregateErrsAndMissing = true)

  def apply[A](aggregateErrsAndMissing: Boolean): EitherAuditor[A] =
    if (aggregateErrsAndMissing)
      AggEitherAuditor[A]()
    else NoAggEitherAuditor[A]()

  private[this] final case class AggEitherAuditor[A]() extends EitherAuditor[A] {
    override protected[this] def changeTo[M]: EitherAuditor[M] = AggEitherAuditor[M]()
    override protected[this] def errorAndMissing(
        err: Seq[String],
        missing: Set[String],
        sub: Seq[Either[EitherAuditorError, _]]): (Seq[String], Set[String]) = {
      val e = sub.foldLeft(err)((s, sv) => s ++ sv.fold(e => e.errorMsgs, _ => Nil))
      val m = sub.foldLeft(missing)((s, sv) => s ++ sv.fold(e => e.missingVarNames, _ => Set.empty))
      (e, m)
    }
  }

  private[this] final case class NoAggEitherAuditor[A]() extends EitherAuditor[A] {
    override protected[this] def changeTo[M]: EitherAuditor[M] = NoAggEitherAuditor[M]()
    override protected[this] def errorAndMissing(
        err: Seq[String],
        missing: Set[String],
        sub: Seq[Either[EitherAuditorError, _]]): (Seq[String], Set[String]) =
      (err, missing)
  }
}

final case class EitherAuditorError(
    modelId: ModelIdentity,
    errorMsgs: Seq[String] = Nil,
    missingVarNames: Set[String] = Set.empty
    // ,
    // subvaluesWithFailures: Seq[EitherAuditorError] = Nil
)
