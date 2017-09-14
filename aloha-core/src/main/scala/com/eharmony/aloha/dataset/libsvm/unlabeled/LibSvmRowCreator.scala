package com.eharmony.aloha.dataset.libsvm.unlabeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.libsvm.unlabeled.json.LibSvmUnlabeledJson
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.util.Logging
import com.eharmony.aloha.util.hashing.HashFunction
import spray.json.JsValue

import scala.collection.{immutable => sci}
import scala.util.Try

class LibSvmRowCreator[-A](
        covariates: FeatureExtractorFunction[A, Sparse],
        hash: HashFunction,
        numBits: Int = LibSvmRowCreator.DefaultBits
) extends RowCreator[A, CharSequence] {
    require(1 <= numBits && numBits <= 31, s"numBits must be in {1, 2, ..., 31}.  Found $numBits")

    private[this] val mask = (1 << numBits) - 1

    def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val (missingInfo, cov) = covariates(data)

        // Get the features into key-value pairs where the keys are the hashed strings and the values
        // are the original values.  Make sure to sort.
        //
        // Use aggregate for efficiency.  For intuition, see http://stackoverflow.com/a/6934302/4484580
        //
        // TODO: Figure out parallel strategy.
        // For cov with large outer sequence size and small inner sequence size, non parallel is faster.
        // For cov with small outer sequence size and large inner sequence size, parallel (cov.par) is faster.
        // Both of these seem to be faster than flat mapping over cov like:
        //
        //   val f = for ( xs <- cov; (k, v) <- xs ) yield (hash.newHasher.putString(k).hash.asInt & mask, v)
        //   val sortedDeduped = sci.OrderedMap(f:_*)
        //
        val sortedDeduped = cov.aggregate(sci.SortedMap.empty[Int, Double])(
//            (z, kvs) => kvs.foldLeft(z){ case (m, (k, v)) => m + ((hash.newHasher.putString(k).hash.asInt & mask, v)) },
            // TODO: Test this
            (z, kvs) => kvs.foldLeft(z){ case (m, (k, v)) => m + ((hash.stringHash(k) & mask, v)) },
            (m1, m2) => m1 ++ m2
        )

        val out = sortedDeduped.view.map { case(k, v) => s"$k:$v" }.mkString(" ")
        (missingInfo, out)
    }
}

final object LibSvmRowCreator {
    val DefaultBits = 18

    final case class Producer[A]()
        extends RowCreatorProducer[A, CharSequence, LibSvmRowCreator[A]]
        with RowCreatorProducerName
        with SparseCovariateProducer
        with CompilerFailureMessages
        with Logging {

        type JsonType = LibSvmUnlabeledJson
        def parse(json: JsValue): Try[LibSvmUnlabeledJson] = Try { json.convertTo[LibSvmUnlabeledJson] }
        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: LibSvmUnlabeledJson): Try[LibSvmRowCreator[A]] = {
            val covariates: Try[FeatureExtractorFunction[A, Sparse]] = getCovariates(semantics, jsonSpec)
            val hash: HashFunction = jsonSpec.salt match {
                case Some(s) => new com.eharmony.aloha.util.hashing.MurmurHash3(s)
                case None    => com.eharmony.aloha.util.hashing.MurmurHash3
            }
            warn(hash.salts)
            covariates.map(c => jsonSpec.numBits.fold(new LibSvmRowCreator(c, hash))(b => new LibSvmRowCreator(c, hash, b)))
        }
    }

}
