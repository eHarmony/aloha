package com.eharmony.aloha.models.reg

import com.eharmony.aloha.models.reg.json.RegressionModelJson
import spray.json.pimpString
import com.eharmony.aloha.io.StringReadable
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._
import com.eharmony.aloha.util.{Logging, Timing}

@RunWith(classOf[BlockJUnit4ClassRunner])
class BigModelParseTest extends RegressionModelJson with Timing with Logging {

    /** The purpose of this test is that we can parse a fairly large model JSON to an abstract syntax tree of data used
      * to construct the regression model.  This JSON has
      *
      *  - 184,846 file lines
      *  - 94 features
      *  - 874 first order weights
      *  - 30,598 second order weights
      *  - 341 spline knots.
      *
      *  and comes from an actual model with the information stripped out.
      */
    @Test def testBigJsonParsedToAstForRegModel() {
        val ((s, data), t) = time(getBigZippedData("/com/eharmony/aloha/models/reg/semi_cleaned_big_model.json.gz"))
        assertTrue(s"Should take less than 10 seconds to parse, took $t", t < 10)

        assertEquals("file lines", 184846, scala.io.Source.fromString(s).getLines().size)
        assertEquals("Features", 94, data.features.size)
        assertEquals("First order weights", 874, data.weights.size)
        assertEquals("Higher order weights", 30598, data.higherOrderFeatures.map(_.size).getOrElse(0))
        assertEquals("spline size", 341, data.spline.map(_.knots.size).getOrElse(0))

        debug("file lines: 184846, features: 94, first order weights: 874, higher order weights: 30598, spline size: 341")
    }

    private[this] def getBigZippedData(resourcePath: String) = {
        val s = StringReadable.gz.fromResource(resourcePath)
        (s, s.parseJson.convertTo[RegData])
    }
}
