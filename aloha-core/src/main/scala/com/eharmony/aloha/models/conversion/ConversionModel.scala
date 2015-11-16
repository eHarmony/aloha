package com.eharmony.aloha.models.conversion

import com.eharmony.aloha.models.{BaseModel, Model}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.score.conversions.ScoreConverter

trait ConversionModel[-A, B, C] extends BaseModel[A, C] {
    val modelId: ModelIdentity
    val submodel: Model[A, B]
    val conversion: B => C

    protected implicit def scoreConverter: ScoreConverter[C]

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean) = {
        val (mo, os) = submodel.getScore(a)
        val o = mo.fold({case (e, missing) => failure(e, missing, os)},
        x => {
            val y = conversion(x)
            success(score = y, subScores = os)
        })
        o
    }

    override def close() = submodel.close()
}
