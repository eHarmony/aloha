package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import scala.util.parsing.combinator.RegexParsers
import org.apache.commons.vfs2.VFS
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.factory.ModelFactory
import java.io.{PrintStream, File}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.lang.StringEscapeUtils

private[csv] sealed trait EnumGen {
    def withClassName(canonicalClassName: String): Enum
}

private[csv] case class WithNums(constants: Seq[(String, Int)]) extends EnumGen {
    def withClassName(canonicalClassName: String) = Enum(canonicalClassName, constants:_*)
}

private[csv] case class NoNums(constants: Seq[String]) extends EnumGen {
    def withClassName(canonicalClassName: String) = Enum.withNoNumbers(canonicalClassName, constants:_*)
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

    def failureMsg(s: String) = parseAll(root, s) match {
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

import InputPosition._

case class CsvModelRunnerConfig(
    inputPosInOutput: InputPosition = Neither,
    imports: Seq[String] = Vector.empty,
    headersInOutput: Boolean = false,
    classCacheDir: Option[File] = None,
    model: Option[String] = None,
    inputFile: Option[String] = None,
    outputFile: Option[String] = None,
    missing: String = "",
    outputType: OutputType.Value = OutputType.IntType,
    separator: String = "\t",
    intraFieldSeparator: String = ",",
    colNamesToTypes: Seq[(String, CsvTypes.CsvType)] = Vector.empty,
    colNameToEnumName: Seq[(String, String)] = Vector.empty,
    fieldIndices: Seq[(String, Int)] = Vector.empty,
    enums: Seq[(String, Enum)] = Vector.empty,
    errorOnOptMissingField: Boolean = false,
    errorOnOptMissingEnum: Boolean = false
) {

    def missingFunction(): String => Boolean = if (missing.isEmpty) _.isEmpty else _ matches missing

    def validate(): Either[String, Unit] = {
        lazy val colNameDups = dupKeys(colNamesToTypes) ++ dupKeys(colNameToEnumName) ++ dupKeys(fieldIndices)
        lazy val enumDups = dupKeys(enums)
        lazy val enumClasses = enums.map(_._1).toSet
        lazy val noEnumAvailable = colNameToEnumName filterNot { case(c, e) => enumClasses contains e }

        if (inputPosInOutput == Both) Left("Cannot place input both before and after the model output in the output.")
        else if (model.isEmpty) Left("no model defined")
        else if (missing == separator) Left("missing string == separator: \"${missing}\"")
        else if (missing == intraFieldSeparator) Left(s"""missing string == intra-field separator: "$missing".""")
        else if (separator == intraFieldSeparator) Left(s"""separator == intra-field separator: "$separator".""")
        else if (colNameDups.nonEmpty) Left(s"duplicate column names: ${colNameDups.mkString(",")}.")
        else if (enumDups.nonEmpty) Left(s"duplicate enum classes: ${enumDups.mkString(",")}.")
        else if (noEnumAvailable.nonEmpty) Left(s"The following columns refer to non existent enums: ${noEnumAvailable.map(_._1).mkString(",")}")
        else Right(())
    }

    private[csv] def dupKeys[A, B](it: Iterable[(A, B)]) = it.groupBy(_._1).collect{ case (k, v) if 1 < v.size => k }
}

object CsvModelRunnerConfig {

    def updateConfig(c: CsvModelRunnerConfig, cName: String, tpe: CsvTypes.CsvType) =
        c.copy(
            colNamesToTypes = c.colNamesToTypes :+ (cName -> tpe),
            fieldIndices    = c.fieldIndices    :+ (cName -> c.colNamesToTypes.size))

    val parser = new scopt.OptionParser[CsvModelRunnerConfig]("model-runner-tool") {
        head("model-runner-tool", "1.x")

        arg[String]("<model>") required() action { (m, c) =>
            c.copy(model = Some(m))
        } text "Apache VFS URL to model file."

        opt[Unit]('A', "after") action { (_, c) =>
            c.copy(inputPosInOutput = InputPosition(c.inputPosInOutput.id | After.id))
        } text "Include the input AFTER the model output in the program output."

        opt[Unit]('B', "before") action { (_, c) =>
            c.copy(inputPosInOutput = InputPosition(c.inputPosInOutput.id | Before.id))
        } text "Include the input BEFORE the model output in the program output."

        opt[File]('C', "cache") action { case (f, c) if f.isDirectory =>
            c.copy(classCacheDir = Option(f))
        } text "Directory to cache generated code.  Makes rerunning faster."

        opt[(String, String)]('E', "enum-def") action { case ((eName, eBody), c) =>
            // Try to parse.  Upon failure, return case config that was passed in.  Let the validation deal with
            // any issues.
            EnumParser.getEnum(eName, eBody).fold(
                _ => c,
                e => c.copy(enums = c.enums :+ (eName -> e)))
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
            c.copy(errorOnOptMissingField = true)
        } text "Produce an error when an optional field is requested for a non-existent column name."

        opt[Unit]("err-on-missing-optional-enum") action { (_, c) =>
            c.copy(errorOnOptMissingEnum = true)
        } text "Produce an error when an optional enum field is request for a column not associated with any enum."

        opt[String]("ifs") action { (ifs, c) =>

            c.copy(intraFieldSeparator = ifs)
        } text "intra-field separator string"

        opt[String]("input-file") action { (f, c) =>
            c.copy(inputFile = Option(f))
        } text "Apache VFS URL for input.  If not specified, data will come from STDIN."

        opt[String]("missing") action { (m, c) =>
            c.copy(missing = m)
        } text "string indicating missing value"

        opt[String]("output-type") action { (ot, c) =>
            c.copy(outputType = OutputType.withName(s"${ot}Type"))
        } text "Model score output type.  One of { Boolean, Byte, Double, Float, Int, Long, Short, String }"

        opt[String]("output-file") action { (f, c) =>
            c.copy(outputFile = Option(f))
        } text "Apache VFS URL for output.  If not specified, data will go to STDOUT."

        opt[String]("sep") action { (s, c) =>
            c.copy(separator = s)
        } text "column delimiter string"



        opt[(String, String)]("Enum") abbr CsvTypes.EnumType.toString action { case ((cName, eName), c) =>
            c.copy(
                colNameToEnumName = c.colNameToEnumName :+ (cName -> eName),
                colNamesToTypes   = c.colNamesToTypes   :+ (cName -> CsvTypes.EnumType),
                fieldIndices      = c.fieldIndices      :+ (cName -> c.colNamesToTypes.size)
            )
        } unbounded() text "an enum column: [column name]=[enum canonical class name]"

        opt[String]("Boolean") abbr CsvTypes.BooleanType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.BooleanType)
        } unbounded() text "an enum column: column name"

        opt[String]("Int") abbr CsvTypes.IntType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.IntType)
        } unbounded() text "an integer column: column name"

        opt[String]("Long") abbr CsvTypes.LongType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.LongType)
        } unbounded() text "an 64-bit integer column: column name"

        opt[String]("Float") abbr CsvTypes.FloatType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.FloatType)
        } unbounded() text "an 32-bit float column: column name"

        opt[String]("Double") abbr CsvTypes.DoubleType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.DoubleType)
        } unbounded() text "an 64-bit float column: column name"

        opt[String]("String") abbr CsvTypes.StringType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.StringType)
        } unbounded() text "a string column: column name"


        opt[(String, String)]("EnumOption") abbr CsvTypes.EnumOptionType.toString action { case ((cName, eName), c) =>
            c.copy(
                colNameToEnumName = c.colNameToEnumName :+ (cName -> eName),
                colNamesToTypes   = c.colNamesToTypes   :+ (cName -> CsvTypes.EnumOptionType),
                fieldIndices      = c.fieldIndices      :+ (cName -> c.colNamesToTypes.size)
            )
        } unbounded() text "an optional enum column: [column name]=[enum canonical class name]"

        opt[String]("BooleanOption") abbr CsvTypes.BooleanOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.BooleanOptionType)
        } unbounded() text "an optional boolean column: column name"

        opt[String]("IntOption") abbr CsvTypes.IntOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.IntOptionType)
        } unbounded() text "an optional integer column: column name"

        opt[String]("LongOption") abbr CsvTypes.LongOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.LongOptionType)
        } unbounded() text "an optional 64-bit integer column: column name"

        opt[String]("FloatOption") abbr CsvTypes.FloatOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.FloatOptionType)
        } unbounded() text "an optional 32-bit float column: column name"

        opt[String]("DoubleOption") abbr CsvTypes.DoubleOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.DoubleOptionType)
        } unbounded() text "an optional 64-bit float column: column name"

        opt[String]("StringOption") abbr CsvTypes.StringOptionType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.StringOptionType)
        } unbounded() text "an optional string column: column name"


        opt[(String, String)]("EnumVector") abbr CsvTypes.EnumVectorType.toString action { case ((cName, eName), c) =>
            c.copy(
                colNameToEnumName = c.colNameToEnumName :+ (cName -> eName),
                colNamesToTypes   = c.colNamesToTypes   :+ (cName -> CsvTypes.EnumVectorType),
                fieldIndices      = c.fieldIndices      :+ (cName -> c.colNamesToTypes.size)
            )
        } unbounded() text "an enum vector column: [column name]=[enum canonical class name]"

        opt[String]("BooleanVector") abbr CsvTypes.BooleanVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.BooleanVectorType)
        } unbounded() text "a boolean vector column: column name"

        opt[String]("IntVector") abbr CsvTypes.IntVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.IntVectorType)
        } unbounded() text "a integer vector column: column name"

        opt[String]("LongVector") abbr CsvTypes.LongVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.LongVectorType)
        } unbounded() text "a 64-bit integer vector column: column name"

        opt[String]("FloatVector") abbr CsvTypes.FloatVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.FloatVectorType)
        } unbounded() text "a 32-bit float vector column: column name"

        opt[String]("DoubleVector") abbr CsvTypes.DoubleVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.DoubleVectorType)
        } unbounded() text "a 64-bit float vector column: column name"

        opt[String]("StringVector") abbr CsvTypes.StringVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.StringVectorType)
        } unbounded() text "a string vector column: column name"


        opt[(String, String)]("EnumOptionVector") abbr CsvTypes.EnumOptionVectorType.toString action { case ((cName, eName), c) =>
            c.copy(
                colNameToEnumName = c.colNameToEnumName :+ (cName -> eName),
                colNamesToTypes   = c.colNamesToTypes   :+ (cName -> CsvTypes.EnumOptionVectorType),
                fieldIndices      = c.fieldIndices      :+ (cName -> c.colNamesToTypes.size)
            )
        } unbounded() text "an enum vector column: [column name]=[enum canonical class name]"

        opt[String]("BooleanOptionVector") abbr CsvTypes.BooleanOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.BooleanOptionVectorType)
        } unbounded() text "a boolean option vector column: column name"

        opt[String]("IntOptionVector") abbr CsvTypes.IntOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.IntVectorType)
        } unbounded() text "a integer option vector column: column name"

        opt[String]("LongOptionVector") abbr CsvTypes.LongOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.LongOptionVectorType)
        } unbounded() text "a 64-bit option integer vector column: column name"

        opt[String]("FloatOptionVector") abbr CsvTypes.FloatOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.FloatOptionVectorType)
        } unbounded() text "a 32-bit float option vector column: column name"

        opt[String]("DoubleOptionVector") abbr CsvTypes.DoubleOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.DoubleOptionVectorType)
        } unbounded() text "a 64-bit float option vector column: column name"

        opt[String]("StringOptionVector") abbr CsvTypes.StringOptionVectorType.toString action { (name, c) =>
            updateConfig(c, name, CsvTypes.StringOptionVectorType)
        } unbounded() text "a string vector option column: column name"

        checkConfig { c => c.validate().fold(failure, _ => success) }
    }
}


