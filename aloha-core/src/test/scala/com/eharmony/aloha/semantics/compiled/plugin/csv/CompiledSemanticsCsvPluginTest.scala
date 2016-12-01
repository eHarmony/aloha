package com.eharmony.aloha.semantics.compiled.plugin.csv

import com.eharmony.aloha.util.Logging

import scala.concurrent.ExecutionContext.Implicits.global

import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Test
import org.junit.Assert._

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.FileLocations
import scala.collection.immutable.ListMap

@RunWith(classOf[BlockJUnit4ClassRunner])
class CompiledSemanticsCsvPluginTest {
    import CompiledSemanticsCsvPluginTest._

    @Test def testOptBooleanFunctions() {
        val default = Option(None)
        val f = semantics.createFunction[Option[Boolean]]("${optional.int} < 0", default).right.get

        val x1 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "optional.int" -> "-1")
        val y1 = f(x1)
        assertEquals(Option(true), y1)

        val x2 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "optional.int" -> "0")
        val y2 = f(x2)
        assertEquals(Option(false), y2)

        val x3 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "optional.int" -> "1")
        val y3 = f(x3)
        assertEquals(Option(false), y3)

        val x4 = CsvLineTest.getCsvLine(CsvLineTest.csvLines)
        val y4 = f(x4)
        assertEquals(None, y4)
    }

    @Test def testReqLongFunctions() {
        val f = semantics.createFunction[Long]("math.signum(${required.long})").right.get

        val x1 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "required.long" -> Long.MinValue.toString)
        val y1 = f(x1)
        assertEquals(-1l, y1)

        val x2 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "required.long" -> "0")
        val y2 = f(x2)
        assertEquals(0l, y2)

        val x3 = CsvLineTest.getCsvLine(CsvLineTest.csvLines, "required.long" -> Long.MaxValue.toString)
        val y3 = f(x3)
        assertEquals(1l, y3)
    }

    /** Tests that we can properly pull out multiple variables of multiple types and that missing data is handled
      * properly in the required and optional cases.
      */
    @Test def testOptBoolReqLongFunctions() {
        val default = Option(None)
        val f = semantics.createFunction[Option[Boolean]]("Long.MaxValue == ${required.long} || ${optional.int} < 0", default).right.get

        // NOTE: Left indicates failure, Right is a proper function result.

        // required long values
        val l = ListMap(
            Option(1.toString)             -> Right(Option(false)),
            Option(Long.MaxValue.toString) -> Right(Option(true)),
            None                           -> Left(())                // Failure
        )

        // optional int values
        val oi = ListMap[Option[String], Either[Unit, Option[Boolean]]](
            Option(1.toString)             -> Right(Option(false)),
            Option((-1).toString)          -> Right(Option(true)),
            None                           -> Right(None)
        )

        for {
            (lv, lr) <- l
            (oiv, oir) <- oi
            lvp = lv.map(v => "required.long" -> v)
            oivp = oiv.map(v => "optional.int" -> v)

            // The CSV line that is fed through the function, f.
            x = CsvLineTest.getCsvLine(CsvLineTest.csvLines, Seq(lvp, oivp).flatten:_*)

            // The expected value.
            exp = lr.right.flatMap(lro => oir.right.flatMap(oiro => Right(lro.flatMap(lrv => oiro.map(_ || lrv)))))


            // The actual value.  On throwing exception, output failure instead.
            act = try { Right(f(x)) } catch { case e: Exception => Left(()) }
        } {
            if (Seq(lvp, oivp).flatten.toString == "List((required.long,1), (optional.int,1))") {
                val y = CsvLineTest.getCsvLine(CsvLineTest.csvLines, Seq(lvp, oivp).flatten:_*)

            }

            assertEquals(exp, act)
        }
    }
}

private object CompiledSemanticsCsvPluginTest extends Logging {
    val csvPlugin = {
        val m = CsvLineTest.types.toMap
        val c = m.map{case(k, v) => debug(s"$k\t$v"); (k, CsvTypes.withNameExtended(v))}
        new CompiledSemanticsCsvPlugin(c)
    }
    val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
    val semantics = CompiledSemantics(compiler, csvPlugin, Seq("com.eharmony.aloha.feature.BasicFunctions._"))
}
