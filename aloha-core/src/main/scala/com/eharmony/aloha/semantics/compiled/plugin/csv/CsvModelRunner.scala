package com.eharmony.aloha.semantics.compiled.plugin.csv

import java.io.{File, InputStream, OutputStream, PrintStream}
import java.util.regex.Matcher

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.audit.impl.scoreproto.ScoreAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.compiled.{CompiledSemantics, CompiledSemanticsPlugin}
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.util.Timing
import com.google.protobuf.GeneratedMessage
import org.apache.commons.codec.binary.Base64

import scala.collection.parallel.immutable.ParVector
import org.apache.commons.vfs2.{FileObject, VFS}
import spray.json.pimpString

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.util.parsing.combinator.RegexParsers

sealed trait InputType

case class ProtoInputType(protoClass: String) extends InputType {
    /**
     * This let's classes be specified to the API like java classes (a.b.c.Outer$Inner)
     * or scala classes (a.b.c.Outer.Inner).
     * @param className class name for which we are trying to get a Class instance.
     * @return
     */
    @tailrec private[this] def attemptToGetClass[A](className: String): Option[Class[A]] = {
        if (-1 == className.indexOf(".")) None
        else Try { Class.forName(className).asInstanceOf[Class[A]] } match {
            case Success(c) => Option(c)
            case Failure(_) => attemptToGetClass(className.reverse.replaceFirst("\\.", Matcher.quoteReplacement("$")).reverse)
        }
    }

    def getProtoPluginAndExtractorFunction[A <: GeneratedMessage]: (CompiledSemanticsProtoPlugin[A], (String) => A, RefInfo[A]) = {
        // The getOrElse is to throw the ClassNotFoundException with the original class name.
        val c = attemptToGetClass[A](protoClass) getOrElse Class.forName(protoClass).asInstanceOf[Class[A]]
        val m = c.getMethod("parseFrom", classOf[Array[Byte]])

        val f: String => A = s => m.invoke(null, Base64.decodeBase64(s)).asInstanceOf[A]
        val plugin = new CompiledSemanticsProtoPlugin[A](c)
        (plugin, f, RefInfoOps.fromSimpleClass(c))
    }
}

sealed trait CsvInputType extends InputType {
    def csvPluginAndLines: (CompiledSemanticsCsvPlugin, CsvLines)
}

case class FileBasedCsvInputType(csvDef: FileObject) extends CsvInputType {
    def csvPluginAndLines: (CompiledSemanticsCsvPlugin, CsvLines) = {
        val c = CsvProtocol.getCsvDataRetriever(StringReadable.fromVfs2(csvDef).parseJson)
        (c.plugin, c.csvLines)
    }
}

sealed trait MissingFunction extends (String => Boolean)
case object EmptyStringMissingFunction extends MissingFunction {
    def apply(s: String): Boolean = s.isEmpty
}

case class RegexMissingFunction(regex: String) extends MissingFunction {
    def apply(s: String): Boolean = s matches regex
}

case class InlineCsvInputType(
        colNamesToTypes: Seq[(String, CsvTypes.CsvType)] = Vector.empty,
        fieldIndices: Seq[(String, Int)] = Vector.empty,
        enums: Seq[(String, Enum)] = Vector.empty,
        colNameToEnumName: Seq[(String, String)] = Vector.empty,
        separator: String = "\t",
        intraFieldSeparator: String = ",",
        missing: String = "",
        errorOnOptMissingField: Boolean = false,
        errorOnOptMissingEnum: Boolean = false) extends CsvInputType {

    def csvPluginAndLines: (CompiledSemanticsCsvPlugin, CsvLines) = {
        val missingFunction: MissingFunction = if (missing.isEmpty) EmptyStringMissingFunction else RegexMissingFunction(missing)
        val plugin = CompiledSemanticsCsvPlugin(colNamesToTypes.toMap)

        val enumClassMap = enums.toMap
        val enumMap = colNameToEnumName.map{ case (name, className) => name -> enumClassMap(className) }.toMap

        val csvLines = CsvLines(fieldIndices.toMap,
                                enumMap,
                                separator,
                                intraFieldSeparator,
                                missingFunction,
                                errorOnOptMissingField,
                                errorOnOptMissingEnum)
        (plugin, csvLines)
    }

    def validate: Either[String, Unit] = {
        lazy val colNameDups = dupKeys(colNamesToTypes) ++ dupKeys(colNameToEnumName) ++ dupKeys(fieldIndices)
        lazy val enumDups = dupKeys(enums)
        lazy val enumClasses = enums.map(_._1).toSet
        lazy val noEnumAvailable = colNameToEnumName filterNot { case(c, e) => enumClasses contains e }

        if (missing == separator) Left("missing string == separator: \"${missing}\"")
        else if (missing == intraFieldSeparator) Left(s"""missing string == intra-field separator: "$missing".""")
        else if (separator == intraFieldSeparator) Left(s"""separator == intra-field separator: "$separator".""")
        else if (colNameDups.nonEmpty) Left(s"duplicate column names: ${colNameDups.mkString(",")}.")
        else if (enumDups.nonEmpty) Left(s"duplicate enum classes: ${enumDups.mkString(",")}.")
        else if (noEnumAvailable.nonEmpty) Left(s"The following columns refer to non existent enums: ${noEnumAvailable.map(_._1).mkString(",")}")
        else Right(())
    }

    private[csv] def dupKeys[A, B](it: Iterable[(A, B)]) = it.groupBy(_._1).collect{ case (k, v) if 1 < v.size => k }
}


