package com.eharmony.aloha.models.reg

import java.io.File

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, Enum}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.util.Try

/**
 * A test to show a walk through of the process of creating a CSV-based model.  Note that in this example, only
 * "profile.user_id" is used in the model.  Therefore, the other fields could be omitted in the columnTypes
 * and csvLines objects.
 */
object FizzBuzzCsvRegModelTest {

  import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvTypes._

  // #1 Define the data field names and types.
  // Vector because we need to retain order for the indices map in CsvLines.
  val columnTypes = Vector(
    "profile.user_id"         -> IntType,
    "user.name"               -> EnumType,
    "user.isMale"             -> BooleanType,
    "user.age"                -> IntType,
    "user.birthdate.unixtime" -> LongType,
    "user.face.ratio"         -> FloatType,
    "user.xyz"                -> DoubleType,
    "user.name.last"          -> StringType
  )

  // #2 Create the semantics
  val csvSemantics: CompiledSemantics[CsvLine] = {

    // Order doesn't matter here.  That's why a Map is OK.
    val csvPlugin = CompiledSemanticsCsvPlugin(columnTypes.toMap)

    // imports allow the model DSL to change.
    val imports = Seq("scala.math._", "com.eharmony.aloha.feature.BasicFunctions._")

    // Function creation in Aloha is expensive because it has to sythesize and compile the
    // functions using scalac, which is notoriously slow.  We provide a cache directory so
    // that we don't have to recompile on every new JVM invocation.
    val cacheDir: File = FileLocations.testGeneratedClasses
    val compiler = TwitterEvalCompiler(classCacheDir = Option(cacheDir))

    // Need to import ExecutionContext
    import scala.concurrent.ExecutionContext.Implicits.global
    CompiledSemantics(compiler, csvPlugin, imports)
  }


  // #3 Create the factory
  // Need to import a spray JsonFormat and a ScoreConverter for the output type of the model (Double).
//  val factory: TypedModelFactory[CsvLine, Double] = ModelFactory.defaultFactory.toTypedFactory[CsvLine, Double](csvSemantics)
  val factory = NewModelFactory.defaultFactory(csvSemantics, OptionAuditor[Double]())


  // #4 Use the factory to instantiate the model.
  val modelTry: Try[Model[CsvLine, Option[Double]]] = factory.fromResource("fizzbuzz.json")
  val model = modelTry.get


  // #5 Define the structure of the data.
  // CsvLines is the object description responsible for turning a string into a CsvLine.
  // fs, ifs, missingData, errorOnOptMissingField and errorOnOptMissingEnum are already the same as the
  // default values and so could be omitted.
  //
  //   CsvLines(indices = columnTypes.unzip._1.zipWithIndex.toMap,
  //            enums = Map("user.name" -> Enum("com.eh.Names", "Bill" -> 1, "James" -> 2, "Dan" -> 4)))
  //
  val csvLines = CsvLines(indices = columnTypes.unzip._1.zipWithIndex.toMap,
                          enums = Map("user.name" -> Enum("com.eh.Names", "Bill" -> 1, "James" -> 2, "Dan" -> 4)),
                          fs = "\t",
                          ifs = ",",
                          missingData = _ == "",
                          errorOnOptMissingField = false,
                          errorOnOptMissingEnum = false)


  // #6 Some data to test
  val data = {
    val range = 1 to 20
    val otherFields = "Bill,true,13,1417845974683,1.2,-1.7976931348623157,Jones"
    range.zip(range.iterator.zip(Iterator continually otherFields).map{case(i, s) => s"$i,$s".replaceAll(",", "\t")}.toSeq)
  }
}

/**
 * Created by deak on 9/6/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class FizzBuzzCsvRegModelTest {
  import FizzBuzzCsvRegModelTest.{csvLines, data, model}

  /**
   * Transform raw string data one at a time inside the loop.
   */
  @Test def testFizzBuzzOneAtATime(): Unit = {
    data.foreach { case (i, s) =>
      val in = csvLines(s)
      val scoreOpt: Option[Double] = model(in)
      assertEquals(s"$i", expected(i), scoreOpt.get, 0)
    }
  }

  /**
   * Transform raw string data all at once.
   */
  @Test def testFizzBuzzInSeqEager(): Unit = {
    val (uids, csvs) = data.unzip

    uids.zip(csvLines(csvs)).foreach { case (i, c) =>
      assertEquals(s"$i", expected(i), model(c).get, 0)
    }
  }

  /**
   * Transform raw string data all at once in a non-strict (lazy) way.
   */
  @Test def testFizzBuzzInSeqNonStrict(): Unit = {
    val (uids, csvs) = data.unzip
    val lines = csvLines.nonStrict(csvs) // Not transformed at this point in time.
    uids.zip(lines.toIterable).foreach { case (i, c) =>
      assertEquals(s"$i", expected(i), model(c).get, 0)
    }
  }

  def expected(n: Int) = n match {
    case x if 0 == x % 3 && 0 == x % 5 => -6.0
    case x if 0 == x % 3               => -2.0
    case x if 0 == x % 5               => -4.0
    case x                             => x.toDouble
  }
}
