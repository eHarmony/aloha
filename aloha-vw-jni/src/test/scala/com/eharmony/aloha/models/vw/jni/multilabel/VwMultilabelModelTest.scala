package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.File

import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.sources.{ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc0}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 9/11/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelModelTest {
  import VwMultilabelModelTest._

  @Test def test1(): Unit = {
    val features = Vector[GenAggFunc[Domain, Sparse]](
      GenFunc0("", (_: Domain) => Iterable(("", 1d)))
    )

    val predProd = VwSparseMultilabelPredictorProducer[Label](
      modelSource = TrainedModel,
      params = "", // to see the output:  "-p /dev/stdout",
      defaultNs = List.empty[Int],
      namespaces = List(("X", List(0))) // List.empty[(String, List[Int])]
    )

    val model =
      MultilabelModel(
        modelId             = ModelId(1, "model"),
        featureNames        = Vector(FeatureName),
        featureFunctions    = features,
        labelsInTrainingSet = AllLabels,
        labelsOfInterest    = Option.empty[GenAggFunc[Domain, sci.IndexedSeq[Label]]],
        predictorProducer   = predProd,
        numMissingThreshold = Option.empty[Int],
        auditor             = Auditor)

    val result = model("This value doesn't matter.")

    try {
      result.value match {
        case None => fail()
        case Some(labelMap) =>
          assertEquals(AllLabels.toSet, labelMap.keySet)
          assertEquals(0.7, labelMap(LabelSeven), 0.01)
          assertEquals(0.8, labelMap(LabelEight), 0.01)
          assertEquals(0.6, labelMap(LabelSix), 0.01)
      }
    }
    finally {
      model.close()
    }
  }
}

object VwMultilabelModelTest {
  private type Label  = String
  private type Domain = Any

  private val LabelSeven = "seven"
  private val LabelEight = "eight"
  private val LabelSix   = "six"

  /**
    * The order here is important because it's the same as the indices in the training data.
    - LabelSeven is _C0_ (index 0)
    - LabelEight is _C1_ (index 1)
    - LabelSix is _C2_ (index 2)
    */
  private val AllLabels = Vector(LabelSeven, LabelEight, LabelSix)

  private def tmpFile() = {
    val f = File.createTempFile(classOf[VwMultilabelModelTest].getSimpleName + "_", ".vw.model")
    f.deleteOnExit()
    f
  }

  private def vwTrainingParams(modelFile: File = tmpFile()) = {

    val flags =
      """
        | --quiet
        | --csoaa_ldf mc
        | --csoaa_rank
        | --loss_function logistic
        | --link logistic
        | -q YX
        | --noconstant
        | --ignore_linear X
        | --ignore y
        | -f
      """.stripMargin.trim

    (flags + " " + modelFile.getCanonicalPath).split("\n").map(_.trim).mkString(" ")
  }

  private val ModelFile = tmpFile()

  private val FeatureName = "feature"

  /**
    * A dataset that creates the following marginal distribution.
    - Pr[seven] = 0.7  where seven is _C0_
    - Pr[eight] = 0.8  where eight is _C1_
    - Pr[six]   = 0.6  where six is _C2_
    */
  private val TrainingData =
    Vector(
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.084 |y _C2147483649_\n1:0.0 |Y _C1_\n0:-0.084 |Y _C0_\n2:-0.084 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.024 |y _C2147483649_\n1:0.0 |Y _C1_\n0:0.0 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.336 |y _C2147483649_\n1:-0.336 |Y _C1_\n0:-0.336 |Y _C0_\n2:-0.336 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.056 |y _C2147483649_\n1:0.0 |Y _C1_\n0:-0.056 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.144 |y _C2147483649_\n1:-0.144 |Y _C1_\n0:0.0 |Y _C0_\n2:-0.144 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.224 |y _C2147483649_\n1:-0.224 |Y _C1_\n0:-0.224 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.036 |y _C2147483649_\n1:0.0 |Y _C1_\n0:0.0 |Y _C0_\n2:-0.036 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.096 |y _C2147483649_\n1:-0.096 |Y _C1_\n0:0.0 |Y _C0_\n2:0.0 |Y _C2_"
    ).map(_.split(raw"\n"))

  private lazy val TrainedModel: ModelSource = {
    val modelFile = tmpFile()
    val params = vwTrainingParams(modelFile)
    val learner = VWLearners.create[VWActionScoresLearner](params)

    for {
      _ <- 1 to 50
      d <- TrainingData
    } {
      val asdf = d.toVector
      learner.learn(asdf.toArray)
    }

    learner.close()

    ExternalSource(Vfs.javaFileToAloha(modelFile))
  }

  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()
}