private[csv] sealed trait EnumGen {
    def withClassName(canonicalClassName: String): Enum
}

private[csv] case class WithNums(constants: Seq[(String, Int)]) extends EnumGen {
    def withClassName(canonicalClassName: String) = Enum(canonicalClassName, constants:_*)
}

private[csv] case class NoNums(constants: Seq[String]) extends EnumGen {
    def withClassName(canonicalClassName: String): Enum = Enum.withNoNumbers(canonicalClassName, constants:_*)
}

/** An enum parser.
  *
  */
private[csv] object EnumParser extends RegexParsers {
    //        private[this] val canonicalClassName: Parser[String] = """([a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)*\.)*[A-Z_][A-Za-z0-9_]*""".r
    private[this] lazy val identifier: Parser[String] = """[A-Za-z_][A-Za-z0-9_]*""".r
    private[this] lazy val separator: Parser[String] = ","
    private[this] lazy val kvSeparator: Parser[String] = ":"
    private[this] lazy val noNumbers = rep1sep(identifier, separator) ^^ { NoNums(_) }
    private[this] lazy val int: Parser[Int] = """0|(-?[1-9][0-9]*)""".r.flatMap { case n =>
        try {
            success(n.toInt)
        } catch {
            case e: NumberFormatException => failure(e.getMessage)
        }
    }
    private[this] lazy val idAndNum: Parser[(String, Int)] = identifier ~ (kvSeparator ~> int) ^^ { case id ~ n => (id, n) }
    private[this] lazy val numbers = rep1sep(idAndNum, separator) ^^ { WithNums(_) }

    // Order matters since there is no backtracking.  numbers needs to appear first.
    lazy val root: Parser[EnumGen] = numbers | noNumbers

    def failureMsg(s: String): Option[String] = parseAll(root, s) match {
        case f: NoSuccess => Option(f.msg)
        case _ => None
    }

    def getEnum(canonicalClassName: String, body: String): Either[String, Enum] = parseAll(root, body) match {
        case Success(enumGen, _) => Right(enumGen.withClassName(canonicalClassName))
        case failure: NoSuccess => Left(failure.msg)
    }
}

object InputPosition extends Enumeration {
    type InputPosition = Value
    val Neither, Before, After, Both = Value
}

object OutputType extends Enumeration {
    type OutputType = Value
    implicit val BooleanType, ByteType, ShortType, IntType, LongType, StringType, FloatType, DoubleType = Value
}

import com.eharmony.aloha.semantics.compiled.plugin.csv.InputPosition._

sealed trait PredictionOutputFormat {
    def run(): Unit
}

case class ModelPredictionOutput(
    out: OutputStream,
    closeOut: Boolean,
    predictionFn: (String => Option[Any]),
    in: Iterator[String],
    inputPosInOutput: InputPosition,
    outputSep: String,
    predictionMissing: String
) extends PredictionOutputFormat {
    def run(): Unit = {
        try {
            val pStream = new PrintStream(out)

            // Create a writing function.  The first function parameter is the input, the second is the model output.
            val writingFunc: (String, String) => String = inputPosInOutput match {
                case InputPosition.Neither => (i, o) => o
                case InputPosition.Before => (i, o) => s"$i$outputSep$o"
                case InputPosition.After => (i, o) => s"$o$outputSep$i"
                case InputPosition.Both => throw new IllegalStateException("Can't output input before and after output.")
            }

            in.foreach{ line => pStream.println(writingFunc(line, predictionFn(line).fold(predictionMissing)(_.toString))) }
        }
        finally {
            if (closeOut)
                org.apache.commons.io.IOUtils.closeQuietly(out)
        }
    }
}

