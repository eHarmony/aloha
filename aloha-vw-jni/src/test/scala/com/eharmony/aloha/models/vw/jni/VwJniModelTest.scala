package com.eharmony.aloha.models.vw.jni

import java.io._
import java.{lang => jl}

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.factory.JavaJsonFormats._
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.fs.FsType
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
import org.apache.commons.vfs2
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{BeforeClass, Ignore, Test}
import spray.json.DefaultJsonProtocol._
import spray.json._
import vw.VW

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try


/**
 * These tests are now designed to pass if the VW model cannot be created in the BeforeClass method.
 * This is due to Travis not working as we expect it to.  Because cat /proc/version doesn't match
 * the purported os the VW JNI library doesn't know which system dependent version of the lib
 * to load and these tests will consequently fail.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwJniModelTest {
    import VwJniModelTest._

    /**
     * This test works locally but fails on jenkins.  Ignore for now.
     */
    @Ignore @Test def testSerialization(): Unit = {
        val m = model[Double](typeTestJson)

        val baos = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(baos)
        oos.writeObject(m)
        oos.close()

        val ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray))
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

    @Test def testNssAndDefaultDoesntCoverAllFeatureInd(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        // Because height_mm feature isn't in any namespace.

        try {
            VwJniModel(
                ModelId.empty,
                VwB64Model,
                "--quiet",
                Vector("height_mm"),
                Vector(h),
                Nil,
                Nil,
                (f: Double) => f,
                None,
                None,
                FsType.vfs2
            )
            fail("should throw IllegalArgumentException")
        }
        catch {
            case e: IllegalArgumentException if e.getMessage == "requirement failed: defaultNamespace and namespaces must cover all indices (0 until 1).  Missing 0" =>
            case e: Throwable => throw e
        }
    }

    @Test def testNamesSizeLtFeaturesSize(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        try {
            VwJniModel(
                ModelId.empty,
                VwB64Model,
                "--quiet",
                Vector(),
                Vector(h),
                Nil,
                Nil,
                (f: Double) => f,
                None,
                None,
                FsType.vfs2
            )
            fail("should throw IllegalArgumentException")
        }
        catch {
            case e: IllegalArgumentException if e.getMessage == "requirement failed: featureNames.size (0}) != featureFunctions.size (1})" =>
            case e: Throwable => throw e
        }
    }

    @Test def testFeaturesSizeLtNamesSize(): Unit = {
        val accessor = GeneratedAccessor("height_cm", (_:CsvLine).ol("height_cm"))
        val h = GenFunc.f1(accessor)("ind(height_cm * 10)", _.map(h => Seq(("height_mm=" + (h * 10), 1.0))).getOrElse(Nil))

        try {
            VwJniModel(
                ModelId.empty,
                VwB64Model,
                "--quiet",
                Vector("height_mm"),
                Vector(),
                Nil,
                Nil,
                (f: Double) => f,
                None,
                None,
                FsType.vfs2
            )
            fail("should throw IllegalArgumentException")
        }
        catch {
            case e: IllegalArgumentException if e.getMessage == "requirement failed: featureNames.size (1}) != featureFunctions.size (0})" =>
            case e: Throwable => throw e
        }
    }

    /**
     * Intentionally pass a corrupted model to VW and test the recoverability.  Currently, throws a SEG FAULT so
     * recoverability is out of the question.  So ignore the test.
     */
    @Ignore @Test def testCorruptedModel(): Unit = {
        val badModel = vfs2.VFS.getManager.resolveFile("res:VwJniModelTest-vw_bad.model").getName.getPath
        println(s"attempting to instantiate corrupted VW model: $badModel")

        // This try won't do anything because this is currently a SEG FAULT so the world will just blow up.
        Try { new VW(s"--quiet -i $badModel") }
    }



    @Test def testResUrlDoesntCopyToLocal(): Unit = {
        val resUrl = url("res:" + VwModelBaseName)
        val res = model[Float](extJson(resUrl.toString))
        assertFalse("'res' URLs should copy VW model to temp directory.", res.finalVwParams.contains(TmpDir))
    }

    @Test def testFileUrlDoesntCopyToLocal(): Unit = {
        val fileUrl = url(VwModelPath).toString
        val file = model[Float](extJson(fileUrl))
        assertFalse("'file' URLs should not copy VW model to temp directory.", file.finalVwParams.contains(TmpDir))
    }

    @Test def testNakedUrlDoesntCopyToLocal(): Unit = {
        val naked = model[Float](extJson(VwModelPath))
        assertFalse("URLs with no protocol should not copy VW model to temp directory.", naked.finalVwParams.contains(TmpDir))

    }

    @Test def testTmpUrlCopiesToLocal(): Unit = {
        val tmpUrl = url("tmp://temp_vw_model1")
        val tmpUrlPath = tmpUrl.getName.getPath
        vfs2.FileUtil.copyContent(url(VwModelPath), tmpUrl)
        val tmp = model[Float](extJson(tmpUrl.toString))
        val file = """^.*\s+-i\s*([^\s]*\.model).*$""".r
        tmp.finalVwParams match {
            case file(tmpFile) =>
                assertFalse(s"Temp file ($tmpFile) should already be deleted.", new File(tmpFile).exists())
                assertTrue("'tmp' URLs should copy VW model to temp directory.", tmp.finalVwParams.contains(TmpDir))
                assertFalse("'tmp' URLs should copy VW model to temp directory.", tmp.finalVwParams.contains(tmpUrlPath))
            case _ => fail("Should have a temp file location")
        }
    }

    @Test def testRamUrlCopiesToLocal(): Unit = {
        val ramUrl = url("ram://ram_vw_model1")
        val ramUrlPath = ramUrl.getName.getPath
        vfs2.FileUtil.copyContent(url(VwModelPath), ramUrl)
        val ram = model[Float](extJson(ramUrl.toString))

        val file = """^.*\s+-i\s*([^\s]*\.model).*$""".r
        ram.finalVwParams match {
            case file(tmpFile) =>
                assertFalse(s"Temp file ($tmpFile) should already be deleted.", new File(tmpFile).exists())
                assertTrue("'ram' URLs should copy VW model to temp directory.", ram.finalVwParams.contains(TmpDir))
                assertFalse("'ram' URLs should copy VW model to temp directory.", ram.finalVwParams.contains(ramUrlPath))
            case _ => fail("Should have a temp file location")
        }
    }

    @Test def testGzFileUrlCopiesToLocal(): Unit = {
        val localUrl = url(VwModelPath)
        val gzUrl = url("gz://" + VwModelPath + ".gz")
        vfs2.FileUtil.copyContent(localUrl, gzUrl)

        // Assert that gzipping actually transformed the file.
        assertFalse("Gzipping should modify file",
                    Base64.encodeBase64(vfs2.FileUtil.getContent(localUrl)) ==
                    Base64.encodeBase64(vfs2.FileUtil.getContent(url(VwModelPath + ".gz"))))

        // Show that we can take gzipped URLs and that they are copied to a local temp file.
        val gz = model[Float](extJson(gzUrl.toString))

        val file = """^.*\s+-i\s*([^\s]*\.model).*$""".r
        gz.finalVwParams match {
            case file(tmpFile) =>
                assertFalse(s"Temp file ($tmpFile) should already be deleted.", new File(tmpFile).exists())
                assertTrue("'gz' URLs should copy VW model to temp directory.", gz.finalVwParams.contains(TmpDir))
            case _ => fail("Should have a temp file location")
        }
    }

    @Test def testBzip2FileUrlCopiesToLocal(): Unit = {
        val localUrl = url(VwModelPath)
        val bz2Url = url("bz2://" + VwModelPath + ".bz2")
        vfs2.FileUtil.copyContent(localUrl, bz2Url)

        // Assert that zipping actually transformed the file.
        assertFalse("BZip2ing should modify file",
            Base64.encodeBase64(vfs2.FileUtil.getContent(localUrl)) ==
                Base64.encodeBase64(vfs2.FileUtil.getContent(url(VwModelPath + ".bz2"))))

        // Show that we can take zipped URLs and that they are copied to a local temp file.
        val bz2 = model[Float](extJson(bz2Url.toString))

        val file = """^.*\s+-i\s*([^\s]*\.model).*$""".r
        bz2.finalVwParams match {
            case file(tmpFile) =>
                assertFalse(s"Temp file ($tmpFile) should already be deleted.", new File(tmpFile).exists())
                assertTrue("'zip' URLs should copy VW model to temp directory.", bz2.finalVwParams.contains(TmpDir))
            case _ => fail("Should have a temp file location")
        }
    }

    @Test def testExternalModel(): Unit = {
        val b64Json =
            s"""
               |{
               |  "modelType": "VwJNI",
               |  "modelId": { "id": 0, "name": "" },
               |  "features": {
               |    "height": "Seq((\\"\\", 180.0))"
               |  },
               |  "vw": {
               |    "params": "--quiet --loss_function logistic",
               |    "model": "$VwB64Model"
               |  }
               |}
             """.stripMargin.trim.parseJson

        val m = model[Float](extJson(VwModelPath))
        assertEquals(ExpVwOutput, m(missingHeight).get, 0)

        val m1 = model[Float](b64Json)
        assertEquals(m1(missingHeight).get, m(missingHeight).get, 0)
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
    private[jni] val VwModelBaseName = "VwJniModelTest-vw.model"
    private[jni] val VwModelFile = new File(FileLocations.testClassesDirectory, VwModelBaseName)
    private[jni] val VwModelPath = VwModelFile.getCanonicalPath

    private[jni] lazy val VwB64Model = VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwModelFile))

    private[jni] def url(s: String) = vfs2.VFS.getManager.resolveFile(s)

    private[jni] val TmpDir = {
        val tf = File.createTempFile("prefix_doesnt_matter", "suffix_doesnt_matter")
        tf.deleteOnExit()
        tf.getParent
    }

    private[jni] def extJson(urlStr: String) = {
        s"""
           |{
           |  "modelType": "VwJNI",
           |  "modelId": { "id": 0, "name": "" },
           |  "features": {
           |    "height": "Seq((\\"\\", 180.0))"
           |  },
           |  "vw": {
           |    "params": "--quiet --loss_function logistic",
           |    "modelUrl": "$urlStr"
           |  }
           |}
         """.stripMargin.trim.parseJson
    }


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
           |    "via": "vfs1",
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

            // Try doesn't catch ExceptionInInitializerError, but this is exactly what we want to catch, so wrap, if found.
            _       <- Try { try { allocateModel() } catch { case e: ExceptionInInitializerError => throw new RuntimeException(e) } }
        } yield Unit

        if (x.isFailure) error(s"Couldn't properly allocate vw model: $VwModelPath")
    }

    // (paste -d '\n' <(jot -b "-1 |" 100) <(jot -b "1 |" 100)) | vw --quiet --link logistic --loss_function logistic -f log_0.5.model
    // echo "" | vw -t --quiet -i log_0.5.model -p pred; cat pred; rm -f ./pred ./log_0.5.model
    // 0.504197
    private[this] def allocateModel() = {
        val m = new VW(s"--quiet --loss_function logistic --link logistic -f $VwModelPath")
        1 to 100 foreach { _ =>
            m.learn("-1 | ")
            m.learn( "1 | ")
        }
        m.close()
    }

    private[this] def logisticModelJson(includeModel: Boolean): JsValue = {
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
