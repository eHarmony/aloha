package com.eharmony.aloha.cli.dataset

import java.io.{Closeable, File, InputStream, PrintStream}
import java.util.regex.Matcher

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.dataset.csv.CsvRowCreator
import com.eharmony.aloha.dataset.libsvm.labeled.LibSvmLabelRowCreator
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmRowCreator
import com.eharmony.aloha.dataset.vw.cb.VwContextualBanditRowCreator
import com.eharmony.aloha.dataset.vw.labeled.VwLabelRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset.{CharSeqRowCreator, RowCreatorBuilder, RowCreatorProducer}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvProtocol}
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.util.Logging
import com.google.protobuf.GeneratedMessage
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.{FileObject, VFS}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


@CLI(flag = "--dataset")
object DatasetCli extends Logging {
    import DatasetType.DatasetType

    private val CommandName = "dataset"

    private[this] implicit val vfs2FoRead = scopt.Read.reads(VFS.getManager.resolveFile)
    private[this] implicit val vfs2FoOptRead =
        scopt.Read.reads[Option[FileObject]](s => if (s == "-") None else Option(vfs2FoRead.reads(s)))

    def main(args: Array[String]): Unit = {
        cliParser.parse(args, Config()) match {
            case Some(Config(chunkSize, cacheDir, Seq(inputType), Some(spec), inFile, datasets, cp, headersInCsv, csvHeaderFile)) =>
                val (is, closeIn) = inStream(inFile)

                // TODO: test par vs seq.
                val lws = lineWriters(inputType, datasets, spec, cacheDir)

                csvHeaderFile foreach { printCsvHeaderFile(lws, _) }

                if (1 < chunkSize)
                    runParallel(is, lws, chunkSize, headersInCsv)
                else runSequential(is, lws, headersInCsv)

                // Close the files.
                lws.foreach(lw => lw.close())

                if (closeIn)
                    IOUtils.closeQuietly(is)

            case Some(config) => System.err.println("Something went wrong (THIS IS A BUG). Config: " + config)
            case None => // Taken care of by scopt
        }
    }

    private final def findCsvLineWriters[A](lws: Seq[LineWriter[A]]) =
        lws.collect { case lw@LineWriter(_, csv@CsvRowCreator(_, _, _), _, _) => (lw, csv) }

    private final def printCsvHeaders[A](lws: Seq[LineWriter[A]]): Unit =
        findCsvLineWriters(lws) foreach { case (lw, csv) => printCsvHeaders(csv, lw.out) }

    private final def printCsvHeaderFile[A](lws: Seq[LineWriter[A]], file: FileObject): Unit =
        findCsvLineWriters(lws).headOption foreach {
            case (_, csv) => printCsvHeaders(csv, new PrintStream(file.getContent.getOutputStream), close = true)
        }

    private final def printCsvHeaders[A](csvRowCreator: CsvRowCreator[A], ps: PrintStream, close: Boolean = false): Unit = {
        ps.println(csvRowCreator.headerString)
        ps.flush()
        if (close)
            ps.close()
    }

    private def runSequential[A](is: InputStream, lws: Seq[LineWriter[A]], headersInCsv: Boolean): Unit = {
        if (headersInCsv)
            printCsvHeaders(lws)

        scala.io.Source.fromInputStream(is).getLines().zipWithIndex.foreach { line =>
            lws.foreach { lw =>
                try {
                    lw(line._1)
                }
                catch {
                    case e: Throwable =>
                    // TODO: Log and update stats
                }
            }
        }
    }

    private def runParallel[A](is: InputStream, lws: Seq[LineWriter[A]], chunkSize: Int, headersInCsv: Boolean): Unit = {
        if (headersInCsv)
            printCsvHeaders(lws)

        val lwsp = lws.par
        scala.io.Source.fromInputStream(is).getLines().zipWithIndex.grouped(chunkSize).foreach { lines =>
            lwsp.foreach { lw =>
                lines.foreach { line =>
                    // TODO: Use an inlined method.
                    try {
                        lw(line._1).flush()
                    }
                    catch {
                        case e: Throwable =>

                        // TODO: Log and update stats
                    }
                }
                // STDOUT (System.out) is automatically flushed.  Streams that print to files are not
                // automatically flushed, so do this for both.
                lw.flush()
            }
        }
    }