case class LoadTestOutput(
    out: OutputStream,
    closeOut: Boolean,
    in: Iterator[String],
    inputFn: (String => Any),
//    model: Model[Any, Any],
    model: Any => Option[Any],
    outputSep: String,
    ltConf: LoadTestConfig
) extends PredictionOutputFormat with Timing {

    def run(): Unit = {
        val input = correctSizedInput()
        val inputSize = input.size
        val models = ParVector.fill(ltConf.threads)(model)

        var loops = 0
        var pred = 0L
        var nonEmpty = 0L

        try {
            val pStream = new PrintStream(out)

            // Clean up before calculating and reporting on memory usage.
            1 to 5 foreach { _ => System.gc() }

            pStream.println(header)
            pStream.println(report(0, 0, 0, Float.MinPositiveValue, inputSize, models.size))
            pStream.flush()

            val score = (a: Any) => model(a).fold(0)(_ => 1)


            while (loops < ltConf.loops) {
                // TODO: This looks like a bug.  `m` is never used.
                val (ne, intTime) = time(models.map(m => input.foldLeft(0)((s, x) => s + score(x))).sum)
                nonEmpty += ne
                pred += input.size * models.size
                loops += 1

                if (loops % ltConf.reportLoopMultiple == 0) {
                    pStream.println(report(loops, pred, nonEmpty, intTime, inputSize, models.size))
                    pStream.flush() // Flush because these might take a long time and the output is small.
                }
            }
        }
        finally {
            if (closeOut)
                org.apache.commons.io.IOUtils.closeQuietly(out)
        }
    }

    private[this] def header = {
        val t = outputSep
        s"loop_number${t}pred_sec${t}running_pred${t}running_nonempty_pred${t}mem_used_mb${t}mem_unallocated_mb${t}unixtime_ns"
    }

    private[this] def report(loops: Int, pred: Long, nonEmpty: Long, intervalTime: Float, intervalSize: Int, models: Int): String = {
        val time = System.nanoTime()
        val t = outputSep
        val predSec = intervalSize * models / intervalTime
        val (usedMB, unallocMB) = memStatsMb()
        s"$loops$t$predSec$t$pred$t$nonEmpty$t$usedMB$t$unallocMB$t$time"
    }

    private[this] def memStatsMb() = {
        val r = Runtime.getRuntime
        val tot = r.totalMemory()
        val free = r.freeMemory()
        val max = r.maxMemory()
        val used = tot - free
        val unalloc = max - used
        (used / (1 << 20).toFloat, unalloc / (1 << 20).toFloat)
    }

    private[this] def correctSizedInput() = {
        val input = in.take(ltConf.predictionsPerLoop).map(inputFn).toVector
        val s = input.size
        if (s >= ltConf.predictionsPerLoop) input
        else {
            val n = math.ceil(ltConf.predictionsPerLoop.toDouble / s).toInt
            Vector.fill(n)(input).flatten.take(ltConf.predictionsPerLoop)
        }
    }
}

case class LoadTestConfig(
    loops: Long = Long.MaxValue,
    threads: Int = Runtime.getRuntime.availableProcessors(),
    predictionsPerLoop: Int = 100,
    reportLoopMultiple: Int = 10,
    useScoreObjects: Boolean = false
)

case class CsvModelRunnerConfig(
    inputPosInOutput: InputPosition = Neither,
    imports: Seq[String] = Vector.empty,
    headersInOutput: Boolean = false,
    classCacheDir: Option[File] = None,
    model: Option[FileObject] = None,
    inputFile: Option[String] = None,
    outputFile: Option[String] = None,
    outputType: OutputType.Value = OutputType.IntType,
    inputType: Either[Unit, Option[InputType]] = Right(None),
    outputSep: String = "\t",
    predictionMissing: String = "",
    loadTest: Option[LoadTestConfig] = None) {

    def validate: Either[String, Unit] = {
        if (inputPosInOutput == Both)                   Left("Cannot place input both before and after the model output in the output.")
        else if (model.isEmpty)                         Left("no model defined")
        else inputType match {
            case Right(Some(in: InlineCsvInputType)) => in.validate
            case Right(None)                         => Left("No input format information given.  Use -p or -c or inline CSV options.")
            case Left(())                            => Left("Multiple input format information given.")
            case _                                   => Right(())
        }
    }
}

