package com.eharmony.aloha.reflect

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 8/16/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ManifestParserTest {

  import ManifestParserTest._

  @Test def testAnyVals(): Unit = testPredef(AnyVals)

  @Test def testOtherPredef(): Unit = {
    testPredef(OtherPredefs diff Set("AnyRef"))

    // AnyRef Manifests *act like* Object ones but they are not the same under equality.
    assertEquals(s"Right(Object)", ManifestParser.parse("scala.AnyRef").toString)
    assertEquals(s"Right(Object)", ManifestParser.parse("AnyRef").toString)
  }

  private[this] def testPredef(testCases: Traversable[String]): Unit = {
    testCases foreach { v =>
      assertEquals(s"Right($v)", ManifestParser.parse(s"scala.$v").toString)
      assertEquals(s"Right($v)", ManifestParser.parse(s"$v").toString)
    }
  }

  @Test def testDoubleDot(): Unit = assertTrue(ManifestParser.parse("scala..Int").isLeft)

  @Test def testMap(): Unit =
    assertEquals(
      "Right(scala.collection.immutable.Map[Int, Float])",
      ManifestParser.parse("scala.collection.immutable.Map[Int, scala.Float]").toString)

  @Test def testMapBad(): Unit =
    assertTrue(ManifestParser.parse("scala.collection.immutable. Map[Int, scala.Float]").isLeft)

  @Test def testMapBad2(): Unit =
    assertTrue(ManifestParser.parse("scala.collection.immutable. 1 Map[Int, scala.Float]").isLeft)

  @Test def testIterableOfOtherPredefs(): Unit = {
    OtherPredefs.filter(v => v != "AnyRef").foreach { v =>
      val s = s"scala.collection.immutable.Iterable[$v]"
      assertEquals(s"Right($s)", ManifestParser.parse(s).toString)
    }

    assertEquals(
      "Right(scala.collection.immutable.Iterable[Object])",
      ManifestParser.parse("scala.collection.immutable.Iterable[AnyRef]").toString)
  }

  @Test def testArray(): Unit =
    Seq("scala.Array[Float]", "Array[scala.Float]").foreach { in =>
      ManifestParser.parse(in) match {
        case Right(s) => assertEquals(s"For '$in'", "float[]", s.runtimeClass.getCanonicalName)
        case Left(m) => fail(s"Failed to parse '$in'. Error: $m")
      }
    }

  @Test def testNestedArray(): Unit =
    Seq("scala.Array[Array[Float]]", "Array[scala.Array[scala.Float]]").foreach { in =>
      ManifestParser.parse(in) match {
        case Right(s) => assertEquals(s"For '$in'", "float[][]", s.runtimeClass.getCanonicalName)
        case Left(m) => fail(s"Failed to parse '$in'. Error: $m")
      }
    }

  @Test def testObjectEqualsAnyRef(): Unit = {
    val o = ManifestParser.parse("scala.collection.immutable.Iterable[Object]")
    assertTrue(o.isRight)
    assertEquals(o, ManifestParser.parse("scala.collection.immutable.Iterable[AnyRef]"))
  }

  @Test def testAnyValUnequalToAnyRef(): Unit = {
    val v = ManifestParser.parse("scala.collection.immutable.Iterable[AnyVal]")
    val r = ManifestParser.parse("scala.collection.immutable.Iterable[AnyRef]")

    (v, r) match {
      case (Right(vs), Right(rs)) => assertNotEquals(vs, rs)
      case (Left(m), _)           => fail(s"Parse failure on Iterable[AnyVal]. Failure: $m")
      case (_, Left(m))           => fail(s"Parse failure on Iterable[AnyVar]. Failure: $m")
    }
  }

  @Test def testClassManifest(): Unit =
    assertEquals(manifest[String], ManifestParser.classManifest("java.lang.String"))

  @Test def testArrayManifest(): Unit = {
    val s = ManifestParser.classManifest("java.lang.String")
    assertEquals(manifest[String].arrayManifest, ManifestParser.arrayManifest(s))
  }

  @Test def testParameterizedManifest(): Unit = {
    val s = ManifestParser.classManifest("java.lang.String")
    val m = ManifestParser.parameterizedManifest("scala.collection.Iterable", Seq(s))
    assertEquals(manifest[Iterable[String]], m)
  }
}

object ManifestParserTest {
  val AnyVals = Set("Boolean", "Byte", "Char", "Double", "Float", "Int", "Long", "Short", "Unit")
  val OtherPredefs = Set("Object", "Any", "AnyVal", "AnyRef", "Nothing")
}
