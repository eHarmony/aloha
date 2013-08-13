package com.eharmony.matching.aloha.models.tree.decision

import scala.collection.JavaConversions.asScalaBuffer

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import spray.json.{JsValue, pimpString}
import spray.json.DefaultJsonProtocol.DoubleJsonFormat

import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect._
import com.eharmony.matching.aloha.semantics.func.{GenFunc, GeneratedAccessor, GenAggFunc}
import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.conversions.RelaxedConversions

/** This test is designed to comprehensively cover all of the test cases in a decision stub (three nodes consisting
  * of a root and two leaves).  We ensure that we not only
  *
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class BasicDecisionTreeFullTest {

    // ===========================================================================================
    //  Both predicates have missing data ...
    // ===========================================================================================
    @Test def ffee() { missing(ffDt.score(ee), "first_feature") }
    @Test def ftee() { noneSatWithMissing(ftDt.score(ee), "first_feature", "second_feature") }
    @Test def tfee() { successWithMissing(tfDt.score(ee), 0, "first_feature") }
    @Test def ttee() { successWithMissing(ttDt.score(ee), 0, "first_feature", "second_feature") }

    // ===========================================================================================
    //  First predicate yields false result and the second contains missing data ...
    // ===========================================================================================
    @Test def fffe() { missing(ffDt.score(fe), "second_feature") }
    @Test def ftfe() { noneSatWithMissing(ftDt.score(fe), "second_feature") }
    @Test def tffe() { successWithMissing(tfDt.score(fe), 0, "second_feature") }
    @Test def ttfe() { successWithMissing(ttDt.score(fe), 0, "second_feature") }

    // ===========================================================================================
    //  First predicate yields true.  Short circuit and don't calculate second result ...
    // ===========================================================================================
    @Test def ffte() { success(ffDt.score(te), 1) }
    @Test def ftte() { success(ftDt.score(te), 1) }
    @Test def tfte() { success(tfDt.score(te), 1) }
    @Test def ttte() { success(ttDt.score(te), 1) }

    @Test def fftf() { success(ffDt.score(tf), 1) }
    @Test def fttf() { success(ftDt.score(tf), 1) }
    @Test def tftf() { success(tfDt.score(tf), 1) }
    @Test def tttf() { success(ttDt.score(tf), 1) }

    @Test def fftt() { success(ffDt.score(tt), 1) }
    @Test def fttt() { success(ftDt.score(tt), 1) }
    @Test def tftt() { success(tfDt.score(tt), 1) }
    @Test def tttt() { success(ttDt.score(tt), 1) }

    // ===========================================================================================
    //  First predicate has missing data and second predicate yields false.
    // ===========================================================================================
    @Test def ffef() { missing(ffDt.score(ef), "first_feature") }
    @Test def ftef() { noneSatWithMissing(ftDt.score(ef), "first_feature") }
    @Test def tfef() { successWithMissing(tfDt.score(ef), 0, "first_feature") }
    @Test def ttef() { successWithMissing(ttDt.score(ef), 0, "first_feature") }

    // ===========================================================================================
    //  First predicate has missing data and second predicate yields true.
    // ===========================================================================================
    @Test def ffet() { missing(ffDt.score(et), "first_feature") }
    @Test def ftet() { success(ftDt.score(et), 2) }
    @Test def tfet() { successWithMissing(tfDt.score(et), 0, "first_feature") }
    @Test def ttet() { success(ttDt.score(et), 2) }

    // ===========================================================================================
    //  Both predicates yields false.
    // ===========================================================================================
    @Test def ffff() { noneSat(ffDt.score(ff)) }
    @Test def ftff() { noneSat(ftDt.score(ff)) }
    @Test def tfff() { success(tfDt.score(ff), 0) }
    @Test def ttff() { success(ttDt.score(ff), 0) }

    // ===========================================================================================
    //  First predicate yields false, second yields true.
    // ===========================================================================================
    @Test def ffft() { success(ffDt.score(ft), 2) }
    @Test def ftft() { success(ftDt.score(ft), 2) }
    @Test def tfft() { success(tfDt.score(ft), 2) }
    @Test def ttft() { success(ttDt.score(ft), 2) }


    private[this] def success(s: Score, v: Int) {
        assertFalse(s"Score should not have any errors.  Found: ${s.getError}", s.hasError)
        assertTrue(s"Score should have a score.  No score found: $s", s.hasScore)
        assertEquals(Option(v.toDouble), RelaxedConversions.asDouble(s))
    }

    private[this] def successWithMissing(s: Score, v: Int, missing: String*){
        assertTrue(s"Score should have a score.  No score found: $s", s.hasScore)
        assertTrue(s"Score should have an error.  No error found: $s", s.hasError)
        assertEquals(Option(v.toDouble), RelaxedConversions.asDouble(s))
        assertEquals(s"No Error message expected. Found ${s.getError.getMessagesList.toSeq}", 0, s.getError.getMessagesCount)
        val m = s.getError.getMissingFeatures.getNamesList.toSeq
        assertEquals(missing, m)
    }

    private[this] def missing(s: Score, missing: String*) {
        assertFalse(s"Score should not have a score.  Found: ${s.getScore}", s.hasScore)
        assertTrue(s"Score should have an error.  No error found: $s", s.hasError)
        assertEquals(s"There should be one error message.  Found: ${s.getError.getMessagesList.toSeq}", 1, s.getError.getMessagesCount)
        assertEquals("Wrong error message found", missingMsg(missing.head), s.getError.getMessages(0))
        val m = s.getError.getMissingFeatures.getNamesList.toSeq
        assertEquals(missing, m)
    }

    private[this] def noneSat(s: Score) {
        assertFalse(s"Score should not have a score.  Found: ${s.getScore}", s.hasScore)
        assertTrue(s"Score should have an error.  No error found: $s", s.hasError)
        assertEquals(s"There should be one error message.  Found: ${s.getError.getMessagesList.toSeq}", 1, s.getError.getMessagesCount)
        assertEquals("Wrong error message found", noneSatMsg, s.getError.getMessages(0))
        assertEquals(s"Should not be any missing features. Found: ${s.getError.getMissingFeatures.getNamesList.toSeq}", 0, s.getError.getMissingFeatures.getNamesCount)
    }

    private[this] def noneSatWithMissing(s: Score, missing: String*){
        assertFalse(s"Score should not have a score.  Found: ${s.getScore}", s.hasScore)
        assertTrue(s"Score should have an error.  No error found: $s", s.hasError)
        assertEquals(s"There should be one error message.  Found: ${s.getError.getMessagesList.toSeq}", 1, s.getError.getMessagesCount)
        assertEquals("Wrong error message found", noneSatMsg, s.getError.getMessages(0))
        val m = s.getError.getMissingFeatures.getNamesList.toSeq
        assertEquals(missing, m)
    }

    private[this] def missingMsg(feature: String) =
        "Encountered unacceptable missing data in predicate: ${" + feature + "} >= 0"

    private[this] val noneSatMsg =
        "No decision tree predicate satisfied. Tried: " +
            "[GenAggFunc((${first_feature}) => ${first_feature} >= 0), " +
            "GenAggFunc((${second_feature}) => ${second_feature} >= 0)]"

    private[this] lazy val ffDt = getDecisionTree(getTreeJson(returnBest = false, missingDataOk = false))
    private[this] lazy val ftDt = getDecisionTree(getTreeJson(returnBest = false, missingDataOk = true))
    private[this] lazy val tfDt = getDecisionTree(getTreeJson(returnBest = true, missingDataOk = false))
    private[this] lazy val ttDt = getDecisionTree(getTreeJson(returnBest = true, missingDataOk = true))

    private[this] val ee = Map.empty[String, Double]
    private[this] val fe = Map[String, Double]("first_feature" -> -1)
    private[this] val te = Map[String, Double]("first_feature" -> 1)
    private[this] val ef = Map[String, Double]("second_feature" -> -1)
    private[this] val et = Map[String, Double]("second_feature" -> 1)
    private[this] val ff = Map[String, Double]("first_feature" -> -1, "second_feature" -> -1)
    private[this] val ft = Map[String, Double]("first_feature" -> -1, "second_feature" -> 1)
    private[this] val tf = Map[String, Double]("first_feature" -> 1, "second_feature" -> -1)
    private[this] val tt = Map[String, Double]("first_feature" -> 1, "second_feature" -> 1)

    /** This semantics operates on Map[String, Double].  Produces functions that return true when the key exists in
      * the map and the value associated value is non-negative.  The functions return false when the key exists in
      * the map but the values
      *
      */
    private[this] val semantics = new Semantics[Map[String, Double]] {
        def refInfoA = RefInfo[Map[String, Double]]
        def close() {}
        def accessorFunctionNames: Seq[String] = Nil
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
            val cs = codeSpec.trim
            val ga = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option("""(_: Map[String, Double]).get(cs)"""))
            val f = GenFunc.f1(ga)("${"+cs+"} >= 0", _ map {_ >= 0})
            val g = Right(f.asInstanceOf[GenAggFunc[Map[String, Double], B]])
            g
        }
    }

    private[this] val f = ModelFactory(BasicDecisionTree.parser)

    private[this] def getDecisionTree(json: JsValue) = f.getModel[Map[String, Double], Double](json, Option(semantics)).get

    private[this] def getTreeJson(returnBest: Boolean, missingDataOk: Boolean) = {
        val json =
            s"""
              |{
              |  "modelType": "DecisionTree",
              |  "modelId": {"id": 0, "name": ""},
              |  "returnBest": $returnBest,
              |  "missingDataOk": $missingDataOk,
              |  "nodes": [
              |    {
              |      "id": 2,
              |      "value": 0,
              |      "selector": {
              |        "selectorType": "linear",
              |        "children": [3, 4],
              |        "predicates": ["first_feature", "second_feature"]
              |      }
              |    },
              |    { "id": 3, "value": 1 },
              |    { "id": 4, "value": 2 }
              |  ]
              |}
            """.stripMargin.trim.asJson
        json
    }
}