object CsvModelRunnerConfig {
    def updateInlineCsv(reportError: String => Unit, flag: String, c: CsvModelRunnerConfig)
                       (lens: (InlineCsvInputType) => InlineCsvInputType): CsvModelRunnerConfig = {
        c.inputType match {
            case Right(Some(_: ProtoInputType))             => c.copy(inputType = Left(reportError(s"Inline CSV definition '$flag' not allowed once -p is specified.")))
            case Right(Some(_: FileBasedCsvInputType))      => c.copy(inputType = Left(reportError(s"Inline CSV definition '$flag' not allowed once -c is specified.")))
            case Right(Some(csvConfig: InlineCsvInputType)) => c.copy(inputType = Right(Option(lens(csvConfig))))
            case Right(None)                                => c.copy(inputType = Right(Option(lens(InlineCsvInputType()))))
            case Left(())                                   => c
        }
    }

    def updateInlineCsvCol(reportError: String => Unit, c: CsvModelRunnerConfig, cName: String, tpe: CsvTypes.CsvType): CsvModelRunnerConfig =
        updateInlineCsv(reportError, "--" + tpe, c){ csv =>
            csv.copy(
                colNamesToTypes = csv.colNamesToTypes :+ (cName -> tpe),
                fieldIndices    = csv.fieldIndices    :+ (cName -> csv.colNamesToTypes.size)
            )
        }

    def updateInlineCsvEnum(reportError: String => Unit, c: CsvModelRunnerConfig, cName: String, eName: String, tpe: CsvTypes.CsvType): CsvModelRunnerConfig =
        updateInlineCsv(reportError, "--" + tpe, c){ csv =>
            csv.copy(
                colNameToEnumName = csv.colNameToEnumName :+ (cName -> eName),
                colNamesToTypes   = csv.colNamesToTypes   :+ (cName -> tpe),
                fieldIndices      = csv.fieldIndices      :+ (cName -> csv.colNamesToTypes.size)
            )
        }

    def updateLoadTest(c: CsvModelRunnerConfig)(transform: LoadTestConfig => LoadTestConfig): CsvModelRunnerConfig = {
        val loadTest = transform(c.loadTest.getOrElse(LoadTestConfig()))
        c.copy(loadTest = Option(loadTest))
    }

    private[this] implicit val vfs2FoRead = scopt.Read.reads(VFS.getManager.resolveFile)

