package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.models.reg.RegressionFeatures
import com.eharmony.aloha.semantics.func.GenAggFunc

import scala.collection.{immutable => sci, mutable => scm}

/**
  * Created by sahil-goyal on 11/7/16.
  */
trait VwMultiLineFeatures[A] extends RegressionFeatures[A] {

  val labelDomainFn: GenAggFunc[A, sci.IndexedSeq[Any]]
  val labelDependentFeatureNames: sci.IndexedSeq[String]
  val labelDependentFeatureFunctions: sci.IndexedSeq[GenAggFunc[Any, Iterable[(String, Double)]]]
  val numMissingLDFThreshold: Option[Int]

  /**
    * Extract the features from the raw data by mapping ''featureFunctions'' over the input.  If
    * ''numMissingThreshold'' is not None and the number of resulting empty Iterables exceeds the
    * ''numMissingThreshold'' value, then the resulting ''Features.missingOk'' value is '''false''';
    * otherwise, it will be '''true'''.  If ''Features.missingOk'' is '''false''', then go back and
    * check all feature functions for missing values and add findings to the ''Features.missing''
    * map.  This ''Features.missing'' is a mapping from the feature specification to the list of
    * variable names whose associated values are missing from the input.
    *
    * @param a raw input data of the model input type.
    * @return a Features instance with the following:
    *           1 the transformed input vector
    *           1 the map of bad features to the missing values in the raw data that were needed to compute the feature
    *           1 whether the amount of missing data is acceptable to still continue
    */
  protected[this] def constructMultiLineFeatures(a: A): Features[(IndexedSeq[Iterable[(String, Double)]], Array[IndexedSeq[Iterable[(String, Double)]]])] = {
    // NOTE: Since this function is at the center of the regression process and will be called many times, it
    //       needs to be efficient.  Therefore, it uses some things that are not idiomatic scala.  For instance,
    //       there are mutable variables, while loops instead of for comprehensions or Range.foreach, etc.

    val sharedFeatures = constructFeatures(a)
    val missing = sharedFeatures.missing
    val labels = labelDomainFn(a)
    val numLabels = labels.size
    val n = labelDependentFeatureNames.size
    val missingLabelDependentFeatures = new Array[scm.Map[String, Seq[String]]](numLabels)
    val f = new Array[Array[Iterable[(String, Double)]]](numLabels)
    val labelDependentFeatures = new Array[IndexedSeq[Iterable[(String, Double)]]](numLabels)
    var i = 0
    while(i < numLabels) {
      missingLabelDependentFeatures(i) = scm.Map.empty[String, Seq[String]]
      f(i) = new Array[Iterable[(String, Double)]](n)
      var j = 0
      while (j < n) {

        // Use concat based on
        //   http://stackoverflow.com/questions/5076740/whats-the-fastest-way-to-concatenate-two-strings-in-java
        //   http://stackoverflow.com/questions/47605/string-concatenation-concat-vs-operator
        f(i)(j) = labelDependentFeatureFunctions(j)(labels(i)).map(p => (labelDependentFeatureNames(j).concat(p._1), p._2))

        // If the feature is empty, it can't contribute to the inner product.  If it can't contribute to the
        // inner product but appears in the inner product specification, there are two possibilities:
        //
        //   1) The specifier doesn't care about performance and needlessly added a NoOp.
        //   2) The feature could emit a value because data necessary to do so is missing.
        //
        // In either case, we take those opportunities to check for missing data and assume the performance
        // hit is acceptable.
        if (f(i)(j).isEmpty) {
          missingLabelDependentFeatures(i) += (labelDependentFeatureFunctions(j).specification -> labelDependentFeatureFunctions(j).accessorOutputMissing(labels(i)))
          missing += (labelDependentFeatureFunctions(j).specification -> labelDependentFeatureFunctions(j).accessorOutputMissing(labels(i)))
        }

        j += 1
      }
      labelDependentFeatures(i) = new collection.mutable.WrappedArray.ofRef(f(i))
      i += 1
    }

    val numMissingLDFOk = numMissingLDFThreshold.forall(t => missingLabelDependentFeatures.forall(_.size <= t))

    val numMissingOk = sharedFeatures.missingOk && numMissingLDFOk

    // If we are going to err out, allow a linear scan (with repeated work so that we can get richer error
    // diagnostics.  Only include the values where the list of missing accessors variables is not empty.
    // This could have been done with a for comprehension but this is a little faster.
    if (!numMissingLDFOk) {
      0 until numLabels foreach { i =>
        0 until n foreach { j =>
          val miss = labelDependentFeatureFunctions(j).accessorOutputMissing(labels(i))
          if(miss.nonEmpty)
            missing += labelDependentFeatureFunctions(j).specification -> miss
        }
      }
    }

    Features((sharedFeatures.features, labelDependentFeatures), missing, numMissingOk)
  }

}
