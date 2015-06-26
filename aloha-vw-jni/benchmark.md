# Benchmarking Aloha VW JNI and Aloha regression models

## Description

What follows are the load test setup and results for end-to-end comparison of feature generation and prediction speeds
between Aloha's regression model and Aloha's model that delegates to VW's JNI wrapper.  Both models create the same 
features so the difference in timings are presumably related to the way in which the models make use of the same 
intermediate data structures.

## Results

[Google Caliper Results](https://microbenchmarks.appspot.com/runs/ec237dff-a8be-40f1-9f94-928ebfed298a#r:scenario.benchmarkSpec.parameters.nExamples,scenario.benchmarkSpec.methodName)

It's also informative to see quantile information associated with predictions / sec:

<img src="https://github.corp.eharmony.com/modeling/aloha/raw/master/aloha-vw-jni/doc/img/aloha_vs_jni_quartiles_small.png" width="800px" />

## VW information

There were 294 sparse basis functions with quadratic interactions (*as shown below*).   

The vw arguments used were *"--quiet -t -q JY -q JW -q IY -q IW -q YW -q JI --loss_function logistic -k -b 26 --ignore Z -i"*

## Getting Data

```scala
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import org.apache.commons.vfs2.VFS

object Data {
    /**
     *
     * @param args The zeroth element is an Apache VFS URL to a file containing one base64 encoded 
     *             ''com.eharmony.matching.common.value.ModelTrainingProtoBuffs.ModelTrainingProto''
     *             per line.
     *             The first element is an Apache VFS URL to the output file.
     *             The second element is the number of lines to keep for the dataset.
     */
    def main(args: Array[String]): Unit = {
        val in = args(0)
        val out = args(1)
        val linesToKeep = args(2).toInt
        
        val lines = io.Source.fromInputStream(VFS.getManager.resolveFile(in).getContent.getInputStream).getLines()
        val data = lines.map { l =>
            new String(Base64.encodeBase64(ModelTrainingProto.parseFrom(Base64.decodeBase64(l))
                                                             .getOppGenderPairingProto.toByteArray))
        }

        val out = BufferedWriter(OutputStreamWriter(VFS.getManager.resolveFile(out).getContent.getOutputStream))
        data.take(linesToKeep).foreach(d => out.append(d).append("\n").flush())
        out.close()
    }
}
```

## Code


```scala
package bench

import java.io.File

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.common.value.ModelTrainingProtoBuffs.ModelTrainingProto
import com.eharmony.common.value.OppositeGenderUserPairingProtoBuffs.OppositeGenderUserPairingProto
import com.google.caliper.{BeforeExperiment, Benchmark, Param}
import com.google.caliper.api.Macrobenchmark
import org.apache.commons.codec.binary.Base64
import org.apache.commons.vfs2.VFS
import spray.json.DefaultJsonProtocol._

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global

class Bench {

    @Param(Array("100", "1000", "10000")) var nExamples: Int = 0

    private[this] lazy val allExamples: Vector[OppositeGenderUserPairingProto] = {
        io.Source.fromInputStream(VFS.getManager.resolveFile("res:opp_gender_pairing_protos.b64.txt").getContent.getInputStream).
            getLines().
            map(l => OppositeGenderUserPairingProto.parseFrom(Base64.decodeBase64(l))).
            toVector
    }

    private[this] var examples: Array[OppositeGenderUserPairingProto] = _

    private[this] def semantics() = {
        val plugin = CompiledSemanticsProtoPlugin[OppositeGenderUserPairingProto]
        val compiler = TwitterEvalCompiler(classCacheDir = Option(new File("target/test-classes/generated")))
        val imports = Seq(
            "com.eharmony.aloha.feature.BasicFunctions._",
            "com.eharmony.matching.maestro.SemanticsFunctions._",
            "com.eharmony.matching.maestro.scala.SemanticsFunctions._",
            "com.eharmony.aloha.feature.OptionMath.Syntax._",
            "scala.math._"
        )
        CompiledSemantics(compiler, plugin, imports)
    }

    private[this] lazy val factory =  
        ModelFactory.defaultFactory.toTypedFactory[OppositeGenderUserPairingProto, Float](semantics())

    private lazy val vw: Model[OppositeGenderUserPairingProto, Float] = {
        val m = factory.fromResource("aloha_vw_model.json").get
        Thread.sleep(2000) // Need this to avoid intermittent errors
        m
    }

    private lazy val reg: Model[OppositeGenderUserPairingProto, Float] =
        factory.fromResource("aloha_regression_model.json").get

    def doBench(m: Model[OppositeGenderUserPairingProto, Float]): Double = {
      var s = 0d
      var j = nExamples - 1
      while (j >= 0) {
        s += m(examples(j)).get
        j -= 1
      }
      s
    }

    @BeforeExperiment def setUp: Unit = {
        // Do these to initialize the lazy vals.
        println(s"VW model ID: ${vw.modelId.getId()}")
        println(s"Reg model ID: ${reg.modelId.getId()}")
        examples = allExamples.take(nExamples).toArray
        1 to 3 foreach { _ => System.gc } // probably doesn't make a difference
    }

    @Macrobenchmark def jni: Double   = doBench(vw)
    @Macrobenchmark def aloha: Double = doBench(reg)
}
```
