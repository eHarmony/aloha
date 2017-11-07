package com.eharmony.aloha.dataset.vw.multilabel

import com.eharmony.aloha.dataset.vw.multilabel.json.VwMultilabeledJson
import com.eharmony.aloha.dataset.{MissingAndErroneousFeatureInfo, SparseFeatureExtractorFunction}
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc0}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.pimpString
import com.eharmony.aloha.semantics.compiled.CompiledSemanticsInstances

import scala.collection.breakOut

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

  @Test def testRowCreationFromJson(): Unit = {

    // Some labels. These are intentionally out of order.  Don't change.
    val allLabelInts = Vector(7, 8, 6)

    // Run tests for the entire power set of positive labels.
    val tests = Seq(
      //         positive labels:
      1,             // {}
      6,             // {6}
      7,             // {7}
      8,             // {8}
      6 * 7,         // {6, 7}
      6 * 8,         // {6, 8}
      7 * 8,         // {7, 8}
      6 * 7 * 8      // {6, 7, 8}
    )

    // Could throw, but if so, test should fail.
    val rc = getRowCreator(allLabelInts)

    tests foreach { test =>
      val exp = positiveAndNegativeValues(test, allLabelInts)
      val (_, act) = rc(test)
      testOutput(test, exp, act, MultilabelOutputPrefix, rc.classNs)
    }
  }

  private[this] def getRowCreator(allLabelInts: Vector[Int]) = {
    val rcProducer =
      new VwMultilabelRowCreator.Producer[Int, Label](allLabelInts.map(y => y.toString))

    val rcTry = rcProducer.getRowCreator(
      CompiledSemanticsInstances.anyNameIdentitySemantics[Int],
      MultilabelDatasetJson
    )

    // Don't care about calling `.get`.  If it fails, the test will appropriately blow up.
    rcTry.get
  }

  private[this] def positiveAndNegativeValues(n: Int, allLabels: Seq[Int]): Seq[Boolean] = {
    val divisorsN = divisors(n)
    val labelInd: Map[Int, Int] = allLabels.zipWithIndex.toMap
    val allLabelSet = labelInd.keySet
    val pos = allLabelSet intersect divisorsN
    val neg = allLabelSet diff divisorsN

    // Because pos and neg form a partition of allLabelSet, labelInd.apply is safe.
    val posAndNeg =
      pos.map(p => labelInd(p) -> true) ++
      neg.map(p => labelInd(p) -> false)

    posAndNeg
      .toSeq
      .sorted
      .map { case (_, isPos) => isPos }
  }

  private[this] def testOutput(
      n: Int,
      expectedResults: Seq[Boolean],
      actualResults: Array[String],
      prefix: Seq[String],
      labelNs: Char
  ): Unit = {

    val suffix =
      expectedResults.zipWithIndex map { case (isPos, i) =>
        s"$i:${if (isPos) PosVal else NegVal} |$labelNs _$i"
      }

    assertEquals(prefix, actualResults.take(prefix.size).toSeq)
    assertEquals(suffix, actualResults.drop(prefix.size).toSeq)
  }


  /**
    * Get all divisors of `n`.
    * This is not super efficient: O(sqrt(N))
    * @param n value for which divisors are desired.
    * @return
    */
  private[this] def divisors(n: Int): Set[Int] =
    (1 to math.sqrt(n).ceil.toInt).flatMap {
      case i if n % i == 0 => List(i, n / i)
      case _ => Nil
    }(breakOut)
}

object VwMultilabelRowCreatorTest {
  private type Domain = Map[String, Any]
  private type Label = String
  private val Omitted = ""
  private val LabelsInTrainingSet = Vector("zero", "one", "two")
  private val NegDummyClass = Int.MaxValue.toLong + 1
  private val PosDummyClass = NegDummyClass + 1
  private val PosVal = 0
  private val NegVal = 1
  private val X = Map.empty[String, Any]
  private val SharedPrefix = "shared "

  private val DummyLabels = List(
    s"$NegDummyClass:$NegVal |y N",
    s"$PosDummyClass:$PosVal |y P"
  )

  private val AllNegative = LabelsInTrainingSet.indices.map(i => s"$i:$NegVal |Y _$i")

  // Notice the positive labels.  This says if the function input is 0 (mod v), where v is
  // one of {6, 7, 8}, then make the example a positive example for the associated label.
  //
  // This JSON should be used with CompiledSemanticsInstances.anyNameIdentitySemantics[Int].
  // This variable `fn_input` works because of the way that the semantics works.  It just
  // returns the `fn_input` is just the input value of the function.
  //
  // All of the features are invariant to the input.
  private[aloha] val MultilabelDatasetJson =
    """
      |{
      |  "imports": [
      |    "com.eharmony.aloha.feature.BasicFunctions._"
      |  ],
      |  "features": [
      |    { "name": "f1", "spec": "1" },
      |    { "name": "f2", "spec": "1" }
      |  ],
      |  "namespaces": [
      |    { "name": "ns1", "features": [ "f1" ] },
      |    { "name": "ns2", "features": [ "f2" ] }
      |  ],
      |  "normalizeFeatures": false,
      |  "positiveLabels": "(6 to 8).flatMap(v => List(v.toString).filter(_ => ${fn_input} % v == 0))"
      |}
    """.stripMargin.parseJson.convertTo[VwMultilabeledJson]

  // This is what the first lines of the output are expected to be.  This doesn't change b/c
  // the features (and dummy class definitions) in MultilabelDatasetJson are invariant to
  // the input.
  private[aloha] val MultilabelOutputPrefix = Seq(
    "shared |ns1 f1 |ns2 f2",    // due to definition of features and namespaces.
    s"2147483648:$NegVal |y N",  // negative dummy class
    s"2147483649:$PosVal |y P"   // positive dummy class
  )

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
    val labelNss = VwMultilabelRowCreator.determineLabelNamespaces(Set.empty).get

    VwMultilabelRowCreator[Domain, Label](
      allLabelsInTrainingSet = LabelsInTrainingSet,
      featuresFunction = ff,
      defaultNamespace = ff.features.indices.toList,  // All features in default NS.
      namespaces = List.empty[(String, List[Int])],
      normalizer = Option.empty[CharSequence => CharSequence],
      positiveLabelsFunction = positiveLabels(),
      classNs = labelNss.labelNs,
      dummyClassNs = labelNss.dummyLabelNs
    )
  }
}
