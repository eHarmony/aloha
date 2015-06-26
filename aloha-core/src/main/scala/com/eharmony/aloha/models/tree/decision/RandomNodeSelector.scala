package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.util.rand.HashedCategoricalDistribution
import com.eharmony.aloha.semantics.func.GenAggFunc

/** A selector that random selects a child node.
  * @param features features on which the hash is based.  Notice that function output type is Any.
  * @param distribution a distribution used for selecting values.
  * @param missingOk whether it is OK to hash on missing data.  Keep in mind that if set to true, there is no
  *                  guarantee about what value will be selected.  (Missing data in this context means None.  There are
  *                  no explicit null checks; just None checks.)
  * @tparam A the input type from which features are extracted.
  */
case class RandomNodeSelector[-A](
        features: Seq[GenAggFunc[A, Any]],
        distribution: HashedCategoricalDistribution,
        missingOk: Boolean = false) extends NodeSelector[A] {

    require(1 <= features.size, "There should be at least one feature on which the hash is based.  Received 0 features.")

    /** "Randomly" but ''idempotently'' pick an index of a sub-tree down which to branch.
      * Compute a uniformly distributed hash code from the features specified to the constructor and then use it to
      * drive alias method sampling to select a sub-tree branch with the desired probability.  Since the uniformly
      * generated variates are based on hashing rather than a random number generator, the selection is idempotent.
      *
      * '''Note''': This uses the MurmurHash3 singleton in Scala rather than Guava's
      *       [[http://docs.guava-libraries.googlecode.com/git-history/v11.0/javadoc/src-html/com/google/common/hash/Hashing.html#line.81 com.google.common.hash.Hashing.murmur3_32]]
      *       implementation for speed and compatibility with Scala. Therefore, there should be no expectation that
      *       the hashcodes produced by the different methods will produce the same hashes on the same data.
      * @param a input from which features are extracted.  These features are then hashed to produce a value.
      * @return a positive value i if node i should be selected.  May return a negative value in which case processErrorAt
      *         should be called with the value returned.
      */
    def apply(a: A) = {
        // Compute the features.  Note that we don't use flatMap because:
        //   [Some(x_1), ..., Some(x_i-1), None, Some(v), Some(x_i+2), ..., Some(x_n)]    AND
        //   [Some(x_1), ..., Some(x_i-1), Some(v), None, Some(x_i+2), ..., Some(x_n)]
        // produce different hashes but if we applied flatMap, they would give the same hash value.
        val x = features.map(_(a))

        val missingIndex = x.indexWhere(_ == None)
        val i = if (-1 == missingIndex || missingOk) distribution(x)
                else -missingIndex - 1
        i
    }

    /** Process an error.
      * @param a input to decision tree model
      * @param i the value returned by apply. This should be a negative number whose absolute value represents
      *          the index of the feature that contained missing data.
      * @return
      */
    def processErrorAt(a: A, i: Int): ErrorsAndMissing = {
        val missing = features.flatMap(_.accessorOutputMissing(a)).distinct.sorted
        val em = ErrorsAndMissing(Seq(s"Couldn't randomly branch due to missing features: ${missing.mkString(", ")} in " + this), missing)
        em
    }
}
