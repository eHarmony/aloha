package com.eharmony.matching.aloha.models.reg

import spray.json.pimpString
import com.eharmony.matching.aloha.io.StringReadable
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._
import com.eharmony.matching.aloha.util.Timing

@RunWith(classOf[BlockJUnit4ClassRunner])
class BigModelParseTest extends RegressionModelJson with Timing {

    @Test def test1() {
        val ((s, data), t) = time(getBigZippedData("/com/eharmony/matching/aloha/models/reg/semi_cleaned_big_model.json.gz"))
        assertTrue(s"Should take less than 5 seconds to parse, took $t", t < 5)

        assertEquals("file lines", 184846, io.Source.fromString(s).getLines().size)
        assertEquals("Features", 94, data.features.size)
        assertEquals("Higher order weights", 30598, data.higherOrderFeatures.map(_.size).getOrElse(0))
        assertEquals("First order weights", 874, data.weights.size)
        assertEquals("spline size", 342, data.spline.map(_.knots.size).getOrElse(0))

        println("file lines: 184846, features: 94, first order weights: 874, higher order weights: 30598, spline size: 342")
    }

    def getBigZippedData(resourcePath: String) = {
        val s = StringReadable.gz().fromResource(resourcePath)
        (s, s.asJson.convertTo[RegData])
    }
}
