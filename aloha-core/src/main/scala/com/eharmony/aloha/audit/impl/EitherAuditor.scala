package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

import scala.collection.breakOut

/**
  * An [[EitherAuditor]] encodes the idea of success or failure.  In the case of failure,
  * (meaning a model could not produce a prediction) diagnostic information is encoded
  * in the form of an [[EitherAuditorError]].  No diagnostic information is emitted when
  * a "''successful''" prediction is produced, even if compromises were made in order to
  * produce the prediction or problems like missing data were encountered.
  *
  * To construct an [[EitherAuditor]], use the `apply` factory methods in the companion
  * object.
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
    val (errors, missing, failIds) = aggregateDiagnostics(errorMsgs, missingVarNames, subvalues)
    Left(EitherAuditorError(::(modelId, failIds), errors, missing))
  }

  override def success(
      modelId: ModelIdentity,
      valueToAudit: A,
      errorMsgs: => Seq[String] = Nil,
      missingVarNames: => Set[String] = Set.empty,
      subvalues: Seq[Either[EitherAuditorError, _]] = Nil,
      prob: => Option[Float] = None
  ): Either[EitherAuditorError, A] = Right(valueToAudit)

  /**
    * Construct a new EitherAuditor[M].  Notice this allows succeeds.
    * @tparam M the type of success value in the new auditor.
    * @return
    */
  protected[this] def changeTo[M]: EitherAuditor[M]

  /**
    * (''Possibly'') aggregate the information from `subvalues` into the error messages, missing
    * variable names and model IDs with problems
    * @param errorMsgs error messages from the current model failure.
    * @param missingVarNames missing variable names from the current model failure.
    * @param subvalues diagnostic information from submodel successes / failures.
    * @return (''possibly'') aggregated errors, missing variable info, and IDs of submodels that
    *         couldn't produce predictions.
    */
  protected[this] def aggregateDiagnostics(
      errorMsgs: Seq[String],
      missingVarNames: Set[String],
      subvalues: Seq[Either[EitherAuditorError, _]]
  ): (Seq[String], Set[String], List[ModelIdentity])
}

object EitherAuditor {

  /**
    * Produce an [[EitherAuditor]] that aggregates diagnostic information.
    * @tparam A the type of value returned in the success case.
    * @return
    */
  def apply[A]: EitherAuditor[A] = apply[A](aggregateDiagnostics = true)

  /**
    * Produce an [[EitherAuditor]].
    * @param aggregateDiagnostics  If `true`, aggregate diagnostics; otherwise,
    *                              disregard submodel information in the
    *                              [[EitherAuditorError]] returned by the auditor.
    * @tparam A the type of value returned in the success case.
    * @return
    */
  def apply[A](aggregateDiagnostics: Boolean): EitherAuditor[A] =
    if (aggregateDiagnostics)
      AggEitherAuditor[A]()
    else NoAggEitherAuditor[A]()

  private[this] final case class AggEitherAuditor[A]() extends EitherAuditor[A] {
    override protected[this] def changeTo[M]: EitherAuditor[M] = AggEitherAuditor[M]()
    override protected[this] def aggregateDiagnostics(
        err: Seq[String],
        missing: Set[String],
        sub: Seq[Either[EitherAuditorError, _]]): (Seq[String], Set[String], List[ModelIdentity]) = {
      val e = sub.foldLeft(err)((s, sv) => s ++ sv.fold(e => e.errorMsgs, _ => Nil))
      val m = sub.foldLeft(missing)((s, sv) => s ++ sv.fold(e => e.missingVarNames, _ => Set.empty))

      val fIds: List[ModelIdentity] = sub.flatMap {
        case Left(f)  => f.failureModelIds
        case Right(_) => Nil
      }(breakOut)

      (e, m, fIds)
    }
  }

  private[this] final case class NoAggEitherAuditor[A]() extends EitherAuditor[A] {
    override protected[this] def changeTo[M]: EitherAuditor[M] = NoAggEitherAuditor[M]()
    override protected[this] def aggregateDiagnostics(
        err: Seq[String],
        missing: Set[String],
        sub: Seq[Either[EitherAuditorError, _]]): (Seq[String], Set[String], List[ModelIdentity]) =
      (err, missing, Nil)
  }
}

/**
  * Diagnostic information about failures encountered.  This structure can be thought
  * of as a flattened tree of failures.  The tree is pruned one of two ways, it is
  * either prune at the root, or pruned at the the first submodel successes
  * encountered.  The tree is pruned at the root if the auditor is created via
  * `EitherAuditor[A](aggregateDiagnostics = false)`.
  *
  * If the auditor is created via `EitherAuditor[A](aggregateDiagnostics = true)`,
  * or simply `EitherAuditor[A]`, then the trees are pruned at successes.  For
  * instance, imagine a model, model `1`, that has submodels `2` and `5`.
  * Submodel `2` has submodels `3` and `4`; submodel `5` has submodels `6` and `7`.
  * Let's say the following submodels fail: 1, 2, 4, 6, 7.  Then, information would
  * be aggregated as follows:
  *
  * {{{
  * //          ORIGINAL        |       PRUNED      |       SIMPLIFIED
  * //  =====================================================================
  * //                          |                   |
  * //   1:F +-- 2:F +--  3:S   |   1 +-- 2 +-- /   |   1 --- 2 +
  * //       |       |          |     |     |       |           |
  * //       |       +--  4:F   |     |     +-- 4   |           +--  4
  * //       |                  |     |             |
  * //       +-- 5:S +--  6:F   |     +-- / +-- /   |
  * //               |          |           |       |
  * //               +--  7:F   |           +-- /   |  DFS preorder: 1, 2, 4
  * //                          |                   |
  * //  =====================================================================
  * }}}
  *
  * Notice because submodel `5` succeeds, error information about submodels
  * `6` and `7` is disregarded because submodel `5` could recover from its
  * submodels' errors.
  *
  * So, in the event that diagnostics are aggregated, only information about
  * submodels `1`, `2`, `4` should be contained in the [[EitherAuditorError]]
  * returned by the [[EitherAuditor]].
  *
  * @param failureModelIds a non-empty list of [[ModelIdentity]] instances of submodels
  *                        that failed to produce a prediction.  Since the top-level
  *                        model must fail in order for an [[EitherAuditorError]] to be
  *                        returned, this is a '''non-empty''' list.  This '''non-empty'''
  *                        list is in
  *                        [[https://en.wikipedia.org/wiki/Tree_traversal#Pre-order DFS preorder]].
  * @param errorMsgs messages indicating errors encountered.
  * @param missingVarNames names of variables with missing data encountered in the
  *                        model computation.
  */
final case class EitherAuditorError(
    failureModelIds: ::[ModelIdentity],
    errorMsgs: Seq[String],
    missingVarNames: Set[String]
)
