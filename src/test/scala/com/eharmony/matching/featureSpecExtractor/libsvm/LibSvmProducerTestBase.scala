package com.eharmony.matching.featureSpecExtractor.libsvm

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLines, CompiledSemanticsCsvPlugin, CsvLine}
import com.eharmony.matching.featureSpecExtractor.{SpecBuilder, SpecProducer}
import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpec
import com.google.common.hash.Hashing
import org.junit.Assert._

trait LibSvmProducerTestBase {

    def test[A <: LibSvmSpec[CsvLine]](seed: Int, prod: SpecProducer[CsvLine, A], label: String = "") {
        val mh3 = Hashing.murmur3_32(seed)
        val bits = 31
        val mask = (1 << bits) - 1
        val f1 = "f1"
        val f2 = "f2"
        val one = 1.0
        val two = 2.0
        val f1Hash = mh3.newHasher.putString(f1).hash.asInt & mask
        val f2Hash = mh3.newHasher.putString(f2).hash.asInt & mask

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
                      |  "imports": [ "com.eharmony.matching.aloha.feature.BasicFunctions._" ],
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
        val sb = SpecBuilder(semantics, List(prod))

        // Assume no problems parsing.  That's not the point of the test.
        val libSvmSpec = sb.fromString(json).get

        // Don't care about the data in here.  None of it is necessary.  Just produce one line since
        // features are invariant to data so all lines will be the same.
        val line: CsvLine = CsvLines(Map.empty)("").head

        // Compute the actual value.
        val (missing, actual) = libSvmSpec(line)

        assertEquals(expected, actual.toString)
    }
}
