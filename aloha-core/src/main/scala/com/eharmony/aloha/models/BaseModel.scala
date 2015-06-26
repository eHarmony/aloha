package com.eharmony.aloha.models

import com.eharmony.aloha.score.conversions.ScoreConversion
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput

/** A ''Model'', in the context of a prediction task, should be thought of as a unit of work whose result should
  * be recorded.  To this end, models return a com.eharmony.aloha.score.Scores.Score defined in the
  * ''aloha-proto'' project.  This data type can actually contain a tree of typed scores, where each score contains a
  * prediction value and a model identifier.  Because of this, models need to be
  * [[com.eharmony.aloha.id.Identifiable]] and need to provide
  * [[com.eharmony.aloha.score.conversions.ScoreConversion]] from the model output type to the Score type.
  *
  * '''NOTE''': The ''getScore'' function below, which is the only abstract function that needs to be implemented in a
  * new model implementation, is package private to the '''com.eharmony.aloha''' package.  While the license
  * of this project is much more liberal than the GPL, this stipulation in the API may have a similar effect.
  *
  * @tparam A model input type
  * @tparam B model output type
  */
trait BaseModel[-A, +B]
extends Model[A, B]
   with ScoreConversion[B] {

    @inline final def apply(a: A) = scoreAsEither(a).right.toOption
    @inline final def scoreAsEither(a: A) = getScore(a)(audit = false)._1
    @inline final def score(a: A) = getScore(a)(audit = true)._2.get

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score])

   /**
    * A default implementation for the Closeable interface.
    */
    def close(): Unit = ()
}
