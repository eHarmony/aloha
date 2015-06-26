package com.eharmony.matching.aloha.score.conversions

import com.eharmony.matching.aloha.AlohaException
import IncorrectScoreTypeAccessException.getMessage
import com.eharmony.aloha.score.Scores.Score

case class IncorrectScoreTypeAccessException(s: Score, accessedType: String)
    extends AlohaException(getMessage(s, accessedType), null)

private[this] object IncorrectScoreTypeAccessException {
    def getMessage(s: Score, accessedType: String): String = "Tried to extract " + accessedType + " from score: " + s.getScore
}
