package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor._
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec
import com.eharmony.matching.featureSpecExtractor.vw.{Vw, VwFeatureNormalizer}

import scala.util.Try

class VwSpecProducer extends SpecProducer with CovariateProducer with CompilerFailureMessages {
    def specProducerName: String = getClass.getSimpleName
    def appliesTo(jsonSpec: JsonSpec): Boolean = jsonSpec.specType.exists(_ == Vw.identifier)
    def getSpec[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec): Try[Spec[A]] = {
        val (covariates, default, nss, normalizer) = getInputs(semantics, jsonSpec)
        val spec = covariates.map(c => new VwSpec(c, default, nss, normalizer))
        spec
    }

    /**
     *
     * @param semantics
     * @param jsonSpec
     * @tparam A
     * @return (covariates, default, namespaces, optional normalizer)
     */
    protected[this] def getInputs[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec) = {

        // Attempt to create the covariate data.
        val covariates: Try[FeatureExtractorFunction[A]] = getCovariates[A](semantics, jsonSpec)

        // Get the namespace information.
        val (default, nss) = jsonSpec.namespaceIndices()

        // If we should normalize the feature, create get the proper normalizer.
        val normalizer = if (jsonSpec.shouldNormalizeFeatures) Some(VwFeatureNormalizer.instance) else None

        (covariates, default, nss, normalizer)
    }
}
