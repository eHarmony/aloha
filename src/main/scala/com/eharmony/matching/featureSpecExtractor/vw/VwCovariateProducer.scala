package com.eharmony.matching.featureSpecExtractor.vw

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.SparseCovariateProducer
import com.eharmony.matching.featureSpecExtractor.json.SparseSpec
import com.eharmony.matching.featureSpecExtractor.vw.json.VwJsonLike

trait VwCovariateProducer[A] { self: SparseCovariateProducer =>

    /**
     * @param semantics
     * @param json
     * @return (covariates, default, namespaces, optional normalizer)
     */
    protected[this] def getVwData(semantics: CompiledSemantics[A], json: VwJsonLike) = {

        // Attempt to create the covariate data.  When no default is given, default to an empty sequence of KV Pairs.
        val covariates = getCovariates(semantics, json, SparseSpec.defVal)

        // Get the namespace information.
        val (default, nss) = json.namespaceIndices()

        // If we should normalize the feature, create get the proper normalizer.
        val normalizer = if (json.shouldNormalizeFeatures) Some(VwFeatureNormalizer.instance) else None

        (covariates, default, nss, normalizer)
    }
}
