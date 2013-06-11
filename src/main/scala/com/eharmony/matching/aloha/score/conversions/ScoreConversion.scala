package com.eharmony.matching.aloha.score.conversions

import collection.JavaConversions.asJavaIterable
import com.eharmony.matching.aloha.id.Identifiable
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.Scores.Score.ModelId
import com.eharmony.matching.aloha.score.Scores.Score.ScoreError
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.Scores.Score.MissingRequiredFields

/** When mixing in with ScoreConversion, values of type A can be converted to a Score by calling toScore.  This works
  * requiring a score converter of type A to be passed to the implicit class that is contained inside the object mixed
  * in with ScoreConversion.
  * {{{
  * import com.eharmony.matching.aloha.io.out.ModelOutput._
  * import com.eharmony.matching.aloha.io.score.conversions.rich.RichScore
  * import com.eharmony.matching.aloha.io.score.Scores.Score
  * class ToIntModel extends Model[String, Int] { // Remember: with Identifiable with ScoreConversion[Int]
  *   private[aloha] def getScore(a: String)(implicit audit: Boolean) = {
  *     val s = try { ModelOutput(a.toInt) } catch { case e: NumberFormatException => Left(ModelOutput.fail(e.getMessage)) }
  *     val t0 = toScoreTuple(s) // <-- THIS IS THE CONVERSION THAT THIS TRAIT PROVIDES
  *     val t1 = toScoreTuple[Int](os)(audit, implicitly[ScoreConverter[Int]]) // <-- "UNDER THE HOOD"
  *     t0
  *   }
  * }
  *
  * // Proper typing:
  * val s: Score = new ToIntModel().score("123")
  * s.relaxed.asInt.foreach(_ match { case 123 => println("yay!") }) // yay
  * println(s.relaxed.asBoolean.map(_ => "yay").getOrElse("boo"))    // boo
  * }}}
  * @tparam A
  */
trait ScoreConversion[+A] { self: Identifiable =>

    /** Convert a model output to a Score object.
      * @param o the model output object.
      * @param c a type class that knows how to convert the output to the proper type of score.
      * @tparam B
      * @return a properly-typed Score object
      */
    @inline protected[this] def toScore[B >: A](o: ModelOutput[B])(implicit c: ScoreConverter[B]): Score = toScore(o, Iterable.empty[Score])


    /** Convert a model output to a Score object.
      * @param o the model output object.
      * @param missingFeatures iterable of string representations of missing feature.  If o is a
      *                        [[com.eharmony.matching.aloha.score.basic.ModelFailure]], then add the missing feature
      *                        to the error regardless of how many are missing (''INCLUDING'' 0 missing).  For a
      *                         [[com.eharmony.matching.aloha.score.basic.ModelSuccess]], only create an error and add
      *                         the missing fields if there ''IS AT LEAST 1 MISSING FEATURE.''
      * @param c a type class that knows how to convert the output to the proper type of score.
      * @tparam B
      * @return a properly-typed Score object
      */
    protected[this] def toScoreWithMissingFeatures[B >: A](o: ModelOutput[B], missingFeatures: Iterable[String])(implicit c: ScoreConverter[B]): Score = {
        val b = Score.newBuilder
        o.fold(
            e => b.setError(ScoreError.newBuilder.
                    addAllMessages(e._1).
                    setModel(ModelId.newBuilder.
                        setId(modelId.getId).
                        setName(modelId.getName)).
                    setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(missingFeatures))),
            s => {
                b.setScore(ScoreConverter.valueToScore(modelId, s)(c))
                if (missingFeatures.iterator.hasNext) {
                    b.setError(ScoreError.newBuilder.
                        setModel(ModelId.newBuilder.
                            setId(modelId.getId).
                            setName(modelId.getName)).
                        setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(missingFeatures)))
                }
            }
        )

        b.build
    }

    /** Convert a model output to a Score object and add subscores if applicable.
      * @param o the model output object.
      * @param subScores a series of zero or more submodel scores.
      * @param c a type class that knows how to convert the output to the proper type of score.
      * @tparam B
      * @return a properly-typed Score object
      */
    protected[this] def toScore[B >: A](o: ModelOutput[B], subScores: Iterable[Score])(implicit c: ScoreConverter[B]): Score = {
        val b = Score.newBuilder
        o.fold(
            // TODO: automatically add missing features (may necessitate adding missing features to failure type).
//            e => b.setError(ScoreError.newBuilder.addAllMessages(e).setModel(ModelId.newBuilder().setId(modelId.getId).setName(modelId.getName))),
            e => b.setError(ScoreError.newBuilder.addAllMessages(e._1).setModel(ModelId.newBuilder().setId(modelId.getId).setName(modelId.getName))),
            s => b.setScore(ScoreConverter.valueToScore(modelId, s)(c))
        )
        if (subScores.nonEmpty) b.addAllSubScores(subScores)
        b.build
    }

    /** Convert a model output to the type of object returned by the [[com.eharmony.matching.aloha.models.Model]] getScore
      * method.
      * @param o the model output object.
      * @param audit whether or not to include the score object in the tuple.
      * @param c a type class that knows how to convert the output to the proper type of score.
      * @tparam B
      * @return a properly-typed Score object
      */
    @inline protected[this] def toScoreTuple[B >: A](o: ModelOutput[B])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
        toScoreTuple(o, Iterable.empty)(audit, c)

    /** Convert a model output to the type of object returned by the [[com.eharmony.matching.aloha.models.Model]] getScore
      * method.
      * @param o the model output object.
      * @param subScores a series of zero or more submodel scores.
      * @param audit whether or not to include the score object in the tuple.
      * @param c a type class that knows how to convert the output to the proper type of score.
      * @tparam B
      * @return a properly-typed Score object
      */
    @inline protected[this] def toScoreTuple[B >: A](o: ModelOutput[B], subScores: Iterable[Score])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
        if (audit) (o, Option(toScore(o, subScores))) else (o, None)
}