    private case class LineWriter[A](extractor: String => A, rowCreator: CharSeqRowCreator[A], out: PrintStream, closeStream: Boolean) extends (String => LineWriter[A]) with Closeable {
        override def apply(a: String): this.type = {
            out.println(rowCreator(extractor(a))._2)
            this
        }
        def flush(): this.type = {
            out.flush()
            this
        }
        override def close(): Unit = if (closeStream) IOUtils.closeQuietly(out)
    }

    private object LineWriter {
        def apply[A](extractor: String => A, rowCreator: CharSeqRowCreator[A], outFile: Option[FileObject]): LineWriter[A] = {
            val (out, close) = outStream(outFile)
            new LineWriter(extractor, rowCreator, out, close)
        }

        /**
         * If the LineWriter can't be created because the [[CharSeqRowCreator]] couldn't
         * be created, there is no need to attempt to clean up the out's OutputStream because the process will fail
         * before attempting to open the OutputStream.
         * @param extractor
         * @param semantics
         * @param datasetType
         * @param out
         * @param spec
         * @tparam A
         * @return
         */
        def create[A](extractor: String => A, semantics: CompiledSemantics[A], datasetType: DatasetType, out: Option[FileObject], spec: FileObject): Try[LineWriter[A]] =
            RowCreatorBuilder(semantics, List(datasetType.rowCreatorProducer[A])).
                fromVfs2(spec).
                map(s => LineWriter(extractor, s, out))

        private def outStream(f: Option[FileObject]): (PrintStream, Boolean) =
            f.fold((System.out, false))(f => (new PrintStream(f.getContent.getOutputStream), true))
    }

    protected[this] def inStream(f: Option[FileObject]): (InputStream, Boolean) =
        f.fold((System.in, false))(f => (f.getContent.getInputStream, true))


    // TODO: Figure this out...
    private[this] def lineWriters[A](inputType: InputType, datasets: Seq[(DatasetType, Option[FileObject])], spec: FileObject, cacheDir: Option[File]): Seq[LineWriter[A]] = {
        val imports = getImports(spec)
        inputType match {
            case CsvInputType(csvDef) =>
                val (p, untypedF) = getCsvPluginAndExtractorFunction(csvDef)
                val f = untypedF.asInstanceOf[CharSequence => A]
                val s = CompiledSemantics(TwitterEvalCompiler(classCacheDir = cacheDir), p, imports).asInstanceOf[CompiledSemantics[A]]
                datasets map {
                    case(dsType, out) => dsType -> LineWriter.create(f, s, dsType, out, spec)
                } flatMap {
                    case (dsType, Success(success)) => Option(success)
                    case (dsType, Failure(failure)) =>
                        System.err.println(s"Couldn't $dsType dataset. Error(s): ${failure.getMessage}")
                        None
                }
            case ProtoInputType(protoClass) =>
                val (untypedP, unTypedF) = getProtoPluginAndExtractorFunction(protoClass)
                val f = unTypedF.asInstanceOf[String => A]
                val s = CompiledSemantics[Nothing](TwitterEvalCompiler(classCacheDir = cacheDir), untypedP, imports).
                          asInstanceOf[CompiledSemantics[A]]

                datasets map {
                    case(dsType, out) => dsType -> LineWriter.create(f, s, dsType, out, spec)
                } flatMap {
                    case (dsType, Success(success)) => Option(success)
                    case (dsType, Failure(failure)) =>
                        System.err.println(s"Couldn't $dsType dataset. Error(s): ${failure.getMessage}")
                        None
                }
        }
    }

