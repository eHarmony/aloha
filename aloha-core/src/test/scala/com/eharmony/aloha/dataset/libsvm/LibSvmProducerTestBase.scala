package com.eharmony.aloha.dataset.libsvm

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmRowCreator
import com.eharmony.aloha.dataset.{RowCreatorBuilder, RowCreatorProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines}
import com.eharmony.aloha.util.hashing.MurmurHash3

//import com.google.common.hash.Hashing
import org.junit.Assert._

trait LibSvmProducerTestBase {

    def test[A <: LibSvmRowCreator[CsvLine]](prod: RowCreatorProducer[CsvLine, CharSequence, A], label: String = "") {
        val bits = 31
        val mask = (1 << bits) - 1
        val f1 = "f1"
        val f2 = "f2"
        val one = 1.0
        val two = 2.0
        val f1Hash = MurmurHash3.stringHash(f1) & mask
        val f2Hash = MurmurHash3.stringHash(f2) & mask

        // This is the important part of the test.  This is the formatted string we are trying to produce
        // in LibSvmSpecProducer.

        val expected = Seq(f1Hash -> one, f2Hash -> two).sorted.map{ case(k, v) => s"$k:$v" }.mkString(
            if (label.isEmpty) "" else s"$label ",
            " ",
            ""
        )

        // Note that we need the import here to make the json work because of the conversions from scalars
        // to key value pairs.
        val json = s"""
                      |{
                      |  "imports": [ "com.eharmony.aloha.feature.BasicFunctions._" ],
                      |  "numBits": $bits,
                      |  "features": [
                      |    { "name": "$f1", "spec": "Seq((\\"\\", $one))" },
                      |    { "name": "$f2", "spec": "$two" }
                      |  ],
                      |  "label": "\\"$label\\""
                      |}
                    """.stripMargin

        val semantics = CompiledSemantics(
            TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
            CompiledSemanticsCsvPlugin(),
            Nil)(scala.concurrent.ExecutionContext.global)

        // Provide seed so its the same as the one in mh3
        val sb = RowCreatorBuilder(semantics, List(prod))

        // Assume no problems parsing.  That's not the point of the test.
        val libSvmSpec = sb.fromString(json).get

        // Don't care about the data in here.  None of it is necessary.  Just produce one line since
        // features are invariant to data so all lines will be the same.
        val line: CsvLine = CsvLines(Map.empty)("")

        // Compute the actual value.
        val (_, actual) = libSvmSpec(line)

        assertEquals(expected, actual.toString)
    }
}
