package com.eharmony.aloha.dataset.csv.json

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLines, CsvTypes}
import com.eharmony.aloha.semantics.func.GenAggFunc
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.DoubleJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan on 5/19/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class OptionCsvColumnWithDefaultTest {
  import OptionCsvColumnWithDefaultTest._

  @Test def testNoneWithDefault(): Unit =
    test(Seq(Some(1), Some(1)), "None", Some(1d))

  @Test def testNoneNoDefault(): Unit =
    test(Seq(None, None), "None")

  @Test def testSomeWithDefault(): Unit =
    test(Seq(Some(2), Some(2)), "Some(2)", Some(3))

  @Test def testSomeNoDefault(): Unit =
    test(Seq(Some(4), Some(4)), "Some(4)")

  @Test def testOptDataWithDefault(): Unit =
    test(Seq(Some(1800), Some(9)), "Option(${height_mm})", Option(9))

  @Test def testOptDataNoDefault(): Unit =
    test(Seq(Some(1800), None), "Option(${height_mm})")

  @Test def testReqDataWithDefault(): Unit =
    test(Seq(Some(1800), Some(1650)), "Some(10d) map (${height_cm} * _)", Option(10))

  @Test def testReqDataNoDefault(): Unit =
    test(Seq(Some(1800), Some(1650)), "Some(10d) map (${height_cm} * _)")

  private[this] def test(expected: Seq[Option[Double]], spec: String, defVal: Option[Double] = None): Unit = {
    val col = OptionCsvColumnWithDefault[Double]("name", spec, defVal)
    val f = compileOptFn(semantics, col)
    val y = lines.map(f)
    assertEquals(expected, y)
  }

  /**
    * Compile the function and make it so that if the function returns a `None`, replace the return value
    * with the default.
    * @param s semantics
    * @param c the column to compile
    * @tparam A the input type of the compiled function.
    * @tparam C `Option[C]` is the output type of the compiled function.
    * @return
    */
  private[this] def compileOptFn[A, C](s: CompiledSemantics[A], c: TypedColumnCol[C]): GenAggFunc[A, Option[C]] = {
    s.createFunction[Option[C]](c.wrappedSpec, Some(c.defVal))(c.refInfo).fold(
      errs => throw new RuntimeException(s"Problem compiling function:\n${errs.mkString("\n")}"),
      fn => c match {
        case col: OptionCsvColumnWithDefault[C] => fn.andThenGenAggFunc(_ orElse c.defVal)
        case _ => fn
      }
    )
  }
}

private[json] object OptionCsvColumnWithDefaultTest {
  type TypedColumnCol[A] = CsvColumn { type ColType = A }

  private[this] val features = Seq(
    "height_mm" -> CsvTypes.DoubleOptionType,
    "height_cm" -> CsvTypes.IntType
  )

  private[this] val missing = ""

  // Test height actual data:  height_mm [TAB] height_cm
  val lines = CsvLines(indices = features.unzip._1.zipWithIndex.toMap)(
    "1800\t180",
    s"$missing\t165"
  )

  lazy val plugin = CompiledSemanticsCsvPlugin(features: _*)
  lazy val semantics = CompiledSemantics(TwitterEvalCompiler(), plugin, Nil)
}