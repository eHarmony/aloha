package com.eharmony.aloha.models

import com.eharmony.aloha.dataset.density.Sparse

import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 8/31/17.
  */
package object multilabel {

  /**
    * Features about the input value (NOT including features based on labels).
    * This should probably be a `sci.IndexedSeq[Sparse]` but `RegressionFeatures`
    * returns a `collection.IndexedSeq` and using
    * [[com.eharmony.aloha.models.reg.RegressionFeatures.constructFeatures]] is
    * preferable and will provide consistent results across many model types.
    */
  private type SparseFeatures = IndexedSeq[Sparse]

  /**
    * Indices of the labels for which predictions should be produced into the
    * sequence of all labels.  Indices will be sorted in ascending order.
    */
  private type LabelIndices = sci.IndexedSeq[Int]

  /**
    * Labels for which predictions should be produced.  This can be an improper subset of all labels.
    * `Labels` align with `LabelIndices` meaning `LabelIndices[i]` will give the index of `Labels[i]`
    * into the sequence of all labels a model knows about.
    *
    * @tparam K the type of labels (or classes in the machine learning literature).
    */
  private type Labels[K] = sci.IndexedSeq[K]

  /**
    * Sparse features related to the labels.  Other outer sequence aligns with the `Labels` and `LabelIndices`
    * sequences meaning `SparseLabelDepFeatures[i]` relates to the features of `Labels[i]`.
    */
  private type SparseLabelDepFeatures = Labels[SparseFeatures]

  /**
    * A sparse multi-label predictor takes:
    *
    - features
    - labels for which a prediction should be produced
    - indices of those labels into sequence of all of the labels the model knows about.
    - label dependent-features
    *
    * and returns a Map from the labels passed in, to the prediction associated with the label.
    *
    * '''NOTE''': This is exposed as package private for testing.
    *
    * @tparam K the type of labels (or classes in the machine learning literature).
    */
  private[multilabel] type SparseMultiLabelPredictor[K] =
    (SparseFeatures, Labels[K], LabelIndices, SparseLabelDepFeatures) => Map[K, Double]

  /**
    * A lazy version of a sparse multi-label predictor.  It is a curried zero-arg function that
    * produces a sparse multi-label predictor.
    *
    * This definition is "lazy" because we can't guarantee that the underlying predictor is
    * `Serializable` so we pass around a function that can be cached in a ''transient''
    * `lazy val`.  This function should however be `Serializable` and testing should be done
    * to ensure that each predictor producer is `Serializable`.
    *
    * @tparam K the type of labels (or classes in the machine learning literature).
    */
  type SparsePredictorProducer[K] = () => SparseMultiLabelPredictor[K]
}