/**
 *
 */
object CsvModelRunner {

    def getConf(args: Seq[String]): Option[CsvModelRunnerConfig] =
        CsvModelRunnerConfig.parser.parse(args, CsvModelRunnerConfig())

    def getRunnableInfo(config: Option[CsvModelRunnerConfig]) = config map { case conf =>
        // Create the object that generates CSV lines.
        val csvLines = CsvLines(
            conf.fieldIndices.toMap,
            conf.enums.toMap,
            conf.separator,
            conf.intraFieldSeparator,
            conf.missingFunction(),
            conf.errorOnOptMissingField,
            conf.errorOnOptMissingField
        )

        val csvPlugin = CompiledSemanticsCsvPlugin(conf.colNamesToTypes.toMap)
        val compiler = TwitterEvalCompiler(classCacheDir = conf.classCacheDir)
        val semantics = CompiledSemantics(compiler, csvPlugin, conf.imports)

        // I hate this code:
        val factory = conf.outputType match {
            case OutputType.BooleanType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Boolean](semantics)
            case OutputType.ByteType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Byte](semantics)
            case OutputType.DoubleType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Double](semantics)
            case OutputType.FloatType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Float](semantics)
            case OutputType.IntType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Int](semantics)
            case OutputType.LongType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Long](semantics)
            case OutputType.ShortType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, Short](semantics)
            case OutputType.StringType => ModelFactory.defaultFactory.toTypedFactory[CsvLine, String](semantics)
        }

        val model = factory.fromVfs2(VFS.getManager.resolveFile(conf.model.get)).get

        val out = conf.outputFile.map(f => VFS.getManager.resolveFile(f).getContent.getOutputStream).getOrElse(System.out)
        val closeOut = conf.outputFile.isDefined

        val is = conf.inputFile.map(f => VFS.getManager.resolveFile(f).getContent.getInputStream).getOrElse(System.in)
        val in = io.Source.fromInputStream(is).getLines()
        val input = csvLines(in)  // Iterator so lazy.  Need to foreach at the end.

        (conf, input, model, out, closeOut)
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
        val config = getConf(args).map{ c =>
            c.copy(
                separator = StringEscapeUtils.unescapeJava(c.separator),
                intraFieldSeparator = StringEscapeUtils.unescapeJava(c.intraFieldSeparator)
            )
        }

        // Extract to named parameters and if the necessary information exists, run each line
        // through the model and write out the output to STDOUT.
        getRunnableInfo(config) foreach { case (conf, lines, model, out, closeOut) =>
            try {
                val pStream = new PrintStream(out)

                // Create a writing function.  The first function parameter is the input, the second is the model output.
                val writingFunc: (String, String) => String = conf.inputPosInOutput match {
                    case InputPosition.Neither => (i, o) => o
                    case InputPosition.Before => (i, o) => s"$i${conf.separator}$o"
                    case InputPosition.After => (i, o) => s"$o${conf.separator}$i"
                    case InputPosition.Both => throw new IllegalStateException("Can't output input before and after output.")
                }

                lines.foreach{ line => pStream.println(writingFunc(line.line, model(line).map(_.toString).getOrElse(conf.missing))) }
            }
            finally {
                if (closeOut)
                    org.apache.commons.io.IOUtils.closeQuietly(out)
            }
        }
    }
}

