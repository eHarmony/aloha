package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.{OptionAuditor, TreeAuditor}
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc, GeneratedAccessor}
import com.eharmony.aloha.util.rand.HashedCategoricalDistribution
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.{immutable => sci}

@RunWith(classOf[BlockJUnit4ClassRunner])
class CategoricalDistibutionModelTest extends ModelSerializationTestHelper {
    import CategoricalDistibutionModelTest._

    /** A factory that produces models that:
      *
      *   Map[String, Double]
      *
      * and return:
      *
      *   String
      */
    private lazy val f = NewModelFactory.defaultFactory(MapSemantics.stringMapSemantics[Double], TreeAuditor[String])
    private lazy val modelMissingNotOk = f.fromString(json()).get
    private lazy val modelMissingOk = f.fromString(json(missingOk = true)).get

    /**
      * Test missing feature: "key_one"
      */
    @Test def testMissingKeyOneWhenMissingNotOk() {
        val s = modelMissingNotOk(Map("key_two" -> 2, "5" -> 5))
        assertTrue("Should not have score.", s.value.isEmpty)
        assertTrue("Should have error.", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
        assertTrue("Should have missing features.", s.missingVarNames.nonEmpty)
        assertEquals("Should have missing features.", Set("key_one"), s.missingVarNames)
        assertEquals("Wrong number of error messages.", 1, s.errorMsgs.size)
        assertEquals("Bad error message.", "Couldn't choose random output due to missing features", s.errorMsgs.head)
    }

    @Test def testMissingKeyOneAndTwoWhenMissingNotOk() {
        val s = modelMissingNotOk(Map("5" -> 5))
        assertTrue("Should not have score.", s.value.isEmpty)
        assertTrue("Should have error.", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
        assertTrue("Should have missing features.", s.missingVarNames.nonEmpty)
        assertEquals("Should have missing features.", Seq("key_one", "key_two"), s.missingVarNames.toSeq.sorted)
        assertEquals("Wrong number of error messages.", 1, s.errorMsgs.size)
        assertEquals("Bad error message.", "Couldn't choose random output due to missing features", s.errorMsgs.head)
    }

    @Test def testMissingKeyOneWhenMissingOk() {
        val s = modelMissingOk(Map("key_two" -> 2, "5" -> 5))
        assertTrue("Should have score.", s.value.isDefined)
        assertEquals("Should have score.", "label_one", s.value.get)
        assertTrue("Should have error.", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
        assertTrue("Should have missing features.", s.missingVarNames.nonEmpty)
        assertEquals("Should have missing features.", Set("key_one"), s.missingVarNames)
        assertEquals("Wrong number of error messages.", 0, s.errorMsgs.size)
    }

    @Test def testSerialization(): Unit = {
        val m = CategoricalDistibutionModel(
            modelId = ModelId(2, "abc"),
            features = Seq(GenFunc.f0("", Identity[Any]())),
            distribution = HashedCategoricalDistribution(1,2,3,4),
            labels = sci.IndexedSeq("a", "b", "c", "d"),
            auditor =  OptionAuditor[String](),
            missingOk = true)

        val m1 = serializeDeserializeRoundTrip(m)
        assertEquals(m, m1)
    }
}

object CategoricalDistibutionModelTest {

    case class Identity[A]() extends (A => A) {
        def apply(a: A): A = a
    }

    class MapSemantics[K: RefInfo, V: RefInfo](f: String => K) extends Semantics[Map[K, V]] {
        def refInfoA: RefInfo[Map[K, V]] = RefInfoOps.wrap[K, V].in[Map]
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

    def json(missingOk: Boolean = false): String =
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
