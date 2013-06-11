package com.eharmony.matching.aloha.score.conversions

import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.Scores.Score.{ModelId => MId}
import com.eharmony.matching.aloha.id.ModelId

package object rich {

    /** Provides extension methods to the protocol buffer based Score class.
      * @param s
      */
    implicit class RichScore(protected val s: Score) extends RichScoreLike

    /** Provides an extension method to the protocol buffer based ModelID class to allow conversion to a ModelIdentity.
      * @param id
      */
    implicit class RichModelId(protected val id: MId) {
        /** Transform the [[com.eharmony.matching.aloha.score.Scores.Score.ModelId]] to a
          * [[com.eharmony.matching.aloha.id.ModelId]].
          * @return
          */
        def toModelId = ModelId(id.getId, if (id.hasName) id.getName else "")
    }
}
