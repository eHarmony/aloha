package com.eharmony.aloha.dataset

import com.eharmony.aloha.dataset.density.{Sparse, Dense}
import com.eharmony.aloha.semantics.func.GenAggFunc

import scala.collection.{immutable => sci}
import scala.reflect.{ClassTag, classTag}

final case class SparseFeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])])
extends FeatureExtractorFunction[A, Iterable[(String, Double)]] {
    protected[this] val postProcessingFunction = (name: String, b: Sparse) => b.map(p => (name + p._1, p._2))
    protected[this] implicit def ctB(): ClassTag[Sparse] = classTag[Sparse]
}

final case class DenseFeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, Double])])
    extends FeatureExtractorFunction[A, Double] {
    protected[this] val postProcessingFunction = (_: String, b: Dense) => b
    protected[this] implicit def ctB(): ClassTag[Dense] = classTag[Dense]
}

final case class StringFeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, String])])
    extends FeatureExtractorFunction[A, String] {
    protected[this] val postProcessingFunction = (_: String, b: String) => b
    protected[this] implicit def ctB(): ClassTag[String] = classTag[String]
}

final case class StringSeqFeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, Seq[String]])])
  extends FeatureExtractorFunction[A, Seq[String]] {
  protected[this] val postProcessingFunction = (_: String, b: Seq[String]) => b
  protected[this] implicit def ctB(): ClassTag[Seq[String]] = classTag[Seq[String]]
}

final case class OptionAnySeqFeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, Seq[Option[Any]]])])
  extends FeatureExtractorFunction[A, Seq[Option[Any]]] {
  override protected[this] val postProcessingFunction = (_: String, b: Seq[Option[Any]]) => b
  protected[this] implicit def ctB(): ClassTag[Seq[Option[Any]]] = classTag[Seq[Option[Any]]]
}


/**
 * A function that takes a value and returns extracted features and information on missing and erring features.
 * @tparam A
 * @tparam Density
 */
// TODO: Merge this with com.eharmony.aloha.models.reg.RegressionFeatures.
trait FeatureExtractorFunction[-A, @specialized(Double) Density] extends (A => (MissingAndErroneousFeatureInfo, IndexedSeq[Density])) {
    val features: sci.IndexedSeq[(String, GenAggFunc[A, Density])]
    protected[this] val postProcessingFunction: (String, Density) => Density
    protected[this] implicit def ctB(): ClassTag[Density]

    /**
     * @param a an input
     * @return information about which feature names are missing or in err in addition to the expanded features.
     */
    def apply(a: A): (MissingAndErroneousFeatureInfo, IndexedSeq[Density]) = {
        def h(l: Array[Density], i: Int, n: Int, missing: List[String], erring: List[String]): (MissingAndErroneousFeatureInfo, IndexedSeq[Density]) = {
            if (i >= n)
                (MissingAndErroneousFeatureInfo(missing.reverse, erring.reverse), l)
            else {
                val (name, feature) = features(i)
                // Prefix the feature value tuple key by the feature name.
                l(i) = postProcessingFunction(name, feature(a))
                val problems = feature.accessorOutputProblems(a)
                h(l, i + 1, n,
                    if (problems.missing.nonEmpty) name :: missing else missing,
                    if (problems.errors.nonEmpty) name :: erring else erring)
            }
        }

        val n = features.size
        implicit val b = ctB() // Need this so that the class tag can be found so that the array can be created.
        h(new Array[Density](n), 0, n, Nil, Nil)
    }
}
