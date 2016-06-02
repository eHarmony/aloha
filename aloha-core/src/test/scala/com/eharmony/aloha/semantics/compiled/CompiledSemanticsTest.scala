package com.eharmony.aloha.semantics.compiled

import concurrent.ExecutionContext.Implicits.global

import java.{lang => jl}

import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.reflect.RefInfo
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class CompiledSemanticsTest {
    private[this] val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

    @Test def test0() {
        val s = CompiledSemantics(compiler, MapStringLongPlugin, Seq())
        val f = s.createFunction[Int]("List(${five:-5L}).sum.toInt").right.get
        val x1 = Map("five" -> 1L)
        val x2 = Map.empty[String, Long]
        val y1 = f(x1)
        val y2 = f(x2)
        val a = 1
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
        val a = 1
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

    private[this] object MapStringJLongPlugin extends CompiledSemanticsPlugin[Map[String, jl.Long]] {
        def refInfoA = RefInfo[Map[String, jl.Long]]
        def accessorFunctionCode(spec: String) = {
            val required = Seq("user.inboundComm", "one", "two", "three")
            spec match {
                case s if required contains s  => Right(RequiredAccessorCode(Seq("(_:Map[String, java.lang.Long]).apply(\"" + spec + "\")")))
                case _                         => Right(OptionalAccessorCode(Seq("(_:Map[String, java.lang.Long]).get(\"" + spec + "\")")))
            }
        }
    }
}

object StaticFuncs {
    def f(a: jl.Long): Long = if (null == a) 13 else 18

    implicit def doubletoJlDouble(d: Double) = java.lang.Double.valueOf(d)
}
