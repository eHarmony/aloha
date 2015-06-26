package com.eharmony.aloha.semantics.compiled.plugin.csv

import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Test
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.factory.ModelFactory

import concurrent.ExecutionContext.Implicits.global
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter
import spray.json.DefaultJsonProtocol.DoubleJsonFormat

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

        val results = lines.map(l => (l.oi("profile.user_id"), model.score(l)))
        val a = 1

    }
}
