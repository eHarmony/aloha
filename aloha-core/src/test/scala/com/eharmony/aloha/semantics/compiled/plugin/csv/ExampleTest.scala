package com.eharmony.aloha.semantics.compiled.plugin.csv


import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.models.reg.RegressionModel

@RunWith(classOf[BlockJUnit4ClassRunner])
class ExampleTest {
    import ExampleTest._

    def getModel() = {
        import scala.concurrent.ExecutionContext.Implicits.global
        import spray.json.DefaultJsonProtocol.DoubleJsonFormat
        import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter

        val compiler = TwitterEvalCompiler()
        val plugin = CompiledSemanticsCsvPlugin(columnTypes)
        val semantics = CompiledSemantics(compiler, plugin, Seq("com.eharmony.aloha.feature.BasicFunctions._"))
        val factory = ModelFactory(RegressionModel.parser).toTypedFactory[CsvLine, Double](semantics)
        val url = getClass.getClassLoader.getResource("3001.json")
        val model = factory.fromUrl(url).get
        model
    }

    @Test def test1() {
        val model = getModel()
        samples.zip(results).zipWithIndex.foreach { case ((x, yExp), i) => {
            val y = model(x)
            assertTrue(s"for test $i: score should exists. ", None != y)
            assertEquals(s"test $i: ", yExp, y.get, 1.0e-9)
        }}
    }
}

object ExampleTest {

    private[this] val orderedColumnTypes = Seq(
        "candidate.profile.photos" -> "vs",
        "candidate.calculated_values.days_since_last_login" -> "oi",
        "candidate.model_features.compatibility.attractiveness.attractiveness" -> "oi",
        "candidate.profile.education" -> "oi",
        "candidate.profile.gender" -> "oe",
        "candidate.profile.income" -> "oi",
        "candidate.profile.user_status" -> "oe",
        "female.model_features.compatibility.attractiveness.attractiveness" -> "oi",
        "female.profile.birthdate" -> "ol",
        "female.profile.height_in_MM" -> "oi",
        "male.calculated_values.days_since_epoch" -> "oi",
        "male.model_features.compatibility.attractiveness.attractiveness" -> "oi",
        "male.profile.birthdate" -> "ol",
        "male.profile.height_in_MM" -> "oi",
        "pairing.distance" -> "oi",
        "user.calculated_values.days_since_last_login" -> "oi",
        "user.profile.education" -> "oi",
        "user.profile.income" -> "oi",
        "pairing.cand_relaxed_state" -> "oe",
        "pairing.user_relaxed_state" -> "oe",
        "gender_relaxed.female_relaxed_state" -> "oe",
        "gender_relaxed.male_relaxed_state" -> "oe"
    ).map{case (k, v) => (k, CsvTypes.withNameExtended(v))}

    val columnTypes = orderedColumnTypes.toMap

    val UserStatusProto = Enum("com.eharmony.matching.common.value.ProfileProtoBuffs.UserStatusProto",
        "NEW" -> 0,
        "INCOMPLETE" -> 1,
        "ACTIVE" -> 2,
        "SUBSCRIBER" -> 3,
        "INCOMPLETE_SUBSCRIBER" -> 4,
        "PENDING" -> 5,
        "INVALID_RQ" -> 6,
        "CONTINUITY_SUBSCRIBER" -> 7,
        "MARRIED" -> -1,
        "CLOSED" -> -2,
        "EXPIRED" -> -3,
        "MARRIAGES_OVER" -> -4,
        "UNFIT" -> -5,
        "GIFT_PURCHASER" -> -6,
        "VISITOR" -> -7,
        "FITNESS_FAILURE" -> -8,
        "GIFT_RECIPIENT" -> -9)

    val GenderProto = Enum("com.eharmony.matching.common.value.ProfileProtoBuffs.GenderProto",
        "MALE" -> 1,
        "FEMALE" -> 2)

    val PairingRelaxTypeProto = Enum("com.eharmony.matching.common.value.PairingProtoBuffers.PairingProtos.PairingRelaxTypeProto",
        "INVALID" -> -1,
        "STRICT" -> 0,
        "RELAXED" -> 1)

    val enums = Map(
        "candidate.profile.gender" -> GenderProto,
        "candidate.profile.user_status" -> UserStatusProto,
        "pairing.cand_relaxed_state" -> PairingRelaxTypeProto,
        "pairing.user_relaxed_state" -> PairingRelaxTypeProto,
        "gender_relaxed.female_relaxed_state" -> PairingRelaxTypeProto,
        "gender_relaxed.male_relaxed_state" -> PairingRelaxTypeProto
    )

    val csvLines = CsvLines(orderedColumnTypes.unzip._1.zipWithIndex.toMap, enums, "\t", ",", _.isEmpty, errorOnOptMissingField = false, errorOnOptMissingEnum = false)

    private[this] val DistInd = 14

    val samples = csvLines.apply(io.Source.fromURL(getClass.getClassLoader.getResource("3001_data.tsv")).getLines().take(10).map(l => {
        val a = l.split("\t", -1)
        try {
            if (a(DistInd).toInt < 0) a(DistInd) = "10"
        } catch { case e: NumberFormatException => }
        a.mkString("\t")
    }).toSeq)

    // These come from the proto version of the model in matching-model-conversions
    val results = Seq(
        0.04125072271616186,
        0.0022369321749216324,
        0.0259153383795239,
        0.027152084191575783,
        0.0039043736677613506,
        0.0350693058991552,
        0.0037044274245017932,
        0.0031681934348870462,
        0.02464632851026785,
        0.04991537686753839
    )
}
