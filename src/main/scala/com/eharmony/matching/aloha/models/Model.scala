package com.eharmony.matching.aloha.models

import com.eharmony.matching.aloha.id.Identifiable
import com.eharmony.matching.aloha.score.conversions.ScoreConversion
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput

/** Models are identifiable and need to allow their scores to be converted to
  * com.eharmony.matching.aloha.score.Scores.Score instances.
  * @tparam A model input type
  * @tparam B model output type
  */
trait Model[-A, +B] extends (A => Option[B]) with Identifiable with ScoreConversion[B] {
    @inline final def apply(a: A) = scoreAsEither(a).right.toOption
    @inline final def scoreAsEither(a: A) = getScore(a)(false)._1
    @inline final def score(a: A) = getScore(a)(true)._2.get

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score])
}
