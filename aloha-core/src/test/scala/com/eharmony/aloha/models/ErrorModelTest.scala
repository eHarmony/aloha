package com.eharmony.aloha.models

import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.factory.ModelFactory
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ErrorModelTest {
    @Test def test1() {
        val em = ErrorModel(ModelId(), Seq("There should be a valid user ID.  Couldn't find one...", "blah blah"))
        val s: Score = em.score(null)
        assertNotNull(s)
    }

    @Test def testEmptyErrors() {
        import spray.json._, DefaultJsonProtocol._
        import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.ByteScoreConverter

        val json =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": { "id": 0, "name": "" }
              |}
            """.stripMargin.trim.parseJson

        val m1 = ModelFactory.defaultFactory.getModel[Unit, Byte](json)
        assertNotNull(m1)

        val json2 =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": { "id": 0, "name": "" },
              |  "errors": []
              |}
            """.stripMargin.trim.parseJson


        val m2 = ModelFactory.defaultFactory.getModel[Unit, Byte](json2)
        assertNotNull(m2)
    }
}