    val parser = new scopt.OptionParser[CsvModelRunnerConfig]("model-runner-tool") {
        head("model-runner-tool", aloha.version)

        arg[FileObject]("<model>") required() action { (m, c) =>
            c.copy(model = Some(m))
        } text "Apache VFS URL to model file."

        // vvvvv   Load test options   vvvvv

        opt[Long]("lt-loops") action { (loops, c) =>
            updateLoadTest(c)(_.copy(loops = loops))
        } validate { loops =>
            if (loops >= 1) Right(())
            else Left(s"load-test-loops must be at least one.  Found $loops.")
        } text "Run a load test with the number of specified loops over the data."

        opt[Int]("lt-threads") action { (threads, c) =>
            updateLoadTest(c)(_.copy(threads = threads))
        } validate { threads =>
            val p = Runtime.getRuntime.availableProcessors
            if (1 <= threads && threads <= p) Right(())
            else Left(s"threads must be between 1 and $p.  Found $threads.")
        } text "Run a load test with the specified number of threads."

        opt[Int]("lt-pred-per-loop") action { (pred, c) =>
            updateLoadTest(c)(_.copy(predictionsPerLoop = pred))
        } validate { pred =>
            if (pred >= 100) Right(())
            else Left(s"Predictions per round must be at least 100.  Found $pred.")
        } text "Run a load test with the number of predictions per loop."

        opt[Int]("lt-report-loop-multiple") action { (mult, c) =>
            updateLoadTest(c)(_.copy(reportLoopMultiple = mult))
        } validate { mult =>
            if (mult >= 1) Right(())
            else Left(s"report loop multiple must be positive.  Found $mult.")
        } text "Run a load test, reporting statistics every [arg] loops."

        opt[Boolean]("lt-use-score-objects") action { (useObjs, c) =>
            updateLoadTest(c)(_.copy(useScoreObjects = useObjs))
        } text "For load test, use score objects if true, otherwise use score primitives. Default = false."

        // ^^^^^   Load test options   ^^^^^

        opt[Unit]('A', "after") action { (_, c) =>
            c.copy(inputPosInOutput = InputPosition(c.inputPosInOutput.id | After.id))
        } text "Include the input AFTER the model output in the program output."

        opt[Unit]('B', "before") action { (_, c) =>
            c.copy(inputPosInOutput = InputPosition(c.inputPosInOutput.id | Before.id))
        } text "Include the input BEFORE the model output in the program output."

        opt[File]('C', "cache") action { case (f, c) if f.isDirectory =>
            c.copy(classCacheDir = Option(f))
        } text "Directory to cache generated code.  Makes rerunning faster."

        opt[String]('p', "proto-input") action { (protoClass, c) =>
            // B/c maxOccurs = 1, non-empty must come from another input type.
            c.inputType match {
                case Right(None) => c.copy(inputType = Right(Option(ProtoInputType(protoClass))))
                case Right(Some(_: ProtoInputType)) => c.copy(inputType = Left(reportError("-p already provided.")))
                case Right(Some(_: InlineCsvInputType)) => c.copy(inputType = Left(reportError("-p cannot be provided with inline CSV options.")))
                case Right(Some(_: FileBasedCsvInputType)) => c.copy(inputType = Left(reportError("-p cannot be provided with -c option.")))
                case Left(()) => c
            }
        } text "canonical class name of the protocol buffer type to use." maxOccurs (1)

        opt[FileObject]('c', "csv-input") action { (csvJsonFile, c) =>
            // B/c maxOccurs = 1, non-empty must come from another input type.
            c.inputType match {
                case Right(None) => c.copy(inputType = Right(Option(FileBasedCsvInputType(csvJsonFile))))
                case Right(Some(_: ProtoInputType)) => c.copy(inputType = Left(reportError("-c cannot be provided with -p option.")))
                case Right(Some(_: InlineCsvInputType)) => c.copy(inputType = Left(reportError("-c cannot be provided with inline CSV options.")))
                case Right(Some(_: FileBasedCsvInputType)) => c.copy(inputType = Left(reportError("-c already provided.")))
                case Left(()) => c
            }
        } text "Apache VFS URL to JSON file specifying the structure of the CSV input." maxOccurs (1)

        opt[(String, String)]('E', "enum-def") action { case ((eName, eBody), c) =>
            // Try to parse.  Upon failure, return case config that was passed in.  Let the validation deal with
            // any issues.
            updateInlineCsv(reportError, "-E", c){ csv =>
                EnumParser.getEnum(eName, eBody).fold(
                    _ => csv,
                    e => csv.copy(enums = csv.enums :+ (eName -> e)))
            }
        } validate { case (eName, eBody) =>
            EnumParser failureMsg eBody map (e => failure(s"for '$eBody', found error: $e")) getOrElse success
        } unbounded() optional() text "an enum definition: canonical class name, enum definition."

        opt[Unit]('H', "headers") action { (_, c) =>
            c.copy(headersInOutput = true)
        } text "Include the headers in the output."

        opt[String]('I', "imports") action { (imp, c) =>
            c.copy(imports = imp.split("""\s*,\s*""").map(_.trim))
        } text "Comma-delimited list of imports."

        opt[Unit]("err-on-missing-optional-field") action { (_, c) =>
            updateInlineCsv(reportError, "--err-on-missing-optional-field", c){ _.copy(errorOnOptMissingField = true) }
        } text "Produce an error when an optional field is requested for a non-existent column name."

        opt[Unit]("err-on-missing-optional-enum") action { (_, c) =>
            updateInlineCsv(reportError, "--err-on-missing-optional-enum", c){ _.copy(errorOnOptMissingEnum = true) }
        } text "Produce an error when an optional enum field is request for a column not associated with any enum."

        opt[String]("ifs") action { (ifs, c) =>
            updateInlineCsv(reportError, "--ifs", c){ csv => csv.copy(intraFieldSeparator = ifs) }
        } text "intra-field separator string"

        opt[String]("input-file") action { (f, c) =>
            c.copy(inputFile = Option(f))
        } text "Apache VFS URL for input.  If not specified, data will come from STDIN."

        opt[String]("missing") action { (m, c) =>
            updateInlineCsv(reportError, "--" + "missing", c){ csv => csv.copy(missing = m) }
        } text "string indicating missing value"

        opt[String]("output-type") action { (ot, c) =>
            c.copy(outputType = OutputType.withName(s"${ot}Type"))
        } text "Model score output type.  One of { Boolean, Byte, Double, Float, Int, Long, Short, String }"

        opt[String]("output-file") action { (f, c) =>
            c.copy(outputFile = Option(f))
        } text "Apache VFS URL for output.  If not specified, data will go to STDOUT."

        opt[String]("sep") action { (s, c) =>
            updateInlineCsv(reportError, "--sep", c){ csv => csv.copy(separator = s) }
        } text "column delimiter string"

        opt[String]("outsep") action { (s, c) =>
            c.copy(outputSep = s)
        } text "column delimiter string for output"

        opt[String]("predmissing") action { (s, c) =>
            c.copy(predictionMissing = s)
        } text "string to output when prediction is missing"

        opt[(String, String)]("Enum") abbr CsvTypes.EnumType.toString action { case ((cName, eName), c) =>
            updateInlineCsvEnum(reportError, c, cName, eName, CsvTypes.EnumType)
        } unbounded() text "an enum column: [column name]=[enum canonical class name]"

        opt[String]("Boolean") abbr CsvTypes.BooleanType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.BooleanType)
        } unbounded() text "an enum column: column name"

        opt[String]("Int") abbr CsvTypes.IntType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.IntType)
        } unbounded() text "an integer column: column name"

        opt[String]("Long") abbr CsvTypes.LongType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.LongType)
        } unbounded() text "an 64-bit integer column: column name"

        opt[String]("Float") abbr CsvTypes.FloatType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.FloatType)
        } unbounded() text "an 32-bit float column: column name"

        opt[String]("Double") abbr CsvTypes.DoubleType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.DoubleType)
        } unbounded() text "an 64-bit float column: column name"

        opt[String]("String") abbr CsvTypes.StringType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.StringType)
        } unbounded() text "a string column: column name"


        opt[(String, String)]("EnumOption") abbr CsvTypes.EnumOptionType.toString action { case ((cName, eName), c) =>
            updateInlineCsvEnum(reportError, c, cName, eName, CsvTypes.EnumOptionType)
        } unbounded() text "an optional enum column: [column name]=[enum canonical class name]"

        opt[String]("BooleanOption") abbr CsvTypes.BooleanOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.BooleanOptionType)
        } unbounded() text "an optional boolean column: column name"

        opt[String]("IntOption") abbr CsvTypes.IntOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.IntOptionType)
        } unbounded() text "an optional integer column: column name"

        opt[String]("LongOption") abbr CsvTypes.LongOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.LongOptionType)
        } unbounded() text "an optional 64-bit integer column: column name"

        opt[String]("FloatOption") abbr CsvTypes.FloatOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.FloatOptionType)
        } unbounded() text "an optional 32-bit float column: column name"

        opt[String]("DoubleOption") abbr CsvTypes.DoubleOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.DoubleOptionType)
        } unbounded() text "an optional 64-bit float column: column name"

        opt[String]("StringOption") abbr CsvTypes.StringOptionType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.StringOptionType)
        } unbounded() text "an optional string column: column name"


        opt[(String, String)]("EnumVector") abbr CsvTypes.EnumVectorType.toString action { case ((cName, eName), c) =>
            updateInlineCsvEnum(reportError, c, cName, eName, CsvTypes.EnumVectorType)
        } unbounded() text "an enum vector column: [column name]=[enum canonical class name]"

        opt[String]("BooleanVector") abbr CsvTypes.BooleanVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.BooleanVectorType)
        } unbounded() text "a boolean vector column: column name"

        opt[String]("IntVector") abbr CsvTypes.IntVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.IntVectorType)
        } unbounded() text "a integer vector column: column name"

        opt[String]("LongVector") abbr CsvTypes.LongVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.LongVectorType)
        } unbounded() text "a 64-bit integer vector column: column name"

        opt[String]("FloatVector") abbr CsvTypes.FloatVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.FloatVectorType)
        } unbounded() text "a 32-bit float vector column: column name"

        opt[String]("DoubleVector") abbr CsvTypes.DoubleVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.DoubleVectorType)
        } unbounded() text "a 64-bit float vector column: column name"

        opt[String]("StringVector") abbr CsvTypes.StringVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.StringVectorType)
        } unbounded() text "a string vector column: column name"


        opt[(String, String)]("EnumOptionVector") abbr CsvTypes.EnumOptionVectorType.toString action { case ((cName, eName), c) =>
            updateInlineCsvEnum(reportError, c, cName, eName, CsvTypes.EnumOptionVectorType)
        } unbounded() text "an enum vector column: [column name]=[enum canonical class name]"

        opt[String]("BooleanOptionVector") abbr CsvTypes.BooleanOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.BooleanOptionVectorType)
        } unbounded() text "a boolean option vector column: column name"

        opt[String]("IntOptionVector") abbr CsvTypes.IntOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.IntOptionVectorType)
        } unbounded() text "a integer option vector column: column name"

        opt[String]("LongOptionVector") abbr CsvTypes.LongOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.LongOptionVectorType)
        } unbounded() text "a 64-bit option integer vector column: column name"

        opt[String]("FloatOptionVector") abbr CsvTypes.FloatOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.FloatOptionVectorType)
        } unbounded() text "a 32-bit float option vector column: column name"

        opt[String]("DoubleOptionVector") abbr CsvTypes.DoubleOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.DoubleOptionVectorType)
        } unbounded() text "a 64-bit float option vector column: column name"

        opt[String]("StringOptionVector") abbr CsvTypes.StringOptionVectorType.toString action { (name, c) =>
            updateInlineCsvCol(reportError, c, name, CsvTypes.StringOptionVectorType)
        } unbounded() text "a string vector option column: column name"

        note(
            """
              |Examples:
              |
              |  -model-runner \
              |    --output-type Int \
              |    -A \
              |    --missing "" \
              |    --sep "\t" \
              |    --ifs "," \
              |    --imports scala.math._,scala.util._,com.eharmony.aloha.feature.BasicFunctions._\
              |    --input-file $HOME/aloha_models/data/123_data.csv \
              |    -E "a.b.c.Gender=MALE,FEMALE" \
              |    -E "a.b.c.OSFamily=OS_UNKNOWN:0,WINDOWS:1,LINUX:2,MAC:3,IOS:4,ANDROID:5" \
              |    -i "profile.user_id" \
              |    -s "profile.locale"\
              |    -e "reg.os_type=a.b.c.OSFamily" \
              |    -oi "profile.num_children" \
              |    -oe "profile.gender=a.b.c.Gender" \
              |    $HOME/aloha_models/123.json
              |
            """.stripMargin.trim
        )

        checkConfig { c => c.validate.fold(failure, _ => success) }
    }
}


