package com.eharmony.matching.aloha.semantics.compiled

import concurrent.ExecutionContext.Implicits.global

import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.reflect.RefInfo

@RunWith(classOf[JUnit4ClassRunner])
class CompiledSemanticsTest {
    private[this] val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

    @Test def test0() {
        val s = CompiledSemantics(compiler, Plugin, Seq())
        val f = s.createFunction[Int]("List(${five:-5L}).sum.toInt").right.get
        val x1 = Map("five" -> 1L)
        val x2 = Map.empty[String, Long]
        val y1 = f(x1)
        val y2 = f(x2)
        val a = 1
    }

    @Test def test1() {
        val s = CompiledSemantics(compiler, Plugin, Seq())
        val f = s.createFunction[Int]("List(${one}, ${two}, ${three}).sum.toInt", Option(Int.MinValue)).right.get
        val x1 = Map[String, Long]("one" -> 2, "two" -> 4, "three" -> 6)
        val y1 = f(x1)
        assertEquals(12, y1)
    }

    @Test def test2() {
        val s = CompiledSemantics(compiler, Plugin, Seq())
        val f = s.createFunction[Double]("${user.inboundComm} / ${user.pageViews}.toDouble", Some(Double.NaN)).right.get
        val x1 = Map[String, Long]("user.inboundComm" -> 5, "user.pageViews" -> 10)
        val x2 = Map[String, Long]("user.inboundComm" -> 5)
        val y1 = f(x1)
        val y2 = f(x2)
        assertEquals(0.5, y1, 1.0e-6)
        assertEquals(Double.NaN, y2, 0)
    }

    @Test def test3() {
        val s = CompiledSemantics(compiler, Plugin, Seq())
        val f = s.createFunction[Long]("new util.Random(0).nextLong").right.get
        val y1 = f(null)
        assertEquals(-4962768465676381896L, y1)
    }

    private[this] object Plugin extends CompiledSemanticsPlugin[Map[String, Long]] {
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
