package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json.{DeserializationException, pimpString}

@RunWith(classOf[BlockJUnit4ClassRunner])
class SegmentationModelTest extends ModelSerializationTestHelper {
    private[this] val PossibleLabels = Seq.range(0, 100).map("index " + _)
    private[this] lazy val Reader = {
        val sem = AnySemanticsWithoutFunctionCreation
        val aud = OptionAuditor[Double]()
        val f = ModelFactory.defaultFactory(sem, aud)

        SegmentationModel.Parser.commonJsonReader(f.submodelFactory, sem, OptionAuditor[String]()).get
//        SegmentationModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option(AnySemanticsWithoutFunctionCreation))
    }

    @Test def testSerialization(): Unit = {
        val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"), OptionAuditor[Double]())
        val m = SegmentationModel(ModelId(3, "jkl"), sub, Vector(1, 2), Vector("1", "2", "3"), OptionAuditor[String]())
        val m1 = serializeDeserializeRoundTrip(m)
        assertEquals(m, m1)
    }

    @Test def testSubmodelClosed(): Unit = {
        val sub = CloserTesterModel(ModelId(), OptionAuditor[Int]())
        SegmentationModel(ModelId.empty, sub, Vector(1, 2), Vector(1, 2, 3), OptionAuditor[Int]()).close()
        assertTrue(sub.isClosed)
    }

    @Test def testHappyPath_Byte()   { simpleTest(_.toByte) }
    @Test def testHappyPath_Short()  { simpleTest(_.toShort) }
    @Test def testHappyPath_Int()    { simpleTest(identity) }
    @Test def testHappyPath_Long()   { simpleTest(_.toLong) }
    @Test def testHappyPath_Float()  { simpleTest(_.toFloat) }
    @Test def testHappyPath_Double() { simpleTest(_.toDouble) }
    @Test def testHappyPath_String() { simpleTest("\"" + _ + "\"") }

    @Test def testBadType() {
        val t = "SOME_UNSUPPORTED_MODEL_TYPE"
        val json = getJson(t, Seq(1), 1)
        try {
            Reader read json
            fail()
        }
        catch {
            case e: DeserializationException => assertEquals(s"Unsupported sub-model output type: $t", e.getMessage)
        }
    }

    private[this] def simpleTest[A: Manifest](f: Int => A) {
        val xs = (1 to 5).map(f)
        val thresh = Seq(2, 4).map(f)
        val ys = Seq(0 -> 2, 1 -> 2, 2 -> 1)
        test(ys, xs, thresh)
    }

    private[this] def test[A: Manifest](expected: Seq[(Int, Int)], xs: Seq[A], cuts: Seq[A]) {
        assertEquals("number of examples should match the number of results", xs.size, expected.map(_._2).sum)
        val ys = expected.flatMap{ case(i, n) => Seq.fill(n)(s"index $i") }

        // Create a model and score the null value (since the model is input-invariant).  Check that it's the output
        // is as expected.
        (xs zip ys).foreach{ case(x, y) => assertEquals(s"For $x:", Option(y), model(x, cuts).apply(null)) }
    }

    private[this] def model[A: RefInfo](subModelValue: A, thresholds: Seq[A]) = {
        val subModelOutputType = RefInfoOps.toString[A].split("\\.").last
        val json = getJson(subModelOutputType, thresholds, subModelValue)
        val m = Reader.read(json)
        m
    }

    private[this] def getJson[A](subModelOutputType: String, thresh: Seq[A], subModelValue: A) = {
        val labels = PossibleLabels.take(thresh.size + 1).map("\"" + _ +"\"")

        val j = s"""
                   |{
                   |  "modelType": "Segmentation",
                   |  "modelId": { "id": 0, "name": "seg"},
                   |  "subModelOutputType": "$subModelOutputType",
                   |  "subModel": {
                   |    "modelType": "Constant",
                   |    "modelId": { "id": 1, "name": "const"},
                   |    "value": $subModelValue
                   |  },
                   |  "thresholds": ${thresh.mkString("[", ", ", "]")},
                   |  "labels": ${labels.mkString("[", ", ", "]")}
                   |}
                 """.stripMargin.trim

        val js = j.parseJson
        js
    }
}
