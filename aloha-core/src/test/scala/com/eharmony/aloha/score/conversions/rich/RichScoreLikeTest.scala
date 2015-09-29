package com.eharmony.aloha.score.conversions.rich

import scala.language.implicitConversions

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.{ModelId => MId}
import com.eharmony.aloha.score.Scores.Score.{ScoreError, BaseScore, MissingRequiredFields}
import com.eharmony.aloha.score.conversions.ScoreConverter.valueToScore
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner


@RunWith(classOf[BlockJUnit4ClassRunner])
class RichScoreLikeTest {

  @Test def testJavaStyleCalling(): Unit = {
    val proto = score(valueToScore(ModelId(1, ""), 1))

    // in java:  RichScoreOps.toRichScore(proto).scoreJson().compactPrint()
    val act = RichScoreOps.toRichScore(proto).scoreJson.compactPrint
    assertEquals("""{"1":1}""", act)
  }


  @Test def testOne(): Unit = {
    val proto: Score = score(valueToScore(ModelId(1, ""), 1))
    assertEquals("""{"1":1}""", proto.scoreJson.compactPrint)
  }

  @Test def testTwo(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d)
    )
    assertEquals("""{"1":1,"2":2.0}""", proto.scoreJson.compactPrint)
  }

  @Test def testThree(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true)
    )
    assertEquals("""{"1":1,"2":2.0,"3":true}""", proto.scoreJson.compactPrint)
  }

  @Test def testFour(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true),
      valueToScore(ModelId(4, ""), "four")
    )
    assertEquals("""{"1":1,"2":2.0,"3":true,"4":"four"}""", proto.scoreJson.compactPrint)
  }

  @Test def testFive(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true),
      valueToScore(ModelId(4, ""), "four"),
      valueToScore(ModelId(5, ""), 5.5f)
    )
    assertEquals("""{"1":1,"2":2.0,"3":true,"4":"four","5":5.5}""", proto.scoreJson.compactPrint)
  }

  @Test def testScoresWithError1(): Unit = {
    val proto: Score = score(
      ScoreError.newBuilder.setModel(ModelId(1, "")).build,
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true),
      valueToScore(ModelId(4, ""), "four"),
      valueToScore(ModelId(5, ""), 5.5f)
    )
    assertEquals("""{"2":2.0,"3":true,"4":"four","5":5.5}""", proto.scoreJson.compactPrint)
  }

  @Test def testScoresWithError2(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      ScoreError.newBuilder.setModel(ModelId(2, "")).build,
      valueToScore(ModelId(3, ""), true),
      valueToScore(ModelId(4, ""), "four"),
      valueToScore(ModelId(5, ""), 5.5f)
    )
    assertEquals("""{"1":1,"3":true,"4":"four","5":5.5}""", proto.scoreJson.compactPrint)
  }

  @Test def testScoresWithError3(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      ScoreError.newBuilder.setModel(ModelId(3, "")).build,
      valueToScore(ModelId(4, ""), "four"),
      valueToScore(ModelId(5, ""), 5.5f)
    )
    assertEquals("""{"1":1,"2":2.0,"4":"four","5":5.5}""", proto.scoreJson.compactPrint)
  }

  @Test def testScoresWithError4(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true),
      ScoreError.newBuilder.setModel(ModelId(4, "")).build,
      valueToScore(ModelId(5, ""), 5.5f)
    )
    assertEquals("""{"1":1,"2":2.0,"3":true,"5":5.5}""", proto.scoreJson.compactPrint)
  }

  @Test def testScoresWithError5(): Unit = {
    val proto: Score = score(
      valueToScore(ModelId(1, ""), 1),
      valueToScore(ModelId(2, ""), 2d),
      valueToScore(ModelId(3, ""), true),
      valueToScore(ModelId(4, ""), "four"),
      ScoreError.newBuilder.setModel(ModelId(1, "")).build
    )
    assertEquals("""{"1":1,"2":2.0,"3":true,"4":"four"}""", proto.scoreJson.compactPrint)
  }

  @Test def testTwoErrors(): Unit = {
    val one =
      ScoreError.newBuilder.
        setModel(ModelId(1, "")).
        addMessages("error one").
        setMissingFeatures(
          MissingRequiredFields.newBuilder.addNames("feature.one").addNames("feature.two")
        ).build

    val two =
      ScoreError.newBuilder.
        setModel(ModelId(2, "")).
        addMessages("error two").addMessages("and error three").
        setMissingFeatures(
          MissingRequiredFields.newBuilder.addNames("feature.three")
        ).build

    val score = Score.newBuilder.setError(one).addSubScores(Score.newBuilder.setError(two)).build

    val expected =
      """
        |{
        |  "1": {
        |    "errors": ["error one"],
        |    "missingFeatures": ["feature.one", "feature.two"]
        |  },
        |  "2": {
        |    "errors": ["error two", "and error three"],
        |    "missingFeatures": ["feature.three"]
        |  }
        |}
      """.stripMargin

    assertEquals(expected.trim, score.errorJson.prettyPrint.trim)
  }

  /**
   * Convert a sequence of ProtoScoreOrError to a Score by embedding each element in the chain in the
   * subscore list of the preceeding element.
   * @param topScore top-level score in the score chain.
   * @param subscoreChain subscores appearing higher in the score chain appear earlier in subscoreChain.
   * @return
   */
  private[this] def score(topScore: ProtoScoreOrError, subscoreChain: ProtoScoreOrError*): Score = {
    val bottomUp = (topScore +: subscoreChain).reverse.toList
    bottomUp.tail.foldLeft(builder(bottomUp.head).build)((s, b) => builder(b).addSubScores(s).build)
  }

  private[this] def builder(p: ProtoScoreOrError) = p match {
    case ProtoScore(s) => Score.newBuilder.setScore(s)
    case ProtoError(e) => Score.newBuilder.setError(e)
  }

  private[this] sealed trait ProtoScoreOrError
  private[this] case class ProtoScore(score: BaseScore) extends ProtoScoreOrError
  private[this] case class ProtoError(error: ScoreError) extends ProtoScoreOrError

  private[this] implicit def error2ProtoError(error: ScoreError): ProtoError = ProtoError(error)
  private[this] implicit def baseScore2ProtoScore(score: BaseScore): ProtoScore = ProtoScore(score)
  private[this] implicit def modelId2ProtoModelId(mid: ModelId): MId =
    MId.newBuilder.setId(mid.id).setName(mid.name).build
}
