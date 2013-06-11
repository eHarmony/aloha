package com.eharmony.matching.notaloha

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import com.eharmony.matching.aloha.models.ConstantModel
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter

@RunWith(classOf[JUnit4ClassRunner])
class NotAlohaScalaTest {
    @Test def test1() {
        val m = ConstantModel(1)
    }
}
