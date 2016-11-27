package com.eharmony.aloha.models.h2o

import com.eharmony.aloha
import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.matching.testhelp.io.{IoCaptureCompanion, TestWithIoCapture}
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global

object CliTest extends IoCaptureCompanion {
  private val modelLocalUrl = VFS.getManager.resolveFile("res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java").getURL.toString
  private val specUrl = "res:com/eharmony/aloha/models/h2o/test_spec.json"
}

/**
  * Created by ryan on 12/1/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CliTest extends TestWithIoCapture(CliTest) {
  import CliTest._

  @Test def testEmpty(): Unit = {
    val (out, err) = runMain[InterrogatablePrintStream](Cli.main, Array.empty)

    val expected =
      ("""
         |Error: Missing option --spec
         |Error: Missing option --model
         |h2o """.stripMargin + aloha.version + """
         |Usage: h2o [options]
         |
         |  -s <value> | --spec <value>
         |        spec is an Apache VFS URL to an aloha spec file.
         |  -m <value> | --model <value>
         |        model is an Apache VFS URL to a VW binary model.
         |  --fs-type <value>
         |        file system type: vfs1, vfs2, file. default = vfs2.
         |  -n <value> | --name <value>
         |        name of the model.
         |  -i <value> | --id <value>
         |        numeric id of the model.
         |  --external
         |        link to a binary VW model rather than embedding it inline in the aloha model.
         |  --num-missing-thresh <value>
         |        number of missing features to allow before returning a 'no-prediction'.
         |  --note <value>
         |        notes to add to the model. Can provide this many parameter times.
       """).stripMargin

//    assertEquals(expected.trim, errContent.trim)
    assertEquals(expected.trim, err.output.trim)
  }

  @Test def testExternal(): Unit = {

    val (out, err) = runMain[InterrogatablePrintStream](Cli.main, Array(
      "-s",  specUrl,
      "-m", modelLocalUrl,
      "--external",
      "--num-missing-thresh", "1",
      "-n", "test-model",
      "-i", "0"
    ))


    val expected =
      ("""
         |{
         |  "modelType":"H2o",
         |  "modelId":{"id":0,"name":"test-model"},
         |  "numMissingThreshold":1,
         |  "features":{
         |    "Sex":{"spec":"${0}","type":"string"},
         |    "Length":"${1}",
         |    "Diameter":"${2}",
         |    "Height":"${3}",
         |    "Whole weight":"${4}",
         |    "Shucked weight":"${5}",
         |    "Viscera weight":"${6}",
         |    "Shell weight":"${7}"
         |  },
         |  "modelUrl":"""" + modelLocalUrl + """",
         |  "via":"vfs2"
         |}
       """).stripMargin.replaceAll("\\n\\s*", "").trim

//    assertEquals(expected, lastLine(out.Content))
    assertEquals(expected, lastLine(out.output))
  }

  @Test def testInternal(): Unit = {
    val (out, err) = runMain[InterrogatablePrintStream](Cli.main, Array(
      "-s", specUrl,
      "-m", modelLocalUrl,
      "-n", "test-model",
      "-i", "2"
    ))

    val expected = """{"modelType":"H2o","modelId":{"id":2,"name":"test-model"},"features":{"Sex":{"spec":"${0}","type":"string"},"Length":"${1}","Diameter":"${2}","Height":"${3}","Whole weight":"${4}","Shucked weight":"${5}","Viscera weight":"${6}","Shell weight":"${7}"},"model":"Ly8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMAovLyBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjAuaHRtbAovLwovLyBBVVRPR0VORVJBVEVEIEJZIEgyTyBhdCAyMDE1LTA5LTE4VDE1OjQ1OjMxLjcyNC0wNzowMAovLyAzLjIuMC4xCi8vCi8vIFN0YW5kYWxvbmUgcHJlZGljdGlvbiBjb2RlIHdpdGggc2FtcGxlIHRlc3QgZGF0YSBmb3IgR0xNTW9kZWwgbmFtZWQgZ2xtX2FmYTA0ZTMxXzE3YWRfNGNhNl85YmQxXzhhYjgwMDA1Y2UzOAovLwovLyBIb3cgdG8gZG93bmxvYWQsIGNvbXBpbGUgYW5kIGV4ZWN1dGU6Ci8vICAgICBta2RpciB0bXBkaXIKLy8gICAgIGNkIHRtcGRpcgovLyAgICAgY3VybCBodHRwOi8vMTkyLjE2OC4xMS4xMTA6NTQzMjEvMy9oMm8tZ2VubW9kZWwuamFyID4gaDJvLWdlbm1vZGVsLmphcgovLyAgICAgY3VybCBodHRwOi8vMTkyLjE2OC4xMS4xMTA6NTQzMjEvMy9Nb2RlbHMuamF2YS9nbG0tYWZhMDRlMzEtMTdhZC00Y2E2LTliZDEtOGFiODAwMDVjZTM4ID4gZ2xtX2FmYTA0ZTMxXzE3YWRfNGNhNl85YmQxXzhhYjgwMDA1Y2UzOC5qYXZhCi8vICAgICBqYXZhYyAtY3AgaDJvLWdlbm1vZGVsLmphciAtSi1YbXgyZyAtSi1YWDpNYXhQZXJtU2l6ZT0xMjhtIGdsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzguamF2YQovLwovLyAgICAgKE5vdGU6ICBUcnkgamF2YSBhcmd1bWVudCAtWFg6K1ByaW50Q29tcGlsYXRpb24gdG8gc2hvdyBydW50aW1lIEpJVCBjb21waWxlciBiZWhhdmlvci4pCmltcG9ydCBqYXZhLnV0aWwuTWFwOwppbXBvcnQgaGV4Lmdlbm1vZGVsLkdlbk1vZGVsOwoKcHVibGljIGNsYXNzIGdsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzggZXh0ZW5kcyBHZW5Nb2RlbCB7CiAgICBwdWJsaWMgaGV4Lk1vZGVsQ2F0ZWdvcnkgZ2V0TW9kZWxDYXRlZ29yeSgpIHsgcmV0dXJuIGhleC5Nb2RlbENhdGVnb3J5LlJlZ3Jlc3Npb247IH0KCiAgICBwdWJsaWMgYm9vbGVhbiBpc1N1cGVydmlzZWQoKSB7IHJldHVybiB0cnVlOyB9CiAgICBwdWJsaWMgaW50IG5mZWF0dXJlcygpIHsgcmV0dXJuIDg7IH0KICAgIHB1YmxpYyBpbnQgbmNsYXNzZXMoKSB7IHJldHVybiAxOyB9CgogICAgLy8gTmFtZXMgb2YgY29sdW1ucyB1c2VkIGJ5IG1vZGVsLgogICAgcHVibGljIHN0YXRpYyBmaW5hbCBTdHJpbmdbXSBOQU1FUyA9IE5hbWVzSG9sZGVyX2dsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzguVkFMVUVTOwoKICAgIC8vIENvbHVtbiBkb21haW5zLiBUaGUgbGFzdCBhcnJheSBjb250YWlucyBkb21haW4gb2YgcmVzcG9uc2UgY29sdW1uLgogICAgcHVibGljIHN0YXRpYyBmaW5hbCBTdHJpbmdbXVtdIERPTUFJTlMgPSBuZXcgU3RyaW5nW11bXSB7CiAgICAvKiBTZXggKi8gZ2xtX2FmYTA0ZTMxXzE3YWRfNGNhNl85YmQxXzhhYjgwMDA1Y2UzOF9Db2xJbmZvXzAuVkFMVUVTLAogICAgLyogTGVuZ3RoICovIG51bGwsCiAgICAvKiBEaWFtZXRlciAqLyBudWxsLAogICAgLyogSGVpZ2h0ICovIG51bGwsCiAgICAvKiBXaG9sZSB3ZWlnaHQgKi8gbnVsbCwKICAgIC8qIFNodWNrZWQgd2VpZ2h0ICovIG51bGwsCiAgICAvKiBWaXNjZXJhIHdlaWdodCAqLyBudWxsLAogICAgLyogU2hlbGwgd2VpZ2h0ICovIG51bGwsCiAgICAvKiBSaW5ncyAqLyBudWxsCiAgICB9OwogICAgLy8gUHJpb3IgY2xhc3MgZGlzdHJpYnV0aW9uCiAgICBwdWJsaWMgc3RhdGljIGZpbmFsIGRvdWJsZVtdIFBSSU9SX0NMQVNTX0RJU1RSSUIgPSBudWxsOwogICAgLy8gQ2xhc3MgZGlzdHJpYnV0aW9uIHVzZWQgZm9yIG1vZGVsIGJ1aWxkaW5nCiAgICBwdWJsaWMgc3RhdGljIGZpbmFsIGRvdWJsZVtdIE1PREVMX0NMQVNTX0RJU1RSSUIgPSBudWxsOwoKICAgIHB1YmxpYyBnbG1fYWZhMDRlMzFfMTdhZF80Y2E2XzliZDFfOGFiODAwMDVjZTM4KCkgeyBzdXBlcihOQU1FUyxET01BSU5TKTsgfQogICAgcHVibGljIFN0cmluZyBnZXRVVUlEKCkgeyByZXR1cm4gTG9uZy50b1N0cmluZygtMTI3Njk2MzE1NTYzNDU0NTg5NkwpOyB9CgogICAgLy8gUGFzcyBpbiBkYXRhIGluIGEgZG91YmxlW10sIHByZS1hbGlnbmVkIHRvIHRoZSBNb2RlbCdzIHJlcXVpcmVtZW50cy4KICAgIC8vIEphbSBwcmVkaWN0aW9ucyBpbnRvIHRoZSBwcmVkc1tdIGFycmF5OyBwcmVkc1swXSBpcyByZXNlcnZlZCBmb3IgdGhlCiAgICAvLyBtYWluIHByZWRpY3Rpb24gKGNsYXNzIGZvciBjbGFzc2lmaWVycyBvciB2YWx1ZSBmb3IgcmVncmVzc2lvbiksCiAgICAvLyBhbmQgcmVtYWluaW5nIGNvbHVtbnMgaG9sZCBhIHByb2JhYmlsaXR5IGRpc3RyaWJ1dGlvbiBmb3IgY2xhc3NpZmllcnMuCiAgICBwdWJsaWMgZmluYWwgZG91YmxlW10gc2NvcmUwKCBkb3VibGVbXSBkYXRhLCBkb3VibGVbXSBwcmVkcyApIHsKICAgICAgICBkb3VibGUgZXRhID0gMC4wOwogICAgICAgIGZpbmFsIGRvdWJsZSBbXSBiID0gQkVUQTsKICAgICAgICBmb3IoaW50IGkgPSAwOyBpIDwgQ0FUT0ZGUy5sZW5ndGgtMTsgKytpKSB7CiAgICAgICAgICAgIGludCBpdmFsID0gKGludClkYXRhW2ldOwogICAgICAgICAgICBpZihpdmFsICE9IGRhdGFbaV0pIHRocm93IG5ldyBJbGxlZ2FsQXJndW1lbnRFeGNlcHRpb24oImNhdGVnb3JpY2FsIHZhbHVlIG91dCBvZiByYW5nZSIpOwogICAgICAgICAgICBpdmFsICs9IENBVE9GRlNbaV07CiAgICAgICAgICAgIGlmKGl2YWwgPCBDQVRPRkZTW2kgKyAxXSkKICAgICAgICAgICAgICAgIGV0YSArPSBiW2l2YWxdOwogICAgICAgIH0KICAgICAgICBmb3IoaW50IGkgPSAxOyBpIDwgYi5sZW5ndGgtMS0yOyArK2kpCiAgICAgICAgICAgIGV0YSArPSBiWzIraV0qZGF0YVtpXTsKICAgICAgICBldGEgKz0gYltiLmxlbmd0aC0xXTsgLy8gcmVkdWNlIGludGVyY2VwdAogICAgICAgIGRvdWJsZSBtdSA9IGhleC5nZW5tb2RlbC5HZW5Nb2RlbC5HTE1faWRlbnRpdHlJbnYoZXRhKTsKICAgICAgICBwcmVkc1swXSA9IG11OwogICAgICAgIHJldHVybiBwcmVkczsKICAgIH0KICAgIC8vIFRoZSBDb2VmZmljaWVudHMKICAgIHB1YmxpYyBzdGF0aWMgZmluYWwgZG91YmxlW10gQkVUQSA9IHswLjAsLTAuODI1NzE5OTYwNjM0NTEyNywwLjA0ODU4OTMzNjcxMDY4NzY4NiwwLjAsMTAuMjU3NzMwMzE4MzI1MTUyLDEwLjkwNTAzNTQyNjExNDU0NCw2LjQxMTg5ODc1MTg1Mjc2MywtMTcuMDY2NTYxNzc1NjYyNzk4LC03LjcwNjIzMjI2NDY4MzQ5NSwxMS41OTE3MjE5ODQxNTQ0MTYsMy45MTg1MjA2MDU5NDU0NDA1fTsKICAgIC8vIENhdGVnb3JpY2FsIE9mZnNldHMKICAgIHB1YmxpYyBzdGF0aWMgZmluYWwgaW50W10gQ0FUT0ZGUyA9IHswLDN9OwoKCgoKCiAgICAvLyBUaGUgY2xhc3MgcmVwcmVzZW50aW5nIHRyYWluaW5nIGNvbHVtbiBuYW1lcwogICAgc3RhdGljIGNsYXNzIE5hbWVzSG9sZGVyX2dsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzggaW1wbGVtZW50cyBqYXZhLmlvLlNlcmlhbGl6YWJsZSB7CiAgICAgICAgcHVibGljIHN0YXRpYyBmaW5hbCBTdHJpbmdbXSBWQUxVRVMgPSBuZXcgU3RyaW5nWzhdOwogICAgICAgIHN0YXRpYyB7CiAgICAgICAgICAgIE5hbWVzSG9sZGVyX2dsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzhfMC5maWxsKFZBTFVFUyk7CiAgICAgICAgfQogICAgICAgIHN0YXRpYyBmaW5hbCBjbGFzcyBOYW1lc0hvbGRlcl9nbG1fYWZhMDRlMzFfMTdhZF80Y2E2XzliZDFfOGFiODAwMDVjZTM4XzAgaW1wbGVtZW50cyBqYXZhLmlvLlNlcmlhbGl6YWJsZSB7CiAgICAgICAgICAgIHN0YXRpYyBmaW5hbCB2b2lkIGZpbGwoU3RyaW5nW10gc2EpIHsKICAgICAgICAgICAgICAgIHNhWzBdID0gIlNleCI7CiAgICAgICAgICAgICAgICBzYVsxXSA9ICJMZW5ndGgiOwogICAgICAgICAgICAgICAgc2FbMl0gPSAiRGlhbWV0ZXIiOwogICAgICAgICAgICAgICAgc2FbM10gPSAiSGVpZ2h0IjsKICAgICAgICAgICAgICAgIHNhWzRdID0gIldob2xlIHdlaWdodCI7CiAgICAgICAgICAgICAgICBzYVs1XSA9ICJTaHVja2VkIHdlaWdodCI7CiAgICAgICAgICAgICAgICBzYVs2XSA9ICJWaXNjZXJhIHdlaWdodCI7CiAgICAgICAgICAgICAgICBzYVs3XSA9ICJTaGVsbCB3ZWlnaHQiOwogICAgICAgICAgICB9CiAgICAgICAgfQogICAgfQogICAgLy8gVGhlIGNsYXNzIHJlcHJlc2VudGluZyBjb2x1bW4gU2V4CiAgICBzdGF0aWMgY2xhc3MgZ2xtX2FmYTA0ZTMxXzE3YWRfNGNhNl85YmQxXzhhYjgwMDA1Y2UzOF9Db2xJbmZvXzAgaW1wbGVtZW50cyBqYXZhLmlvLlNlcmlhbGl6YWJsZSB7CiAgICAgICAgcHVibGljIHN0YXRpYyBmaW5hbCBTdHJpbmdbXSBWQUxVRVMgPSBuZXcgU3RyaW5nWzNdOwogICAgICAgIHN0YXRpYyB7CiAgICAgICAgICAgIGdsbV9hZmEwNGUzMV8xN2FkXzRjYTZfOWJkMV84YWI4MDAwNWNlMzhfQ29sSW5mb18wXzAuZmlsbChWQUxVRVMpOwogICAgICAgIH0KICAgICAgICBzdGF0aWMgZmluYWwgY2xhc3MgZ2xtX2FmYTA0ZTMxXzE3YWRfNGNhNl85YmQxXzhhYjgwMDA1Y2UzOF9Db2xJbmZvXzBfMCBpbXBsZW1lbnRzIGphdmEuaW8uU2VyaWFsaXphYmxlIHsKICAgICAgICAgICAgc3RhdGljIGZpbmFsIHZvaWQgZmlsbChTdHJpbmdbXSBzYSkgewogICAgICAgICAgICAgICAgc2FbMF0gPSAiRiI7CiAgICAgICAgICAgICAgICBzYVsxXSA9ICJJIjsKICAgICAgICAgICAgICAgIHNhWzJdID0gIk0iOwogICAgICAgICAgICB9CiAgICAgICAgfQogICAgfQp9Cg=="}"""

//    assertEquals(expected.trim, lastLine(outContent))
    assertEquals(expected.trim, lastLine(out.output))
  }

  @Test def testCompile(): Unit = {
    val (out, err) = runMain[InterrogatablePrintStream](Cli.main, Array(
      "-s", specUrl,
      "-m", modelLocalUrl,
      "-n", "test-model",
      "-i", "2"
    ))

//    val modelJson = lastLine(outContent)
    val modelJson = lastLine(out.output)

    val csvPlugin = CompiledSemanticsCsvPlugin(
      "0" -> CsvTypes.StringOptionType,
      "1" -> CsvTypes.DoubleOptionType,
      "2" -> CsvTypes.DoubleOptionType,
      "3" -> CsvTypes.DoubleOptionType,
      "4" -> CsvTypes.DoubleOptionType,
      "5" -> CsvTypes.DoubleOptionType,
      "6" -> CsvTypes.DoubleOptionType,
      "7" -> CsvTypes.DoubleOptionType
    )

    val semantics = CompiledSemantics(
      TwitterEvalCompiler(),
      csvPlugin,
      Seq("com.eharmony.aloha.feature.BasicFunctions._"))

    val factory = ModelFactory.defaultFactory.toTypedFactory[CsvLine, Double](semantics)
    val model = factory.fromString(modelJson).get
    val csvLines = CsvLines(indices = (0 to 7 map (i => (i.toString, i))).toMap,
                            fs = ",")
    val lines = csvLines(
      "M,,,,,,,",
      "M,0,0,0,0,0,0,0",
      "F,0,0,0,0,0,0,0",
      "I,0,0,0,0,0,0,0")

    val output = lines.map(model.apply)
    assertEquals(None, output.head)

    val exp = Seq(3.9671099427, 3.9185206059454405, 3.0928006453)
    output.tail.zip(exp).zipWithIndex.foreach{ case((a, e), i) => assertEquals(s"for $i", e, a.get, 1.0e-6) }
  }

  private[this] def lastLine(s: String) = s.trim.split("\n").last.trim
}
