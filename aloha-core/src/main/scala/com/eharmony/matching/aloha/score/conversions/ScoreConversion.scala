package com.eharmony.matching.aloha.score.conversions

import collection.JavaConversions.{asJavaIterable, asScalaBuffer}
import com.eharmony.matching.aloha.id.{ModelIdentity, Identifiable}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.{ModelId, ScoreError, MissingRequiredFields}
import com.eharmony.matching.aloha.score.basic.ModelOutput

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
  * @tparam A model output type
  */
trait ScoreConversion[+A] { self: Identifiable[ModelIdentity] =>

    private[this] def mId = ModelId.newBuilder.setId(modelId.getId()).setName(modelId.getName()).build

    /** '''WARNING''': This should only be used in the event that success or failure couldn't be called explicitly.
      * This function is simpler and won't contain as much information as success or failure (subscores, etc).
      * Additionally, those functions have call-by-name inputs whereas this does not.  This function's implementation
      * is simply:
      *
      * {{{
      * mo.fold(f => failure(f._1, f._2, subScores),
      *         s => success(score = s, subScores = subScores))
      * }}}
      * @param mo a basic model output
      * @param audit whether to audit the value (i.e. produce a Score object).
      * @param c a score converter
      * @return a 2-tuple containing a model output and an optional score (score will exist if audit is true).
      */
    @inline protected[this] final def scoreTuple(mo: ModelOutput[A], subScores: Iterable[Score] = Iterable.empty)(implicit audit: Boolean, c: ScoreConverter[A]): (ModelOutput[A], Option[Score]) =
        mo.fold(f => failure(f._1, f._2, subScores),
                s => success(score = s, subScores = subScores))

    /** Produce a score representing a failure.
      * @param errors an ordered sequence of error messages.
      * @param missing features that would have been used to produce a score but are missing.  Note that this might not
      *                be a comprehensive list due to conditional execution.
      * @param subScores sub-scores to log if we are auditing.  Call-by-name.
      * @param audit whether to audit the value (i.e. produce a Score object).
      * @return a 2-tuple containing a model output and an optional score (score will exist if audit is true).
      */
    protected[this] final def failure(errors: Seq[String], missing: Iterable[String] = Iterable.empty, subScores: => Iterable[Score] = Iterable.empty)(implicit audit: Boolean): (ModelOutput[A], Option[Score]) = {
        val s: Option[Score] =
            if (!audit) None
            else {
                val b = Score.newBuilder
                if (subScores.nonEmpty) b.addAllSubScores(subScores)
                val e = ScoreError.newBuilder.setModel(mId).addAllMessages(errors)
                val m = accumulateMissing(missing, subScores)
                if (m.nonEmpty) e.setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(m))
                Option(b.setError(e).build)
            }

        (ModelOutput.fail(errors, missing), s)
    }

    /** Produce a score representing a success.
      * @param score a score
      * @param missing features that would have been used to produce a score but are missing.  Note that this might not
      *                be a comprehensive list due to conditional execution.  Call-by-name.
      * @param subScores sub-scores to log if we are auditing.  Call-by-name.
      * @param audit whether to audit the value (i.e. produce a Score object).
      * @param c a ScoreConverter object that converts score to a BaseScore object.
      * @return a 2-tuple containing a model output and an optional score (score will exist if audit is true).
      */
    protected[this] final def success(score: A, missing: => Iterable[String] = Iterable.empty, subScores: => Iterable[Score] = Iterable.empty)(implicit audit: Boolean, c: ScoreConverter[A]): (ModelOutput[A], Option[Score]) = {
        val s: Option[Score] =
            if (!audit) None
            else {
                val m = mId
                val b = Score.newBuilder
                if (subScores.nonEmpty) b.addAllSubScores(subScores)
                val mm = accumulateMissing(missing, subScores)
                if (mm.nonEmpty)
                    b.setError(ScoreError.newBuilder.
                        setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(mm)).
                        setModel(m))
                b.setScore(c.boxScore(score).setModel(m))
                Option(b.build)
            }

        (ModelOutput(score), s)
    }

    private [this] def accumulateMissing(missing: Iterable[String], subScores: Iterable[Score]) = {
        val subMissing = subScores.flatMap(_.getError.getMissingFeatures.getNamesList)
        val mm = missing ++ subMissing
        if (mm.nonEmpty) mm.toSeq.distinct else Nil
    }

