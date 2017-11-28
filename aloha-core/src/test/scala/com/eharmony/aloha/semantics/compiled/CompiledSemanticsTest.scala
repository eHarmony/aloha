package com.eharmony.aloha.semantics.compiled

import java.{lang => jl}

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.LazyInitSupport
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

@RunWith(classOf[BlockJUnit4ClassRunner])
class CompiledSemanticsTest {
    private[this] val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

    @Test def test0() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq())
        val f = s.createFunction[Int]("List(${five:-5L}).sum.toInt").right.get
        val x1 = Map("five" -> 1L)
        val x2 = Map.empty[String, Long]
        assertEquals(1, f(x1))
        assertEquals(5, f(x2))
    }

    @Test def test1() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq())
        val f = s.createFunction[Int]("List(${one}, ${two}, ${three}).sum.toInt", Option(Int.MinValue)).right.get
        val x1 = Map[String, Long]("one" -> 2, "two" -> 4, "three" -> 6)
        val y1 = f(x1)
        assertEquals(12, y1)
    }

    @Test def test2() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq())
        val f = s.createFunction[Double]("${user.inboundComm} / ${user.pageViews}.toDouble", Some(Double.NaN)).right.get
        val x1 = Map[String, Long]("user.inboundComm" -> 5, "user.pageViews" -> 10)
        val x2 = Map[String, Long]("user.inboundComm" -> 5)
        val y1 = f(x1)
        val y2 = f(x2)
        assertEquals(0.5, y1, 1.0e-6)
        assertEquals(Double.NaN, y2, 0)
    }

    @Test def test3() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq())
        val f = s.createFunction[Long]("new util.Random(0).nextLong").right.get
        val y1 = f(null)
        assertEquals(-4962768465676381896L, y1)
    }

    @Test def testNullDefaultOnExistingValue() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq("com.eharmony.aloha.semantics.compiled.StaticFuncs._"))
        val f = s.createFunction[Long]("f(${one})").left.map(_.foreach(println)).right.get
        val y1 = f(Map("one" -> 1))
        assertEquals(18, y1)
    }

    /** '''NOTE''': While this is indeed possible, users are strongly advised not to do this!!!
      *
      * Show that we must cast do funky casting but that it is possible to
      1.  make a function that takes a java.lang.Long
      1.  For missing values, force a null value to be passed on to a user defined function (Horrible Idea!)
      */
    @Test
    def testNullDefaultOnMissingPrimitiveValue() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq("com.eharmony.aloha.semantics.compiled.StaticFuncs._"))
        var errors: Seq[String] = Nil
        val f = s.createFunction[Long]("f(${missing:-null}.asInstanceOf[java.lang.Long])").
                    left.map(e => errors = e).
                    right.get
        val y1 = f(Map.empty)
        assertEquals("Should process correctly when defaulting to null", 13, y1)
        assertEquals("No errors should appear", 0, errors.size)
    }

    /** '''NOTE''': While this is indeed possible, users are strongly advised not to do this!!!
      *
      * Show that we must cast do funky casting but that it is possible to
      1.  make a function that takes a java.lang.Long
      1.  For missing values, force a null value to be passed on to a user defined function (Horrible Idea!)
      */
    @Test
    def testNullDefaultOnNonMissingPrimitiveValue() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq("com.eharmony.aloha.semantics.compiled.StaticFuncs._"))
        var errors: Seq[String] = Nil
        val f = s.createFunction[Long]("f(${missing:-null}.asInstanceOf[java.lang.Long])").
            left.map(e => errors = e).
            right.get
        val y1 = f(Map("missing" -> 13))
        assertEquals("Should process correctly when defaulting to null", 18, y1)
        assertEquals("No errors should appear", 0, errors.size)
    }


    @Test
    def testFeatureFunctionsDoNotCallReferencedFunctionsWhenDataIsMissing(): Unit = {
        // According to the semantics, the input type must be an Option.
        val semantics = CompiledSemanticsInstances.anyNameMissingValueSemantics[Option[String]]()

        // Call the `code` function on a variable with missing data.
        val spec = s"${getClass.getCanonicalName}.code" + "(${missingVariable})"

        // The output type of the function is Option[Boolean] for two reasons:
        //   1. the output of the `code` function in the companion object is Boolean
        //   2. the semantics produces a GeneratedAccessor[Option[String]], therefore,
        //      the output of the function must be an Option.  As a consequence of
        //      having optional data, a default must be provided.  This is `Some(None)`
        //      because the default is that when missing data is present, `f` will return
        //      the `None` inside the `Some(None)`.
        val default = Some(None)
        val fAttempt = semantics.createFunction[Option[Boolean]](spec, default)

        // Assert the compilation succeeded.
        assertTrue(s"Compilation failed. Found $fAttempt", fAttempt.isRight)
        val f = fAttempt.right.get

        // Call the function with (missing) data.  It doesn't matter whether the data is
        // missing or not because the semantics should return missing data.
        val x = None
        val y = f(x)
        assertEquals(None, y)

        // The `code` function should not have been called because the data wasn't present
        // (since the semantics deleted it even if it was present).
        val wasCalled = CompiledSemanticsTest.codeWasCalled
        assertFalse(wasCalled)
    }

    private[this] object MapStringLongPlugin extends CompiledSemanticsPlugin[Map[String, Long]] {
        def refInfoA = RefInfo[Map[String, Long]]
        def accessorFunctionCode(spec: String) = {
            val required = Seq("user.inboundComm", "one", "two", "three")
            spec match {
                case s if required contains s  => Right(RequiredAccessorCode(Seq("(_:Map[String, Long]).apply(\"" + spec + "\")")))
                case _                         => Right(OptionalAccessorCode(Seq("(_:Map[String, Long]).get(\"" + spec + "\")")))
            }
        }
    }
}

object CompiledSemanticsTest extends LazyInitSupport

object StaticFuncs {
    def f(a: jl.Long): Long = if (null == a) 13 else 18

    implicit def doubletoJlDouble(d: Double): java.lang.Double = java.lang.Double.valueOf(d)
}
