package com.eharmony.matching.aloha.models

import java.io.Closeable

import com.eharmony.matching.aloha.id.{Identifiable, ModelIdentity}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput

/** A ''Model'', in the context of a prediction task, should be thought of as a unit of work whose result should
  * be recorded.  To this end, models return a '''com.eharmony.aloha.score.Scores.Score''' defined in the
  * ''aloha-proto'' project.  This data type can actually contain a tree of typed scores, where each score contains a
  * prediction value and a model identifier.  Because of this, models need to be
  * [[com.eharmony.matching.aloha.id.Identifiable]] and need to provide
  * [[com.eharmony.matching.aloha.score.conversions.ScoreConversion]] from the model output type to the Score type.
  *
  * '''NOTE''': The ''getScore'' function below, which is the only abstract function that needs to be implemented in a
  * new model implementation, is package private to the '''com.eharmony.matching.aloha''' package.  While the license
  * of this project is much more liberal than the GPL, this stipulation in the API may have a similar effect.
  *
  * '''NOTE''': ''These functions may throw exceptions.  While the aloha code based should not cause
  *               any exceptions to be thrown, it also does NOT actively attempt to catch exceptions
  *               thrown by those creating models or user-defined functions that are used inside of 
  *               feature specifications within the models.''
  *
  * '''NOTE''': ''Models may contain Closeable resources so they extends java.io.Closeable.  It is the caller's
  *               obligation to call the close method on the model.  For instance: ''
  * {{{
  * val model: Model[A, B] = getModel()
  * val examples: Seq[A] = getExamples()
  * val results = try { examples map { e => (e, model(e)) } }         finally { model.close }
  * }}}
  * @tparam A model input type
  * @tparam B model output type
  */
trait Model[-A, +B] extends Identifiable[ModelIdentity] with Closeable {

    /** Get a model prediction.
      * @param a An input to the model.
      * @return An option containing a value provided that the model prediction succeeds; otherwise None.
      */
    def apply(a: A): Option[B]

    /** Get a model prediction.  This provides slightly more information on failure on the left.  Successes will be
      * present on the right.  For information about the structure of ModelOutput, see
      * [[com.eharmony.matching.aloha.score.basic.ModelOutput]]
      * @param a An input to the model.
      * @return a ModelOutput
      */
    def scoreAsEither(a: A): ModelOutput[B]

    /** This is the most heavyweight very of the score.  It will return a Score protocol buffer with the score and
      * error information.  The benefit is that it can contain a trace of sub-scores along with errors and missing
      * features.
      * @param a An input to the model.
      * @return a full score object that may or may not contain score, sub-score and error information.
      */
    def score(a: A): Score

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score])
}
