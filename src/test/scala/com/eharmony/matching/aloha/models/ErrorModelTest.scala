package com.eharmony.matching.aloha.models

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.score.Scores.Score

@RunWith(classOf[JUnit4ClassRunner])
class ErrorModelTest {
    @Test def test1() {
        val em = ErrorModel(ModelId(), Seq("There should be a valid user ID.  Couldn't find one...", "blah blah"))
        val s: Score = em.score(null)
        println(s)
    }
}
