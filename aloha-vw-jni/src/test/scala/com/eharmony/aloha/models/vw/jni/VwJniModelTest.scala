package com.eharmony.aloha.models.vw.jni

import java.io._
import java.{lang => jl}

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.factory.JavaJsonFormats._
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.TypeCoercion
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.aloha.semantics.func.{GenFunc, GeneratedAccessor}
import com.eharmony.aloha.util.Logging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{BeforeClass, Ignore, Test}
import spray.json.DefaultJsonProtocol._
import spray.json._
import vw.VW

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwJniModelTest {
    import VwJniModelTest._

    @Test def testSerialization(): Unit = {
        val m = model[Double](typeTestJson)
        val f = File.createTempFile("testSerialization", "obj")
        f.deleteOnExit()

        val oos = new ObjectOutputStream(new FileOutputStream(f))
        oos.writeObject(m)
        oos.close()

        val ois = new ObjectInputStream(new FileInputStream(f))
        val m1 = ois.readObject().asInstanceOf[VwJniModel[CsvLine, Double]]
        ois.close()

        assertEquals(m.toString, m1.toString)
        assertEquals(m(missingHeight), m1(missingHeight))
    }

    @Test def testFailureWhenVwIsCreatedWithLinkParamAndInitialRegressorHasSameLink(): Unit = {
        try {
            val f = VwJniModel.allocateModel(1, logisticModelB64Encoded)
            new VW(LogisticModelParams + s" -i ${f.getCanonicalPath}").close()
            fail("VW should throw a java.lang.Exception with message: \"option '--link' cannot be specified more than once\"");
        }
        catch {
            // Can't rely on msg because different versions of boost (a dep of VW) return different messages
            // case e: IllegalArgumentException if e.getClass.getSimpleName == "IllegalArgumentException" && e.getMessage == "option '--link' cannot be specified more than once" =>
            case e: IllegalArgumentException if e.getClass.getSimpleName == "IllegalArgumentException" =>
            case t: Throwable => throw t
        }
    }

    @Test def testAllocatedModelEqualsOriginalModel(): Unit = {
        val modelBytes = readFile(VwModelFile)
        val out = new String(Base64.encodeBase64(modelBytes))
        val tmpFile = VwJniModel.allocateModel(1, out)
        val tmpBytes = readFile(tmpFile)
        println(out)
        assertArrayEquals(modelBytes, tmpBytes)
    }

    @Test def testByteOutputType(): Unit = testOutputType[Byte]()
    @Test def testShortOutputType(): Unit = testOutputType[Short]()
    @Test def testIntOutputType(): Unit = testOutputType[Int]()
    @Test def testLongOutputType(): Unit = testOutputType[Float]()
    @Test def testFloatOutputType(): Unit = testOutputType[Float]()
    @Test def testDoubleOutputType(): Unit = testOutputType[Double]()
    @Test def testStringOutputType(): Unit = testOutputType[String]()
    @Test def testJavaByteOutputType(): Unit = testOutputType[jl.Byte]()
    @Test def testJavaShortOutputType(): Unit = testOutputType[jl.Short]()
    @Test def testJavaIntegerOutputType(): Unit = testOutputType[jl.Integer]()
    @Test def testJavaLongOutputType(): Unit = testOutputType[jl.Long]()
    @Test def testJavaFloatOutputType(): Unit = testOutputType[jl.Float]()
    @Test def testJavaDoubleOutputType(): Unit = testOutputType[jl.Double]()

    @Test def testNoThreshWithMissing(): Unit = {
        val m = model[Float](noThreshJson)
        val y = m(missingHeight)
        assertTrue(y.isDefined)
    }

    @Test def testExceededThresh(): Unit = {
        val m = model[Float](threshJson)
        val y = m(missingHeight)
        assertTrue(y.isEmpty)
        assertEquals(List("height_cm"), m.featureFunctions.head.accessorOutputMissing(missingHeight))
    }

    /**
     * This should succeed.  It just logs when non-existent features are listed in the namespace.
     */
    @Test def testNsWithUndeclaredFeatureNames(): Unit = {
        val m = model[Float](nsWithUndeclFeatureJson)
    }

    /**
     * It's ok to have namespaces not cover all features.  The remainder goes into the default ns.
     */
    @Test def testNsDoesntCoverAllFeatureNamesFromJson(): Unit = {
        val m = model[Float](nonCoveringNsJson)
        val input = m.generateVwInput(missingHeight)
        assertEquals(Right("| height:180"), input)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testNssAndDefaultDoesntCoverAllFeatureInd(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        // Because height_mm feature isn't in any namespace.
        VwJniModel(
            ModelId.empty,
            VwB64Model,
            "--quiet",
            Vector("height_mm"),
            Vector(h),
            Nil,
            Nil,
            (f: Double) => f
        )
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testNamesSizeLtFeaturesSize(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        VwJniModel(
            ModelId.empty,
            VwB64Model,
            "--quiet",
            Vector(),
            Vector(h),
            Nil,
            Nil,
            (f: Double) => f
        )
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testFeaturesSizeLtNamesSize(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        VwJniModel(
            ModelId.empty,
            VwB64Model,
            "--quiet",
            Vector("height_mm"),
            Vector(),
            Nil,
            Nil,
            (f: Double) => f
        )
    }

    /**
     * Intentionally pass a corrupted model to VW and test the recoverability.  Currently, throws a SEG FAULT so
     * recoverability is out of the question.  So ignore the test.
     */
    @Ignore @Test def testCorruptedModel(): Unit = {
        val badModel = VFS.getManager.resolveFile("res:VwJniModelTest-vw_bad.model").getName.getPath
        println(s"attempting to instantiate corrupted VW model: $badModel")

        // This try won't do anything because this is currently a SEG FAULT so the world will just blow up.
        Try { new VW(s"--quiet -i $badModel") }
    }


    /**
     * Can't test.  Catching throwable or setting test annotation to @Test(expected = classOf[Error]) causes the
     * world to blow up.  Usually says: Invalid memory access of location 0x0 rip=[HEX HERE]
     */
    // TODO: Figure out how to test this.
    @Test def testBadVwArgsThrowsEx(): Unit = {
        try {
            val m = model[Float](badVwArgsJson)
            fail("Should throw exception.")
        }
        catch {
            case e: AssertionError => throw e

            // Swallow.  We want this exception.
            // Can't rely on msg because different versions of boost (a dep of VW) return different messages
            case e: IllegalArgumentException => // if e.getMessage == "option '--BAD_FEATURE___ounq24tjnasdf8h'" =>

            // Wrong exception type.
            case e: Throwable if e.getMessage != "Should throw exception." =>
                // if e.getMessage == "option '--BAD_FEATURE___ounq24tjnasdf8h'" =>
                fail("Should throw IllegalArgumentException.  Threw Exception.")
        }
    }

    private[this] def model[A : RefInfo : ScoreConverter : JsonReader](modelJson: JsValue) = {
        val m = ModelFactory.defaultFactory.getModel[CsvLine, A](modelJson, Option(semantics)).get
        assertEquals("VwJniModel", m.getClass.getSimpleName)
        m.asInstanceOf[VwJniModel[CsvLine, A]]
    }

    /**
     * Tests that the output equals the expected output value.  This ensures that the
     * @tparam A
     */
    private[this] def testOutputType[A : RefInfo : ScoreConverter : JsonReader](): Unit = {
        val tc = TypeCoercion[Double, Option[A]].get
        val m = model[A](typeTestJson)
        val y = m(missingHeight)
        assertEquals(tc(ExpVwOutput), y)
    }
}

object VwJniModelTest extends Logging {
    private[jni] val VwModelFile = new File(FileLocations.testClassesDirectory, "VwJniModelTest-vw.model")
    private[jni] val VwModelPath = VwModelFile.getCanonicalPath

    private[jni] lazy val VwB64Model = VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwModelFile))

    val columns = Seq(
        "height_cm" -> CsvTypes.LongOptionType,
        "weight" -> CsvTypes.IntType,
        "hair.color" -> CsvTypes.StringType
    )

    val featuresTestJson =
        """
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "height_mm": { "spec": "${height_cm} * 10", "defVal": [["=UNK", 1]] },
          |    "height_cm": "${height_cm}",
          |    "weight": "ind(${weight} / 10)",
          |    "hair": { "spec": "ind(${hair.color})" }
          |  },
          |  "namespaces": {
          |    "personal_features": [ "height_mm", "weight", "hair" ]
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ]
          |  }
          |}
        """.stripMargin.trim.parseJson

    lazy val typeTestJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "weight": "ind(${weight} / 10)"
          |  },
          |  "namespaces": {
          |    "personal_features": [ "height_mm", "weight", "hair" ]
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ],
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson

    lazy val noThreshJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "features": {
          |    "height": "ind(${height_cm} * 10)"
          |  },
          |  "namespaces": {
          |    "personal_features": [ "height" ]
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ],
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson

    lazy val threshJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "height": "ind(${height_cm} * 10)"
          |  },
          |  "namespaces": {
          |    "personal_features": [ "height" ]
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ],
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson

    lazy val nsWithUndeclFeatureJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "height": "Seq((\"\", 1.0))"
          |  },
          |  "namespaces": {
          |    "personal_features": [ "weight" ]
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ],
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson

    lazy val nonCoveringNsJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "height": "Seq((\"\", 180.0))"
          |  },
          |  "vw": {
          |    "params": [
          |      "--quiet",
          |      "-t"
          |    ],
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson


    lazy val badVwArgsJson =
       ("""
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "features": { "height": "Seq((\"\", 180.0))" },
          |  "vw": {
          |    "params": "--quiet --BAD_FEATURE___ounq24tjnasdf8h",
          |    "model": """".stripMargin + VwB64Model + """"
          |  }
          |}
        """.stripMargin).trim.parseJson


    val csvLines = CsvLines(indices = columns.unzip._1.zipWithIndex.toMap, fs = ",")

    val semantics = {
        // NOTE: It is very important that the classCacheDir is specified because the serialization test
        //       will fail otherwise.  This is because the class definition won't be found otherwise.
        val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
        val plugin = CompiledSemanticsCsvPlugin(columns:_*)
        val imports = Seq("scala.math._", "com.eharmony.aloha.feature.BasicFunctions._")
        CompiledSemantics(compiler, plugin, imports)
    }

    val missingHeight = csvLines(",0,red")


    /**
     * The output of the model created by createModel.
     */
    val ExpVwOutput = 0.5041967630386353

    /**
     * Don't call this manually.  Should only be called by the testing framework.  Is Idempotent though.
     */
    @BeforeClass def createModel(): Unit = {
        val x = for {
            deleted <- Try { VwModelFile.delete }
            _ <- allocateModel()
        } yield Unit

        if (x.isFailure) error(s"Couldn't properly allocate vw model: $VwModelPath")
    }

    // (paste -d '\n' <(jot -b "-1 |" 100) <(jot -b "1 |" 100)) | vw --quiet --link logistic --loss_function logistic -f log_0.5.model
    // echo "" | vw -t --quiet -i log_0.5.model -p pred; cat pred; rm -f ./pred ./log_0.5.model
    // 0.504197
    private[this] def allocateModel(): Try[Unit] = {
        val m = new VW(s"--quiet --loss_function logistic --link logistic -f $VwModelPath")
        1 to 100 foreach { _ =>
            m.learn("-1 | ")
            m.learn( "1 | ")
        }
        m.close()
        Try(())
    }

    private def logisticModelJson(includeModel: Boolean) = {

        val json =
            s"""
              |{
              |  "modelType": "VwJNI",
              |  "modelId": { "id": 0, "name": "" },
              |  "features": {
              |    "height": "Seq((\\"\\", 180.0))"
              |  },
              |  "vw": {
              |    "params": "--quiet --loss_function logistic --link logistic${if (includeModel) " -i " + VwModelPath else ""}",
              |    "model": "$logisticModelB64Encoded"
              |  }
              |}
            """.stripMargin.trim.parseJson

        json
    }

    def logisticModelB64Encoded = new String(Base64.encodeBase64(readFile(VwModelFile)))

    private val LogisticModelParams = "--quiet --link logistic --loss_function logistic"

    private def readFile(f: File, maxFileSize: Int = 1024): Array[Byte] =
        readInputStream(new FileInputStream(f), maxFileSize)

    private def readInputStream(is: InputStream, maxFileSize: Int = 1024): Array[Byte] = {
        val d = new Array[Byte](maxFileSize)
        try {
            val nBytes = is.read(d)
            val data = new Array[Byte](nBytes)
            System.arraycopy(d, 0, data, 0, nBytes)
            data
        }
        finally {
            IOUtils.closeQuietly(is)
        }
    }
}
