package com.eharmony.matching.aloha.score.order

import com.eharmony.matching.aloha.score.Scores.Score.{BaseScore, ScoreError}
import com.eharmony.matching.aloha.score.Scores.Score

object Orderings {
    implicit object ScoreErrorById extends Ordering[ScoreError] {
        def compare(x: ScoreError, y: ScoreError) =
            if (x.getModel.getId < y.getModel.getId) -1 else 1
    }

    implicit object BaseScoreById extends Ordering[BaseScore] {
        def compare(x: BaseScore, y: BaseScore) =
            if (x.getModel.getId < y.getModel.getId) -1 else 1
    }

    implicit object ScoreById extends Ordering[Score] {
        private[this] val o = Ordering[Option[Long]]
        def compare(x: Score, y: Score) = o.compare(modelId(x), modelId(y))
        private[this] def modelId(s: Score) =
            if (s.hasError) Option(s.getError.getModel.getId)
            else if (s.hasScore) Option(s.getScore.getModel.getId)
            else None
    }
}
