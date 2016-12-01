package com.eharmony.aloha.semantics.compiled.plugin.csv

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.DoubleJsonFormat
import com.eharmony.aloha.score.conversions.rich.RichScore

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[BlockJUnit4ClassRunner])
class DumThroAwayTest {
    @Test def test1() {
        val compiler = TwitterEvalCompiler()
        val plugin = CompiledSemanticsCsvPlugin(Map("profile.user_id" -> CsvTypes.withNameExtended("oi")))
        val imports = Seq("com.eharmony.aloha.feature.BasicFunctions._", "scala.math._")
        val semantics = CompiledSemantics(compiler, plugin, imports)
        val factory = ModelFactory.defaultFactory.toTypedFactory[CsvLine, Double](semantics)

        val model = factory.fromResource("fizzbuzz.json").get

        val lineProducer = CsvLines(Map("profile.user_id" -> 0))
        val examples = "" :: (-16 to 16 map { _.toString }).toList
        val lines = lineProducer(examples)

        val expected = Seq(
            (None, -1.0),
            (Some(-16), 16.0),
            (Some(-15), -6.0),
            (Some(-14), 14.0),
            (Some(-13), 13.0),
            (Some(-12), -2.0),
            (Some(-11), 11.0),
            (Some(-10), -4.0),
            (Some(-9), -2.0),
            (Some(-8), 8.0),
            (Some(-7), 7.0),
            (Some(-6), -2.0),
            (Some(-5), -4.0),
            (Some(-4), 4.0),
            (Some(-3), -2.0),
            (Some(-2), 2.0),
            (Some(-1), 1.0),
            (Some(0), -6.0),

            (Some(1), 1.0),
            (Some(2), 2.0),
            (Some(3), -2.0),
            (Some(4), 4.0),
            (Some(5), -4.0),
            (Some(6), -2.0),
            (Some(7), 7.0),
            (Some(8), 8.0),
            (Some(9), -2.0),
            (Some(10), -4.0),
            (Some(11), 11.0),
            (Some(12), -2.0),
            (Some(13), 13.0),
            (Some(14), 14.0),
            (Some(15), -6.0),
            (Some(16), 16.0)
        )

        val results = lines.map { l => (l.oi("profile.user_id"), model.score(l)) }.
                            map { case (optId, s) => (optId, s.relaxed.asDouble.get) }

        assertEquals(expected, results)
    }
}