//    protected[this] final def scoreTuple[B >: A](mo: ModelOutput[B], subScores: Iterable[Score], missing: Iterable[String])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) = {
//
//    }
//
//    /**
//      * @param ms
//      * @param subScores
//      * @param missing
//      * @param audit whether to audit
//      * @param c a score converter that can map the model output to a BaseScore proto.
//      * @tparam B
//      * @return
//      */
//    protected[this] final def scoreTuple[B >: A](ms: ModelSuccess[B], subScores: Iterable[Score], missing: Iterable[String])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) = {
//        val s: Option[Score] =
//            if (!audit) None
//            else {
//                val m = mId
//                val b = Score.newBuilder
//                if (subScores.nonEmpty) b.addAllSubScores(subScores)
//                if (missing.nonEmpty)
//                    b.setError(ScoreError.newBuilder.
//                        setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(missing)).
//                        setModel(m))
//                b.setScore(c.boxScore(ms.b).setModel(m))
//                Option(b.build)
//            }
//
//        (ms, s)
//    }
//
//    @inline protected[this] final def scoreTuple[B >: A](ms: ModelSuccess[B])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
//        scoreTuple(ms, Iterable.empty, Iterable.empty)
//
//    @inline protected[this] final def scoreTuple[B >: A](ms: ModelSuccess[B], subScores: Iterable[Score])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
//        scoreTuple(ms, subScores, Iterable.empty)
//
//    @inline protected[this] final def scoreTuple(mf: ModelFailure)(implicit audit: Boolean): (ModelFailure, Option[Score]) =
//        scoreTuple(mf: ModelFailure, Iterable.empty)
//
//    protected[this] final def scoreTuple(mf: ModelFailure, subScores: Iterable[Score])(implicit audit: Boolean): (ModelFailure, Option[Score]) = {
//        val s: Option[Score] =
//            if (!audit) None
//            else {
//                val m = mId
//                val b = Score.newBuilder
//                if (subScores.nonEmpty) b.addAllSubScores(subScores)
//                val e = ScoreError.newBuilder.setModel(m).addAllMessages(mf.a._1)
//                if (mf.a._2.nonEmpty) e.setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(mf.a._2))
//                Option(b.setError(e).build)
//            }
//
//        (mf, s)
//    }


    // ============================================================================================================
    // ============================================================================================================
    // ============================================================================================================