/**
 *
 */
@CLI(flag = "--modelrunner")
object CsvModelRunner {

    implicit def optionAuditor[A]: OptionAuditor[A] = OptionAuditor[A]()

    def getConf(args: Seq[String]): Option[CsvModelRunnerConfig] =
        CsvModelRunnerConfig.parser.parse(args, CsvModelRunnerConfig())

    def inputAndModel[A](inputType: InputType, outputType: OutputType.Value, protoOutput: Boolean, imports: Seq[String], cacheDir: Option[File], model: FileObject): ((String) => A, A => Option[Any]) = {
        // s: CompiledSemanticsLike[Any]
        // fn: (String) => A
        // refInfo: RefInfo[A]
        val (s, fn, refInfo) = inputType match {
            case t : CsvInputType  =>
                val (p, csvLines) = t.csvPluginAndLines
                (
                    CompiledSemantics(TwitterEvalCompiler(classCacheDir = cacheDir), p, imports).asInstanceOf[CompiledSemantics[A]],
                    ((s: String) => csvLines(s)).asInstanceOf[String => A],
                    RefInfo[CsvLine].asInstanceOf[RefInfo[A]]
                )
            case t: ProtoInputType =>
                val (p, untypedF, ri) = t.getProtoPluginAndExtractorFunction
                (
                    CompiledSemantics(TwitterEvalCompiler(classCacheDir = cacheDir), p.asInstanceOf[CompiledSemanticsPlugin[A]], imports),
                    untypedF.asInstanceOf[String => A],
                    ri.asInstanceOf[RefInfo[A]]
                )
        }

        implicit val refInfoA: RefInfo[A] = refInfo

//        def instantiate[B : RefInfo : ScoreConverter : JsonFormat] =
//            ModelFactory.defaultFactory.toTypedFactory[A, B](s).fromVfs2(model).get

        // s: CompiledSemanticsLike[Any]
        // fn: (String) => A
        // refInfo: RefInfo[A]



        def instantiate[N](protoOutput: Boolean)
                          (implicit r: RefInfo[N],
                           oa: OptionAuditor[N],
                           sa: ScoreAuditor[N],
                           sc: ScoreConverter[N]): A => Option[N] = {
            if (protoOutput) {
                val m = ModelFactory.defaultFactory(s, sa).fromVfs2(model).get
                m.andThen(s => if (s.hasScore) sc.unboxScore(s.getScore) else None)
            }
            else {
                ModelFactory.defaultFactory(s, oa).fromVfs2(model).get
            }
        }

        import OutputType._

        val protoOutput: Boolean = false

        val m = outputType match {
            case BooleanType => instantiate[Boolean](protoOutput)
            case ByteType    => instantiate[Byte](protoOutput)
            case DoubleType  => instantiate[Double](protoOutput)
            case FloatType   => instantiate[Float](protoOutput)
            case IntType     => instantiate[Int](protoOutput)
            case LongType    => instantiate[Long](protoOutput)
            case ShortType   => instantiate[Short](protoOutput)
            case StringType  => instantiate[String](protoOutput)
        }

        ((s: String) => fn(s), m)
    }

