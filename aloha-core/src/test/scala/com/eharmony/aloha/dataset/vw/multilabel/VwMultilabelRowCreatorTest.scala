package com.eharmony.aloha.dataset.vw.multilabel

import com.eharmony.aloha.dataset.{MissingAndErroneousFeatureInfo, SparseFeatureExtractorFunction}
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc0}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan.deak on 9/22/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelRowCreatorTest {
  import VwMultilabelRowCreatorTest._

  @Test def testSharedIsPresentWhenNoFeaturesSpecified(): Unit = {
    val rc = rowCreator(0)
    val a = output(rc(X))
    val expected = SharedPrefix +: (DummyLabels ++ AllNegative)
    assertEquals(expected, a.toList)
  }

  @Test def testOneFeatureNoPos(): Unit = {
    val rc = rowCreator(numFeatures = 1)
    val a = output(rc(X))

    val shared = s"$SharedPrefix| f1"
    val expected = shared +: (DummyLabels ++ AllNegative)
    assertEquals(expected, a.toList)
  }
}

object VwMultilabelRowCreatorTest {
  private type Domain = Map[String, Any]
  private type Label = String
  private val Omitted = ""
  private val LabelsInTrainingSet = Vector("zero", "one", "two")
  private val NegDummyClass = Int.MaxValue.toLong + 1
  private val PosDummyClass = NegDummyClass + 1
  private val PosVal = -1
  private val NegVal = 0
  private val X = Map.empty[String, Any]
  private val SharedPrefix = "shared "

  private val DummyLabels = List(
    s"$NegDummyClass:$NegVal |y N",
    s"$PosDummyClass:$PosVal |y P"
  )

  private val AllNegative = LabelsInTrainingSet.indices.map(i => s"$i:$NegVal |Y _$i")


  private def output(out: (MissingAndErroneousFeatureInfo, Array[String])) = out._2

  private[this] val featureFns = SparseFeatureExtractorFunction[Domain](Vector(
    "f1" -> GenFunc0(Omitted, _ => Seq(("", 1d))),
    "f2" -> GenFunc0(Omitted, _ => Seq(("", 2d)))
  ))

  private def featureFns(n: Int) = {
    val ff = (1 to n) map { i =>
      s"f$i" -> GenFunc0(Omitted, (_: Any) => Seq(("", i.toDouble)))
    }

    SparseFeatureExtractorFunction[Domain](ff)
  }

  private def positiveLabels(ps: Label*): GenAggFunc[Any, Vector[Label]] = {
    GenFunc0(Omitted, _ => Vector(ps:_*))
  }

  private def rowCreator(numFeatures: Int, posLabels: Label*): VwMultilabelRowCreator[Domain, Label] = {
    val ff = featureFns(numFeatures)
    val pos = positiveLabels(posLabels:_*)
    StdRowCreator.copy(
      featuresFunction = ff,
      defaultNamespace = ff.features.indices.toList,
      positiveLabelsFunction = pos
    )
  }

  private[this] val StdRowCreator: VwMultilabelRowCreator[Domain, Label] = {
    val ff = featureFns(0)

    VwMultilabelRowCreator[Domain, Label](
      allLabelsInTrainingSet = LabelsInTrainingSet,
      featuresFunction = ff,
      defaultNamespace = ff.features.indices.toList,  // All features in default NS.
      namespaces = List.empty[(String, List[Int])],
      normalizer = Option.empty[CharSequence => CharSequence],
      positiveLabelsFunction = positiveLabels()
    )
  }
}
