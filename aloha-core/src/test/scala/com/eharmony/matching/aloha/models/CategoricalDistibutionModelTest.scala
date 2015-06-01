package com.eharmony.matching.aloha.models

import scala.collection.JavaConversions.asScalaBuffer

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import spray.json.DefaultJsonProtocol.StringJsonFormat

import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.{RefInfoOps, RefInfo}
import com.eharmony.matching.aloha.semantics.func.{GenAggFunc, GeneratedAccessor, GenFunc}
import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.StringScoreConverter
import com.eharmony.matching.aloha.score.conversions.rich.RichScore

@RunWith(classOf[BlockJUnit4ClassRunner])
class CategoricalDistibutionModelTest {
    import CategoricalDistibutionModelTest._

    /** A factory that produces models that:
      *
      *   Map[String, Double]
      *
      * and return:
      *
      *   String
      */
    lazy val f = ModelFactory(CategoricalDistibutionModel.parser).toTypedFactory[Map[String, Double], String](MapSemantics.stringMapSemantics[Double])
    lazy val modelMissingNotOk = f.fromString(json(missingOk = false)).get
    lazy val modelMissingOk = f.fromString(json(missingOk = true)).get

    /**
      * Test missing feature: "key_one"
      */
    @Test def testMissingKeyOneWhenMissingNotOk() {
        val s = modelMissingNotOk.score(Map("key_two" -> 2, "5" -> 5))
        assertFalse("Should not have score.", s.hasScore)
        assertTrue("Should have error.", s.hasError)
        assertTrue("Should have missing features.", s.getError.hasMissingFeatures)
        assertEquals("Should have missing features.", Seq("key_one"), s.getError.getMissingFeatures.getNamesList.toSeq)
        assertEquals("Wrong number of error messages.", 1, s.getError.getMessagesCount)
        assertEquals("Bad error message.", "Couldn't choose random output due to missing features", s.getError.getMessages(0))
    }

    @Test def testMissingKeyOneAndTwoWhenMissingNotOk() {
        val s = modelMissingNotOk.score(Map("5" -> 5))
        assertFalse("Should not have score.", s.hasScore)
        assertTrue("Should have error.", s.hasError)
        assertTrue("Should have missing features.", s.getError.hasMissingFeatures)
        assertEquals("Should have missing features.", Seq("key_one", "key_two"), s.getError.getMissingFeatures.getNamesList.toSeq.sorted)
        assertEquals("Wrong number of error messages.", 1, s.getError.getMessagesCount)
        assertEquals("Bad error message.", "Couldn't choose random output due to missing features", s.getError.getMessages(0))
    }

    @Test def testMissingKeyOneWhenMissingOk() {
        val s = modelMissingOk.score(Map("key_two" -> 2, "5" -> 5))
        assertTrue("Should have score.", s.hasScore)
        assertEquals("Should have score.", "label_one", s.strict.asString.get.right.get)
        assertTrue("Should have error.", s.hasError)
        assertTrue("Should have missing features.", s.getError.hasMissingFeatures)
        assertEquals("Should have missing features.", Seq("key_one"), s.getError.getMissingFeatures.getNamesList.toSeq)
        assertEquals("Wrong number of error messages.", 0, s.getError.getMessagesCount)
    }
}

object CategoricalDistibutionModelTest {

    class MapSemantics[K: RefInfo, V: RefInfo](f: String => K) extends Semantics[Map[K, V]] {
        def refInfoA = RefInfoOps.wrap[K, V].in[Map]
        def accessorFunctionNames = Nil
        def close() {}
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): Either[Seq[String], GenAggFunc[Map[K, V], B]] = {
            RefInfo[B] match {
                case v if v == RefInfo[V] =>
                    val ga = GeneratedAccessor(codeSpec, (_:Map[K, V]).apply(f(codeSpec)), None)
                    val g = GenFunc.f1(ga)(codeSpec, identity)
                    Right(g.asInstanceOf[GenAggFunc[Map[K, V], B]])
                case o if o == RefInfoOps.option[V] =>
                    val ga = GeneratedAccessor(codeSpec, (_:Map[K, V]).get(f(codeSpec)), None)
                    val g = GenFunc.f1(ga)(codeSpec, identity)
                    Right(g.asInstanceOf[GenAggFunc[Map[K, V], B]])
                case _ =>
                    // TODO: Log or something
                    val ga = GeneratedAccessor(codeSpec, (_:Map[K, V]).get(f(codeSpec)), None)
                    val g = GenFunc.f1(ga)(codeSpec, identity)
                    Right(g.asInstanceOf[GenAggFunc[Map[K, V], B]])
            }
        }
    }

    object MapSemantics {
        def stringMapSemantics[V: RefInfo] = new MapSemantics[String, V](identity)
    }

    def json(missingOk: Boolean = false) =
        s"""
          |{
          |  "modelType": "CategoricalDistribution",
          |  "modelId": {"id": 0, "name": ""},
          |  "features": [ "key_one", "key_two", "5" ],
          |  "probabilities": [ 0.25, 0.75 ],
          |  "labels": [ "label_one", "label_two" ],
          |  "missingOk": $missingOk
          |}
        """.stripMargin.trim
}