    private[this] def getCsvPluginAndExtractorFunction(csvDef: FileObject): (CompiledSemanticsCsvPlugin, String => CsvLine) = {
        import spray.json.pimpString
        val csvDataRetriever = CsvProtocol.getCsvDataRetriever(StringReadable.fromVfs2(csvDef).parseJson)
        val csvLines: CsvLines = csvDataRetriever.csvLines
        val plugin: CompiledSemanticsCsvPlugin = csvDataRetriever.plugin
        (plugin, (s: String) => csvLines(s))
    }

    /**
     * This let's classes be specified to the API like java classes (a.b.c.Outer$Inner)
     * or scala classes (a.b.c.Outer.Inner).
     * @param className class name for which we are trying to get a Class instance.
     * @return
     */
    @tailrec private def attemptToGetClass[A](className: String): Option[Class[A]] = {
        if (-1 == className.indexOf(".")) None
        else Try { Class.forName(className).asInstanceOf[Class[A]] } match {
            case Success(c) => Option(c)
            case Failure(_) => attemptToGetClass(className.reverse.replaceFirst("\\.", Matcher.quoteReplacement("$")).reverse)
        }
    }

    private[this] def getProtoPluginAndExtractorFunction[A <: GeneratedMessage](protoClass: String): (CompiledSemanticsProtoPlugin[A], String => A) = {
        // The getOrElse is to throw the ClassNotFoundException with the original class name.
        val c = attemptToGetClass[A](protoClass) getOrElse Class.forName(protoClass).asInstanceOf[Class[A]]
        val m = c.getMethod("parseFrom", classOf[Array[Byte]])
        val f: String => A = s => m.invoke(null, Base64.decodeBase64(s)).asInstanceOf[A]
        val plugin = new CompiledSemanticsProtoPlugin[A](c)
        (plugin, f)
    }

    private[this] def getImports(spec: FileObject): Seq[String] = {
        import spray.json.DefaultJsonProtocol._
        import spray.json.pimpString
        StringReadable.fromVfs2(spec).parseJson.asJsObject("rowCreator contain a JSON object").getFields("imports") match {
            case Seq(imp) => imp.convertTo[Vector[String]]
            case _        => Nil
        }
    }

