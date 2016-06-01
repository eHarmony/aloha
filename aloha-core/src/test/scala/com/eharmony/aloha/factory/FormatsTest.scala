package com.eharmony.aloha.factory

import com.eharmony.matching.notaloha.AnEnum
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.jsonFormat1
import spray.json._

/**
  * Created by ryan on 5/24/16.
  */
object FormatsTest {
  case class GenEnumPossessor[E <: Enum[E]](value: E)
  case class EnumPossessor(value: AnEnum)
  implicit val AnEnumFormat = JavaJsonFormats.enumFormat(classOf[AnEnum])
  implicit val EnumPossessorFormat: RootJsonFormat[EnumPossessor] = jsonFormat1(EnumPossessor)
}

@RunWith(classOf[BlockJUnit4ClassRunner])
class FormatsTest {
  import FormatsTest._

  @Test(expected = classOf[DeserializationException]) def testEnumFormatValue1(): Unit =
    """{ "value": "VALUE_1" }""".parseJson.convertTo[EnumPossessor]

  @Test def testEnumFormatValue2(): Unit =
    assertEquals(EnumPossessor(AnEnum.VALUE_2), """{ "value": "VALUE_2" }""".parseJson.convertTo[EnumPossessor])

  @Test def testEnumFormatValue3(): Unit =
    assertEquals(EnumPossessor(AnEnum.VALUE_3), """{ "value": "VALUE_3" }""".parseJson.convertTo[EnumPossessor])

  @Test def testGenEnumFormatValue3(): Unit = {
    val clas = Class.forName(classOf[AnEnum].getName)
    implicit val ge = geFormat(clas)
    val v = """{ "value": "VALUE_3" }""".parseJson.convertTo(ge)
    assertEquals(GenEnumPossessor(AnEnum.VALUE_3), v)
  }

  def geFormat[E <: Enum[E]](clas: Class[_]): RootJsonFormat[GenEnumPossessor[E]] = {
    implicit val ef = Formats.enumFormat(clas.asInstanceOf[Class[E]])
    jsonFormat1(GenEnumPossessor[E])
  }
}

