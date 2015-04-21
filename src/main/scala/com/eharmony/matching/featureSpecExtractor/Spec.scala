package com.eharmony.matching.featureSpecExtractor

import scala.collection.{immutable => im}

trait Spec[A] extends Serializable {
    protected[this] def featureSpecs: im.IndexedSeq[FeatureSpec]
    protected[this] def namespaces: im.ListMap[String, im.IndexedSeq[Int]]
    protected[this] def featuresFunction: A => (MissingAndErroneousFeatureInfo, Seq[Iterable[(String, Double)]])
    protected[this] def defaultNamespace: im.IndexedSeq[Int]

    def toInput(data: A): (MissingAndErroneousFeatureInfo, String)
    def toInput(data: A,includeZeroValues: Boolean): (MissingAndErroneousFeatureInfo, String)
}

/*

public abstract class Spec<T> implements Serializable {

    protected final List<JavaFeatureSpec> featureSpecs;
    protected final List<Map.Entry<String, List<Integer>>> namespaces; // names and feature indexes in featureSpecs
    protected final MissingFeaturesFunction<T> featuresFunction;
    protected final List<Integer> defaultNamespace; // ordered indexes in featureSpecs of default namespace features

    public Spec(
            List<JavaFeatureSpec> featureSpecs,
            List<Map.Entry<String, List<Integer>>> namespaces,
            MissingFeaturesFunction<T> featuresFunction) {
        this.featureSpecs = featureSpecs;
        this.namespaces = namespaces;
        this.featuresFunction = featuresFunction;

        defaultNamespace = new ArrayList<Integer>();
        Set<Integer> nameSpacedFeatures = new HashSet<Integer>(featureSpecs.size());
        for (Map.Entry<String, List<Integer>> namespace : namespaces) {
            for (Integer featureIndex : namespace.getValue()) {
                nameSpacedFeatures.add(featureIndex);
            }
        }

        for (int i = 0; i < featureSpecs.size(); i++) {
            if (!nameSpacedFeatures.contains(i))
                defaultNamespace.add(i);
        }
    }

    public Tuple2<MissingAndErroneousFeatureInfo, String> toInput(T data) {
        return toInput(data, false);
    }

    public Tuple2<MissingAndErroneousFeatureInfo, String> toInput(T data, boolean includeZeroValues) {
        return toInput(data, includeZeroValues, new StringBuilder());
    }

    protected abstract Tuple2<MissingAndErroneousFeatureInfo, String> toInput(T data, boolean includeZeroValues, StringBuilder sb);
}


*/