    /**
     * Example:
     * {{{
     *  -dataset                                     \
     *    -cp /file/to/some.jar, /file/to/other.jar  \
     *    -s /file/to/rowCreator.js                        \
     *    -p com.eharmony.SomeProto                  \  (csv|proto)
     *    -c /file/to/csv.def.js                     \
     *    -i /file/to/b64_proto_in.txt               \  if omitted, use STDIN
     *    -vw_labeled /file/to/labeled.vw            \
     *    -vw_bandit /file/to/bandit.vw              \  if omitted, use STDOUT (like below)
     *    -libsvm_labeled
     * }}}
     * @return
     */
    def cliParser = {
        new scopt.OptionParser[Config](CommandName) {
            head(CommandName, aloha.version)
            opt[File]("cachedir") action { (x, c) =>
                c.copy(cacheDir = Option(x))
            } text "a cache directory" optional()
            opt[Int]("parallel") action { (x, c) =>
                if (x < 1) reportError(s"parallel flag must provide a positive value.  Provided chunk size of ${c.chunkSize}. ASDF")
                c.copy(chunkSize = x)
            } text "a list of Apache VFS URLs additional jars to be included on the classpath" optional()
            opt[FileObject]('s', "spec") action { (x, c) =>
                c.copy(spec = Option(x))
            } text "Apache VFS URL to a JSON specification file containing attributes of the dataset being created." required()
            opt[String]('p', "proto-input") action { (x, c) =>
                // B/c maxOccurs = 1, non-empty must come from another input type.
                c.copy(inputTypes = c.inputTypes :+ ProtoInputType(x))
            } text "canonical class name of the protocol buffer type to use." maxOccurs (1)
            opt[FileObject]('c', "csv-input") action { (x, c) =>
                // B/c maxOccurs = 1, non-empty must come from another input type.
                c.copy(inputTypes = c.inputTypes :+ CsvInputType(x))
            } text "Apache VFS URL to JSON file specifying the structure of the CSV input." maxOccurs (1)
            opt[FileObject]('i', "in") action { (x, c) =>
                c.copy(input = Option(x))
            } text "Apache VFS URL to the input file.  If not supplied, STDIN will be used." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.vw.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.vw -> x)
            } text "produce an unlabeled VW dataset and place the output in the specified location." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.vw_labeled.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.vw_labeled -> x)
            } text "produce a labeled VW dataset and place the output in the specified location." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.vw_cb.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.vw_cb -> x)
            } text "produce a contextual bandit VW dataset and place the output in the specified location." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.libsvm.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.libsvm -> x)
            } text "produce an unlabeled LIBSVM dataset and place the output in the specified location." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.libsvm_labeled.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.libsvm_labeled -> x)
            } text "produce a labeled LIBSVM dataset and place the output in the specified location." maxOccurs (1)
            opt[Option[FileObject]](DatasetType.csv.toString) action { (x, c) =>
                c.copy(datasets = c.datasets :+ DatasetType.csv -> x)
            } text "produce a CSV dataset and place the output in the specified location." maxOccurs (1)
            opt[Unit]("csv-headers") action { (_, c) =>
                c.copy(headersInCsv = true)
            } text "Produce headers in CSV output." maxOccurs (1)
            opt[FileObject]("csv-header-file") action { (f, c) =>
                c.copy(csvHeaderFile = Some(f))
            } text "Write CSV headers to the designated file." maxOccurs (1)
            checkConfig { c =>
                if (c.chunkSize < 1)         Left(s"parallel flag must provide a positive value.  Provided chunk size of ${c.chunkSize}.")
                if (c.inputTypes.isEmpty)    Left("No input type provided.  Provide one of the proto-input or csv-input options.")
                if (1 < c.inputTypes.size)   Left("Multiple input types provided.  Provide one of the proto-input or csv-input options.")
                else if (c.datasets.isEmpty) Left("No output dataset type provided.  Provide at least one of: " + DatasetType.values.mkString(", "))
                else {
                    val dup = c.datasets.groupBy(_._2).collect { case (k, v) if 1 < v.size => (v.map(_._1), k) }
                    if (dup.nonEmpty)
                        Left(dup.map { case (ts, f) => ts.mkString("(", ", ", ")") + " => " + f.getOrElse("-") }
                            .mkString("multiple output types going to same file: ", ", ", "."))
                    else Right(())
                }
            }
        }
    }

    object DatasetType extends Enumeration {
        type DatasetType = Value
        val vw, vw_labeled, vw_cb, libsvm, libsvm_labeled, csv = Value

        implicit class DatasetTypeOps(val v: DatasetType) extends AnyVal {
            def rowCreatorProducer[A]: RowCreatorProducer[A, CharSequence, CharSeqRowCreator[A]] = v match {
                case `vw`             => new VwRowCreator.Producer[A]
                case `vw_labeled`     => new VwLabelRowCreator.Producer[A]
                case `vw_cb`          => new VwContextualBanditRowCreator.Producer[A]
                case `libsvm`         => new LibSvmRowCreator.Producer[A]
                case `libsvm_labeled` => new LibSvmLabelRowCreator.Producer[A]
                case `csv`            => new CsvRowCreator.Producer[A]
            }
        }
    }

    case class Config(chunkSize: Int = 1,
                      cacheDir: Option[File] = None,
                      inputTypes: Seq[InputType] = Nil,
                      spec: Option[FileObject] = None,
                      input: Option[FileObject] = None,
                      datasets: Seq[(DatasetType, Option[FileObject])] = Nil,
                      classPath: Seq[FileObject] = Nil,
                      headersInCsv: Boolean = false,
                      csvHeaderFile: Option[FileObject] = None)

    sealed trait InputType
    case class ProtoInputType(protoClass: String) extends InputType
    case class CsvInputType(csvDef: FileObject) extends InputType
}