    def getPredictionOutputFormat(config: Option[CsvModelRunnerConfig]): Option[PredictionOutputFormat] = config map { conf =>
        val inputType: InputType = conf.inputType.right.get.get

        val protoOutput = conf.loadTest.map(lt => lt.useScoreObjects).getOrElse(false)

        val (inF, model) = inputAndModel[Any](inputType, conf.outputType, protoOutput, conf.imports, conf.classCacheDir, conf.model.get)

        val out: OutputStream = conf.outputFile.fold[OutputStream](System.out)(f => VFS.getManager.resolveFile(f).getContent.getOutputStream())
        val closeOut: Boolean = conf.outputFile.isDefined

        val is: InputStream = conf.inputFile.fold(System.in)(f => VFS.getManager.resolveFile(f).getContent.getInputStream)
        val in: Iterator[String] = scala.io.Source.fromInputStream(is).getLines()

        conf.loadTest match {
            case Some(lt) =>
                LoadTestOutput(out, closeOut, in, inF, model, conf.outputSep, lt)
            case None =>
                ModelPredictionOutput(
                    out,
                    closeOut,
                    (s: String) => model(inF(s)),
                    in,
                    conf.inputPosInOutput,
                    conf.outputSep,
                    conf.predictionMissing)
        }
    }

