package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.audit.impl.TreeAuditor
import com.eharmony.aloha.audit.impl.TreeAuditor.Tree
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.reflect._
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc, GeneratedAccessor}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/** This test is designed to comprehensively cover all of the test cases in a decision stub (three nodes consisting
  * of a root and two leaves).  We ensure that we not only
  *
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class BasicDecisionTreeFullTest {

  // ===========================================================================================
  //  Both predicates have missing data ...
  // ===========================================================================================
  @Test def ffee() { missing(ffDt(ee), "first_feature") }
  @Test def ftee() { noneSatWithMissing(ftDt(ee), "first_feature", "second_feature") }
  @Test def tfee() { successWithMissing(tfDt(ee), 0, "first_feature") }
  @Test def ttee() { successWithMissing(ttDt(ee), 0, "first_feature", "second_feature") }

  // ===========================================================================================
  //  First predicate yields false result and the second contains missing data ...
  // ===========================================================================================
  @Test def fffe() { missing(ffDt(fe), "second_feature") }
  @Test def ftfe() { noneSatWithMissing(ftDt(fe), "second_feature") }
  @Test def tffe() { successWithMissing(tfDt(fe), 0, "second_feature") }
  @Test def ttfe() { successWithMissing(ttDt(fe), 0, "second_feature") }

  // ===========================================================================================
  //  First predicate yields true.  Short circuit and don't calculate second result ...
  // ===========================================================================================
  @Test def ffte() { success(ffDt(te), 1) }
  @Test def ftte() { success(ftDt(te), 1) }
  @Test def tfte() { success(tfDt(te), 1) }
  @Test def ttte() { success(ttDt(te), 1) }

  @Test def fftf() { success(ffDt(tf), 1) }
  @Test def fttf() { success(ftDt(tf), 1) }
  @Test def tftf() { success(tfDt(tf), 1) }
  @Test def tttf() { success(ttDt(tf), 1) }

  @Test def fftt() { success(ffDt(tt), 1) }
  @Test def fttt() { success(ftDt(tt), 1) }
  @Test def tftt() { success(tfDt(tt), 1) }
  @Test def tttt() { success(ttDt(tt), 1) }

  // ===========================================================================================
  //  First predicate has missing data and second predicate yields false.
  // ===========================================================================================
  @Test def ffef() { missing(ffDt(ef), "first_feature") }
  @Test def ftef() { noneSatWithMissing(ftDt(ef), "first_feature") }
  @Test def tfef() { successWithMissing(tfDt(ef), 0, "first_feature") }
  @Test def ttef() { successWithMissing(ttDt(ef), 0, "first_feature") }

  // ===========================================================================================
  //  First predicate has missing data and second predicate yields true.
  // ===========================================================================================
  @Test def ffet() { missing(ffDt(et), "first_feature") }
  @Test def ftet() { success(ftDt(et), 2) }
  @Test def tfet() { successWithMissing(tfDt(et), 0, "first_feature") }
  @Test def ttet() { success(ttDt(et), 2) }

  // ===========================================================================================
  //  Both predicates yields false.
  // ===========================================================================================
  @Test def ffff() { noneSat(ffDt(ff)) }
  @Test def ftff() { noneSat(ftDt(ff)) }
  @Test def tfff() { success(tfDt(ff), 0) }
  @Test def ttff() { success(ttDt(ff), 0) }

  // ===========================================================================================
  //  First predicate yields false, second yields true.
  // ===========================================================================================
  @Test def ffft() { success(ffDt(ft), 2) }
  @Test def ftft() { success(ftDt(ft), 2) }
  @Test def tfft() { success(tfDt(ft), 2) }
  @Test def ttft() { success(ttDt(ft), 2) }


  private[this] def success(s: Tree[Double], v: Int) {
    assertTrue(s"Score should not have any errors.  Found: ${s.errorMsgs}", s.errorMsgs.isEmpty)
    assertTrue(s"Score should have a score.  No score found: $s", s.value.isDefined)
    assertEquals(Option(v.toDouble), s.value)
  }

  private[this] def successWithMissing(s: Tree[Double], v: Int, missing: String*){
    assertTrue(s"Score should have a score.  No score found: $s", s.value.isDefined)
    assertTrue(s"Score should have an error.  No error found: $s", s.missingVarNames.nonEmpty)
    assertEquals(Option(v.toDouble), s.value)
    assertTrue(s"No Error message expected. Found ${s.errorMsgs}", s.errorMsgs.isEmpty)
    val m = s.missingVarNames
    assertEquals(missing.toSet, m)
  }

  private[this] def missing(s: Tree[Double], missing: String*) {
    assertTrue(s"Score should not have a score.  Found: ${s.value}", s.value.isEmpty)
    assertTrue(s"Score should have an error.  No error found: $s", s.missingVarNames.nonEmpty)
    assertEquals(s"There should be one error message.  Found: ${s.errorMsgs}", 1, s.errorMsgs.size)
    assertEquals("Wrong error message found", missingMsg(missing.head), s.errorMsgs.head)
    val m = s.missingVarNames
    assertEquals(missing.toSet, m)
  }

  private[this] def noneSat(s: Tree[Double]) {
    assertTrue(s"Score should not have a score.  Found: ${s.value}", s.value.isEmpty)
    assertTrue(s"Score should have an error.  No error found: $s", s.missingVarNames.nonEmpty || s.errorMsgs.nonEmpty)
    assertEquals(s"There should be one error message.  Found: ${s.errorMsgs}", 1, s.errorMsgs.size)
    assertEquals("Wrong error message found", noneSatMsg, s.errorMsgs.head)
    assertTrue(s"Should not be any missing features. Found: ${s.missingVarNames}", s.missingVarNames.isEmpty)
  }

  private[this] def noneSatWithMissing(s: Tree[Double], missing: String*){
    assertTrue(s"Score should not have a score.  Found: ${s.value}", s.value.isEmpty)
    assertTrue(s"Score should have an error.  No error found: $s", s.missingVarNames.nonEmpty)
    assertEquals(s"There should be one error message.  Found: ${s.errorMsgs}", 1, s.errorMsgs.size)
    assertEquals("Wrong error message found", noneSatMsg, s.errorMsgs.head)
    val m = s.missingVarNames
    assertEquals(missing.toSet, m)
  }

  private[this] def missingMsg(feature: String) =
    "Encountered unacceptable missing data in predicate: ${" + feature + "} >= 0"

  private[this] val noneSatMsg =
    "No decision tree predicate satisfied. Tried: " +
      "[GenAggFunc((${first_feature}) => ${first_feature} >= 0), " +
      "GenAggFunc((${second_feature}) => ${second_feature} >= 0)]"

  private[this] lazy val ffDt = getDecisionTree(getTreeJson(returnBest = false, missingDataOk = false)).get
  private[this] lazy val ftDt = getDecisionTree(getTreeJson(returnBest = false, missingDataOk = true)).get
  private[this] lazy val tfDt = getDecisionTree(getTreeJson(returnBest = true, missingDataOk = false)).get
  private[this] lazy val ttDt = getDecisionTree(getTreeJson(returnBest = true, missingDataOk = true)).get

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
    def refInfoA: RefInfo[Map[String, Double]] = RefInfo[Map[String, Double]]
    def close(): Unit = {}
    def accessorFunctionNames: Seq[String] = Nil
    def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
      val cs = codeSpec.trim
      val ga = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option("""(_: Map[String, Double]).get(cs)"""))
      val f = GenFunc.f1(ga)("${"+cs+"} >= 0", _ map {_ >= 0})
      val g = Right(f.asInstanceOf[GenAggFunc[Map[String, Double], B]])
      g
    }
  }

  private[this] val f = ModelFactory.defaultFactory(semantics, TreeAuditor[Double]())

  // getModel[Map[String, Double], Double](json, Option(semantics)).get
  private[this] def getDecisionTree(jsonStr: String) = f.fromString(jsonStr)

  private[this] def getTreeJson(returnBest: Boolean, missingDataOk: Boolean): String = {
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
            """.stripMargin.trim
    json
  }
}
