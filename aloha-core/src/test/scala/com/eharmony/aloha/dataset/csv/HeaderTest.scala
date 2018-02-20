package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset.RowCreatorBuilder
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Assert.assertEquals

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan.deak on 2/20/18.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class HeaderTest {
  import HeaderTest._

  @Test def testRegularEncoding(): Unit = {
    val rc = csvRowCreator("regular")
    val headers = rc.headers
    val out = rc.apply(EmptyLine)._2

    assertEquals(ExpRegularHeaders, headers)
    assertEquals(ExpRegularOut, out)
  }

  @Test def testHotOne(): Unit = {
    val rc = csvRowCreator("hotOne")
    val headers = rc.headers
    val out = rc.apply(EmptyLine)._2

    assertEquals(ExpHotOneHeaders, headers)
    assertEquals(ExpHotOneOut, out)
  }
}

object HeaderTest {

  private val ExpRegularHeaders = Vector("vec_0", "vec_1", "vec_2", "str", "doub", "bool", "enum")
  private val ExpHotOneHeaders  = Vector("vec_0", "vec_1", "vec_2", "str", "doub", "bool", "enum_VALUE_2", "enum_VALUE_3")

  private val ExpRegularOut = "1,2,3,some_string_value,4.0,true,VALUE_2"
  private val ExpHotOneOut  = "1,2,3,some_string_value,4.0,true,1,0"

  /**
    * JSON for a CSV row creator that produces the following features:
    *
    - "vec": a Seq[Int] of length 3
    - "str": a string
    - "bool": a boolean
    - "enum": an enum of type `com.eharmony.matching.notaloha.AnEnum`.
    *
    * The row creator doesn't rely on any input data that is transformed.  Instead, it is used
    * to check the typing, output and headers for the resulting data.
    *
    * @param encoding Different types of encodings: one of "regular", "hotOne", "thermometer".
    * @return
    */
  private def dsJson(encoding: String) =
    s"""
       |{
       |  "imports": [],
       |  "separator": ",",
       |  "nullValue": "null",
       |  "encoding": "$encoding",
       |  "features": [
       |    { "spec": "1 to 3", "type": "int", "size": 3, "name": "vec" },
       |    { "spec": "\\"some_string_value\\"", "type": "string", "name": "str" },
       |    { "spec": "4", "type": "double", "name": "doub" },
       |    { "spec": "true", "type": "boolean", "name": "bool" },
       |    { "spec": "com.eharmony.matching.notaloha.AnEnum.VALUE_2",
       |      "type": "enum",
       |      "enumClass": "com.eharmony.matching.notaloha.AnEnum",
       |      "name": "enum"
       |    }
       |  ]
       |}
     """.stripMargin

  private def csvRowCreator(encoding: String) = {
    val json = dsJson(encoding)
    val plugin = CompiledSemanticsCsvPlugin()
    val semantics = CompiledSemantics(TwitterEvalCompiler(classCacheDir = None), plugin, Nil)
    val sb = RowCreatorBuilder(semantics, List(CsvRowCreator.Producer[CsvLine]()))
    sb.fromString(json).get
  }

  // Since dsJson doesn't rely on any input data, this can be anything, including null.
  private val EmptyLine: CsvLineImpl = null
}