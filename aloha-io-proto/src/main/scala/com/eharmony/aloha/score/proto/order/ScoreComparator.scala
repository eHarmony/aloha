package com.eharmony.aloha.score.proto.order

import java.util.Comparator
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.proto.order.Orderings.ScoreById

class ScoreComparator extends Comparator[Score] {
    def compare(s1: Score, s2: Score) = ScoreById.compare(s1, s2)
}
