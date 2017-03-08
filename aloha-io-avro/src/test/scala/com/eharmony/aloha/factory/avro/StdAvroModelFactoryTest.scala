package com.eharmony.aloha.factory.avro

import com.eharmony.aloha.audit.impl.avro.Score
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.io.vfs.Vfs1
import com.eharmony.aloha.models.Model
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.util.Try

/**
  * Created by deak on 3/2/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class StdAvroModelFactoryTest {
  import StdAvroModelFactoryTest._

  @Test def testHappyPathUrlVfs1ConfigExplicit(): Unit =
    test(StdAvroModelFactory.fromConfig(UrlConfig(Vfs1(SchemaVfs1FileObject), ReturnType, Imports)))

  @Test def testHappyPathUrlVfs1ConfigImplicit(): Unit =
    // Notice the implicit conversion from Apache VFS FileObject to aloha.io.vfs.Vfs.
    test(StdAvroModelFactory.fromConfig(UrlConfig(SchemaVfs1FileObject, ReturnType, Imports)))

  @Test def testHappyPathUrlVfs2Config(): Unit =
    test(StdAvroModelFactory.fromConfig(UrlConfig(SchemaVfs2FileObject, ReturnType, Imports)))

  @Test def testHappyPathUrlFileConfig(): Unit =
    test(StdAvroModelFactory.fromConfig(UrlConfig(SchemaFile, ReturnType, Imports)))

  @Test def testHappyPathSchemaConfig(): Unit =
    test(StdAvroModelFactory.fromConfig(SchemaConfig(TheSchema, ReturnType, Imports)))

  @Test def testHappyPathApplyDefaultVfs2(): Unit =
    test(StdAvroModelFactory(SchemaUrl, ReturnType, Imports))

  @Test def testHappyPathApplyVfs1(): Unit =
    test(StdAvroModelFactory(SchemaUrl, ReturnType, Imports, useVfs2 = false))


  private[this] def test(factoryTry: => Try[ModelFactory[GenericRecord, Score]]): Unit = {
    // If it throws, bubble up the error, so the test errs
    // Factory actually gets created here b/c call-by-name.
    val factory = factoryTry.get

    // If it throws, bubble up the error, so the test errs
    val model: Model[GenericRecord, Score] = factory.fromString(ModelJson).get

    val score = model(record).getValue.asInstanceOf[Double]

    assertEquals(ExpectedResult, score, 0)
  }

  /**
    * Record should produce a 7d when run through the model
    * @return
    */
  private[this] def record = {
    val r = new GenericData.Record(TheSchema)
    r.put("req_str_1", "smart handsome stubborn")
    r
  }
}

object StdAvroModelFactoryTest {
  private lazy val TheSchema = {
    val is = getClass.getClassLoader.getResourceAsStream(SchemaUrlResource)
    try new Schema.Parser().parse(is) finally IOUtils.closeQuietly(is)
  }

  private val ExpectedResult = 7d

  private val SchemaUrlResource = "avro/class7.avpr"

  private val SchemaUrl = s"res:$SchemaUrlResource"

  private val SchemaFile = new java.io.File(getClass.getClassLoader.getResource(SchemaUrlResource).getFile)

  private val SchemaVfs1FileObject = org.apache.commons.vfs.VFS.getManager.resolveFile(SchemaUrl)

  private val SchemaVfs2FileObject = org.apache.commons.vfs2.VFS.getManager.resolveFile(SchemaUrl)

  private val Imports = Seq("com.eharmony.aloha.feature.BasicFunctions._", "scala.math._")

  private val ReturnType = "Double"

  private val ModelJson =
    """
      |{
      |  "modelType": "Regression",
      |  "modelId": { "id": 0, "name": "" },
      |  "features" : {
      |    "my_attributes": "${req_str_1}.split(\"\\\\W+\").map(v => (s\"=$v\", 1.0))"
      |  },
      |  "weights": {
      |    "my_attributes=handsome": 1,
      |    "my_attributes=smart": 2,
      |    "my_attributes=stubborn": 4
      |  }
      |}
    """.stripMargin
}