    /**
     * Main entry point to model runner.
     *
     * Example:
     *   run \
     *     --missing "" \
     *     --sep "\t" \
     *     --ifs "," \
     *     -e user.name com.eh.Names \
     *     -b user.isMale \
     *     -i user.age \
     *     -l user.birthdate.unixtime \
     *     -f user.face.ratio \
     *     -d user.xyz \
     *     -s user.name.first \
     *     -E  com.eh.Names Bill,James,Dan \
     *     -E  com.eh.Names Bill:1,James:2,Dan:4
     *
     * @param args arguments to application.
     */
    def main(args: Array[String]) {
        // Surpress VFS logging.
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

        // Get the configuration.
        // When reading in from the shell script, the shell script escapes the control characters, so we unescape.
        // Don't do this in
        val config = getConf(args).flatMap { c =>
            c.inputType.right.get.map {
                case in: InlineCsvInputType => c.copy(inputType = Right(Option(in.copy(
                    // TODO: If this doesn't work, use commons-lang3 StringEscapeUtils.unescapeJava for unescaping.
                    //       Removed commons-lang3 as a dependency because it's only used in 2 places.  Here and
                    //       aloha-vw-jni Cli class.  Here's how it was originally.
                    //
                    //         separator = StringEscapeUtils.unescapeJava(in.separator),
                    //         intraFieldSeparator = StringEscapeUtils.unescapeJava(in.intraFieldSeparator)
                    separator = unescape(in.separator),
                    intraFieldSeparator = unescape(in.intraFieldSeparator)
                ))))
                case _ => c
            }
        }

        getPredictionOutputFormat(config) foreach { pof => pof.run() }
    }

    private[this] def unescape(s: String) = s.replaceAllLiterally("\\\\", "\\")
}