//    /** Convert a model output to a Score object.
//      * @param o the model output object.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    @inline protected[this] def toScore[B >: A](o: ModelOutput[B])(implicit c: ScoreConverter[B]): Score = toScore(o, Iterable.empty[Score])
//
//
//    /** Convert a model output to a Score object.
//      * @param o the model output object.
//      * @param missingFeatures iterable of string representations of missing feature.  If o is a
//      *                        [[com.eharmony.matching.aloha.score.basic.ModelFailure]], then add the missing feature
//      *                        to the error regardless of how many are missing (''INCLUDING'' 0 missing).  For a
//      *                         [[com.eharmony.matching.aloha.score.basic.ModelSuccess]], only create an error and add
//      *                         the missing fields if there ''IS AT LEAST 1 MISSING FEATURE.''
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    protected[this] def toScoreWithMissingFeatures[B >: A](o: ModelOutput[B], missingFeatures: Iterable[String])(implicit c: ScoreConverter[B]): Score = {
//        val b = Score.newBuilder
//        o.fold(
//            e => b.setError(ScoreError.newBuilder.
//                    addAllMessages(e._1).
//                    setModel(ModelId.newBuilder.
//                        setId(modelId.getId).
//                        setName(modelId.getName)).
//                    setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(missingFeatures))),
//            s => {
//                b.setScore(ScoreConverter.valueToScore(modelId, s)(c))
//                if (missingFeatures.iterator.hasNext) {
//                    b.setError(ScoreError.newBuilder.
//                        setModel(ModelId.newBuilder.
//                            setId(modelId.getId).
//                            setName(modelId.getName)).
//                        setMissingFeatures(MissingRequiredFields.newBuilder.addAllNames(missingFeatures)))
//                }
//            }
//        )
//
//        b.build
//    }
//
//    /** Convert a model output to a Score object and add subscores if applicable.
//      * @param o the model output object.
//      * @param subScores a series of zero or more submodel scores.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    protected[this] def toScore[B >: A](o: ModelOutput[B], subScores: Iterable[Score])(implicit c: ScoreConverter[B]): Score = {
//        val b = Score.newBuilder
//        o.fold(
//            // TODO: automatically add missing features (may necessitate adding missing features to failure type).
////            e => b.setError(ScoreError.newBuilder.addAllMessages(e).setModel(ModelId.newBuilder().setId(modelId.getId).setName(modelId.getName))),
//            e => b.setError(ScoreError.newBuilder.addAllMessages(e._1).setModel(ModelId.newBuilder().setId(modelId.getId).setName(modelId.getName))),
//            s => b.setScore(ScoreConverter.valueToScore(modelId, s)(c))
//        )
//        if (subScores.nonEmpty) b.addAllSubScores(subScores)
//        b.build
//    }
//
//    /** Convert a model output to a Score object and a single optional subscore if applicable.
//      * @param o the model output object.
//      * @param subScore a series of zero or more submodel scores.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    protected[this] def toScore[B >: A](o: ModelOutput[B], subScore: Option[Score])(implicit c: ScoreConverter[B]): Score =
//        toScore(o, subScore.toIterable)
//
//    /** Convert a model output to the type of object returned by the [[com.eharmony.matching.aloha.models.Model]] getScore
//      * method.
//      * @param o the model output object.
//      * @param audit whether or not to include the score object in the tuple.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    @inline protected[this] def toScoreTuple[B >: A](o: ModelOutput[B])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
//        toScoreTuple(o, Iterable.empty)(audit, c)
//
//    /** Convert a model output to the type of object returned by the [[com.eharmony.matching.aloha.models.Model]] getScore
//      * method.
//      * @param o the model output object.
//      * @param subScores a series of zero or more submodel scores.
//      * @param audit whether or not to include the score object in the tuple.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    @inline protected[this] def toScoreTuple[B >: A](o: ModelOutput[B], subScores: Iterable[Score])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
//        if (audit) (o, Option(toScore(o, subScores))) else (o, None)
//
//    /** Convert a model output to the type of object returned by the [[com.eharmony.matching.aloha.models.Model]] getScore
//      * method.
//      * @param o the model output object.
//      * @param subScore a single optional submodel score.
//      * @param audit whether or not to include the score object in the tuple.
//      * @param c a type class that knows how to convert the output to the proper type of score.
//      * @tparam B
//      * @return a properly-typed Score object
//      */
//    @inline protected[this] def toScoreTuple[B >: A](o: ModelOutput[B], subScore: Option[Score])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score]) =
//        toScoreTuple(o, subScore.toIterable)


    // scoreTuple[B >: A](o: ModelOutput[B])(implicit audit: Boolean, c: ScoreConverter[B]): (ModelOutput[B], Option[Score])
    // scoreTuple(o:ModelOutput[B], subscore: Option[Score])
    // scoreTuple(o:ModelOutput[B], subscores: Iterable[Score])
    // scoreTupleWithMissing(o:ModelOutput[B], subscore: Option[Score], missing: Iterable[String])
    // scoreTupleWithMissing(o:ModelOutput[B], subscores: Iterable[Score], missing: Iterable[String])
    // scoreTupleWithMissing(o:ModelOutput[B], missing: Iterable[String])
}
