package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.SerializabilityEvidence
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 9/11/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelModelTest {
  import VwMultilabelModelTest._

  @Test def test1(): Unit = {
    val predProd = VwSparseMultilabelPredictorProducer[Label](
      modelSource = null,   // ModelSource,
      params = "",
      defaultNs = List.empty[Int],
      namespaces = List.empty[(String, List[Int])]
    )

    val model =
      MultilabelModel(
        modelId = ModelId(1, "model"),
        featureNames =  sci.IndexedSeq.empty[String],
        featureFunctions = sci.IndexedSeq.empty[GenAggFunc[Domain, Sparse]],
        labelsInTrainingSet = sci.IndexedSeq.empty[Label],
        labelsOfInterest = Option.empty[GenAggFunc[Domain, sci.IndexedSeq[Label]]],
        predictorProducer = predProd,
        numMissingThreshold = Option(1000000),
        auditor = Auditor)
  }
}

object VwMultilabelModelTest {
  private type Label = String
  private type Domain = Any

  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()
}