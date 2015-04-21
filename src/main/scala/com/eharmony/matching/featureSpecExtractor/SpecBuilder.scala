package com.eharmony.matching.featureSpecExtractor

import scala.util.{Failure, Try}
import scala.collection.{immutable => sci}
import java.io.{File, IOException}
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc


/**

package com.eharmony.matching.featureSpecExtractor;

import com.eharmony.matching.aloha.conversions.sparse.FeatureExpander;
import com.eharmony.matching.aloha.conversions.sparse.JavaFeatureSpec;
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemanticsPlugin;
import com.eharmony.matching.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin;
import com.eharmony.matching.aloha.semantics.func.GenAggFunc;
import com.eharmony.matching.featureSpecExtractor.libsvm.LabelLibSVMSpec;
import com.eharmony.matching.featureSpecExtractor.libsvm.LibSVMSpec;
import com.eharmony.matching.featureSpecExtractor.vw.ContextualBanditsVwSpec;
import com.eharmony.matching.featureSpecExtractor.vw.LabelVwSpec;
import com.eharmony.matching.featureSpecExtractor.vw.SingleFunctionCreator;
import com.eharmony.matching.featureSpecExtractor.vw.VwSpec;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.collection.IndexedSeq;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpecBuilder {
    private static final int DEFAULT_HASH_BITS = 18;

    protected static final Logger log = LoggerFactory.getLogger(SpecBuilder.class);

    public static final <T extends GeneratedMessage> Spec<T> buildVwFromJson(
            String vwJsonSpecFile,
            T prototype) throws IOException {
        return buildFromJson(vwJsonSpecFile, prototype, null, SpecType.VW, Optional.<Integer>absent());
    }

    public static final <T extends GeneratedMessage> Spec<T> buildVwFromJson(
            FileObject vwJsonSpecFile,
            T prototype) throws IOException {
        return buildFromJson(vwJsonSpecFile, prototype, null, SpecType.VW, Optional.<Integer>absent());
    }

    public static final <T extends GeneratedMessage> Spec<T> buildVwFromJson(
            String vwJsonSpecFile,
            T prototype,
            String tempFile) throws IOException {
        return buildFromJson(vwJsonSpecFile, prototype, tempFile, SpecType.VW, Optional.<Integer>absent());
    }

    public static final <T extends GeneratedMessage> Spec<T> buildLibSVMFromJson(
            String vwJsonSpecFile,
            T prototype,
            int numBits) throws IOException {
        return buildFromJson(vwJsonSpecFile, prototype, null, SpecType.LibSVM, Optional.of(numBits));
    }

    public static final <T extends GeneratedMessage> Spec<T> buildFromJson(
            String vwJsonSpecFile,
            T prototype,
            String preCompiledDirectory,
            SpecType specType,
            Optional<Integer> numBits)
            throws IOException {
        return buildFromJson(VFS.getManager().resolveFile(vwJsonSpecFile),
                preCompiledDirectory == null ? null : new File(preCompiledDirectory),
                new CompiledSemanticsProtoPlugin<T>(prototype),
                specType,
                numBits);
    }

    public static final <T extends GeneratedMessage> Spec<T> buildFromJson(
            FileObject jsonSpecFile,
            T prototype,
            String preCompiledDirectory,
            SpecType specType,
            Optional<Integer> numBits)
            throws IOException {
        return buildFromJson(jsonSpecFile,
                preCompiledDirectory == null ? null : new File(preCompiledDirectory),
                new CompiledSemanticsProtoPlugin<T>(prototype),
                specType,
                numBits);
    }

    public static final <T> Spec<T> buildVwFromJson(
            FileObject jsonSpecFile,
            File preCompiledDirectory,
            CompiledSemanticsPlugin<T> plugin) throws IOException{
        return buildFromJson(jsonSpecFile,
                preCompiledDirectory,
                plugin,
                SpecType.VW,
                Optional.<Integer>absent());
    }

    public static final <T> Spec<T> buildFromJson(
            FileObject jsonSpecFile,
            File preCompiledDirectory,
            CompiledSemanticsPlugin<T> plugin,
            SpecType specType,
            Optional<Integer> numBits) throws IOException {
        JsonSpecPojo jsonSpecPojo = JsonSpecPojo.readJsonPojo(jsonSpecFile);
        return buildFromJson(jsonSpecPojo, preCompiledDirectory, plugin, specType, numBits);
    }

    public static final <T> Spec<T> buildFromJson(
            String jsonSpec,
            File preCompiledDirectory,
            CompiledSemanticsPlugin<T> plugin,
            SpecType specType,
            Optional<Integer> numBits) throws IOException {
        JsonSpecPojo jsonSpecPojo = JsonSpecPojo.readJsonPojoAsString(jsonSpec);
        return buildFromJson(jsonSpecPojo, preCompiledDirectory, plugin, specType, numBits);
    }

    public static final <T> Spec<T> buildFromJson(
            JsonSpecPojo jsonSpecPojo,
            File preCompiledDirectory,
            CompiledSemanticsPlugin<T> plugin,
            SpecType specType,
            Optional<Integer> numBits) throws IOException {
        validate(jsonSpecPojo);

        SingleFunctionCreator<T> c = new SingleFunctionCreator<T>(plugin, jsonSpecPojo.imports, preCompiledDirectory);
        List<JavaFeatureSpec> featureSpecs = getFeatureSpecList(jsonSpecPojo);

        Spec<T> spec;
        if (StringUtils.isNotEmpty(jsonSpecPojo.label)) {
            if (specType == SpecType.VW) {
                return new LabelVwSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        jsonSpecPojo.normalizeFeatures,
                        c.createSingleFunction(jsonSpecPojo.label, ""),
                        StringUtils.isEmpty(jsonSpecPojo.importance) ? null : c.createSingleFunction(jsonSpecPojo.importance, ""));
            }
            else {
                return new LabelLibSVMSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        numBits.or(DEFAULT_HASH_BITS),
                        c.createSingleFunction(jsonSpecPojo.label, ""));
            }
        }
        else if (StringUtils.isNotEmpty(jsonSpecPojo.cbAction)) {
            spec = new ContextualBanditsVwSpec<T>(
                    featureSpecs,
                    getNamespaces(jsonSpecPojo),
                    toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                    jsonSpecPojo.normalizeFeatures,
                    c.createSingleFunction(jsonSpecPojo.cbAction, ""),
                    c.createSingleFunction(jsonSpecPojo.cbCost, ""),
                    c.createSingleFunction(jsonSpecPojo.cbProbability, ""));
        }
        else {
            if (specType == SpecType.VW) {
                spec = new VwSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        jsonSpecPojo.normalizeFeatures);
            }
            else {
                spec = new LibSVMSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        numBits.or(DEFAULT_HASH_BITS));
            }
        }

        return spec;
    }

    public static final List<JavaFeatureSpec> buildFeatureSpecs(JsonSpecPojo jsonSpecPojo) throws IOException {
        validate(jsonSpecPojo);
        return getFeatureSpecList(jsonSpecPojo);
    }

    private static void validate(JsonSpecPojo jsonSpecPojo) {
        Optional<String> errorString = validateWithString(jsonSpecPojo);
        if (errorString.isPresent()) {
            throw new IllegalArgumentException(errorString.get());
        }
    }

    public static Optional<String> validateWithString(JsonSpecPojo jsonSpecPojo) {
        Set<String> uniqueNames = new HashSet<String>();
        for (JsonSpecPojo.JsonFeature jsonFeature : jsonSpecPojo.features) {
            if (jsonFeature.defVal != null) {
                for (List<Object> defaults : jsonFeature.defVal) {
                    if (defaults.size() != 2 || !(defaults.get(0) instanceof String) || !(defaults.get(1) instanceof Double))
                        return Optional.of("default is not properly formatted for feature: " + jsonFeature.name);
                }
            }
            if (uniqueNames.contains(jsonFeature.name))
                return Optional.of("duplicate feature name: " + jsonFeature.name);
            uniqueNames.add(jsonFeature.name);
        }

        HashSet<String> allFeatures = new HashSet<String>(uniqueNames);

        uniqueNames = new HashSet<String>();
        HashSet<String> namespaceNames = new HashSet<String>();
        for (JsonSpecPojo.JsonNamespace namespace : jsonSpecPojo.namespaces) {

            if (namespaceNames.contains(namespace.name))
                return Optional.of("namespace: " + namespace.name + " is defined more than once!");
            namespaceNames.add(namespace.name);

            for (String featureName : namespace.features) {
                if (uniqueNames.contains(featureName))
                    return Optional.of("cannot have the same feature in multiple namespaces (or multiple times in the same namespace): "
                            + featureName);
                if (!allFeatures.contains(featureName))
                    return Optional.of("feature: " + featureName + " in namespace: " + namespace.name
                            + " is not actually defined!");
                uniqueNames.add(featureName);
            }

        }
        return Optional.absent();
    }

    // Refactor this to take in a Semantics<T> and then build up from there
    protected final static <T> MissingFeaturesFunction<T> toFunction(
            Collection<String> imports, List<JavaFeatureSpec> specs,
            CompiledSemanticsPlugin<T> plugin,
            File preCompiledDirectory) {
        final FeatureExpander<T> fe = new FeatureExpander<T>(plugin, specs, imports, preCompiledDirectory, false);

        final IndexedSeq<Tuple2<String, GenAggFunc<T, scala.collection.Iterable<Tuple2<String, Object>>>>> features = fe.features();

        // this is the function that actually returns the features that will be converted to vw input
        final Function<T, List<Iterable<Map.Entry<String, Double>>>> theRealFunctionToApply = fe.javaConverter();

        // but before returning that function we wrap it so we can detect which features in the passed in proto instance
        // are missing or erroneous in some way.
        return new MissingFeaturesFunction(features, theRealFunctionToApply);
    }

    protected final static List<JavaFeatureSpec> getFeatureSpecList(JsonSpecPojo jsonSpecPojo) throws IOException {
        List<JavaFeatureSpec> featureSpecs = new ArrayList<JavaFeatureSpec>();
        for (JsonSpecPojo.JsonFeature jsonFeature : jsonSpecPojo.features) {
            if (jsonFeature.defVal != null) {
                // convert the json read list format to the type the constructor takes
                List<Map.Entry<String, Double>> convertedDefaults = new ArrayList<Map.Entry<String, Double>>(jsonFeature.defVal.size());
                for (List def : jsonFeature.defVal)
                    convertedDefaults.add(new AbstractMap.SimpleEntry<String, Double>((String) def.get(0), (Double) def.get(1)));
                featureSpecs.add(new JavaFeatureSpec(jsonFeature.name, jsonFeature.spec, convertedDefaults));
            } else
                featureSpecs.add(new JavaFeatureSpec(jsonFeature.name, jsonFeature.spec));
        }
        return featureSpecs;
    }

    protected final static List<Map.Entry<String, List<Integer>>> getNamespaces(JsonSpecPojo jsonSpecPojo) {
        HashMap<String, Integer> featureIndexLookup = new HashMap<String, Integer>();
        for (int i = 0; i < jsonSpecPojo.features.size(); i++)
            featureIndexLookup.put(jsonSpecPojo.features.get(i).name, i);

        List<Map.Entry<String, List<Integer>>> namespaces = new ArrayList<Map.Entry<String, List<Integer>>>();
        for (JsonSpecPojo.JsonNamespace jsonNamespace : jsonSpecPojo.namespaces) {
            List<Integer> featureIndexes = new ArrayList<Integer>(jsonNamespace.features.size());
            for (String featureName : jsonNamespace.features)
                featureIndexes.add(featureIndexLookup.get(featureName));
            namespaces.add(new AbstractMap.SimpleEntry<String, List<Integer>>(jsonNamespace.name, featureIndexes));
        }
        return namespaces;
    }
}

  */

/**
 * This class is going to take
 */
object SpecBuilder {

//    def buildVwFromJson[A <: GeneratedMessage](vwJsonSpecFile: String, prototype: A): Spec[A] = {
//        // buildFromJson(vwJsonSpecFile, prototype, null, SpecType.VW, Optional.<Integer>absent())
//        ???
//    }

    def toFunction[A](semantics: Semantics[A], specs: Seq[FeatureSpec]): Try[FeatureExtractorFunction[A]] = {
        def oneSpec(s: FeatureSpec) = semantics.createFunction[Iterable[(String, Double)]](s.spec, s.defaultValue)
        def createSpecs(s: List[FeatureSpec], fns: List[(String, GenAggFunc[A, Iterable[(String, Double)]])]): Try[sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])]] = {
            s match {
                case Nil => Try { fns.reverse.toIndexedSeq }  // Done
                case h :: t => oneSpec(h) match {
                    case Left(e) =>
                        // Short-circuiting failure.
                        Failure { new RuntimeException((s"Failure creating function for feature: '${h.featureName}' with spec: ${h.spec}" +: e).mkString("\n")) }
                    case Right(f) => createSpecs(t, (h.featureName, f) :: fns) // Recurse
                }
            }
        }

        createSpecs(specs.toList, Nil).map(FeatureExtractorFunction.apply[A])
    }

/*

    // Refactor this to take in a Semantics<T> and then build up from there
    protected final static <T> MissingFeaturesFunction<T> toFunction(
            Collection<String> imports, List<JavaFeatureSpec> specs,
            CompiledSemanticsPlugin<T> plugin,
            File preCompiledDirectory) {
        final FeatureExpander<T> fe = new FeatureExpander<T>(plugin, specs, imports, preCompiledDirectory, false);

        final IndexedSeq<Tuple2<String, GenAggFunc<T, scala.collection.Iterable<Tuple2<String, Object>>>>> features = fe.features();

        // this is the function that actually returns the features that will be converted to vw input
        final Function<T, List<Iterable<Map.Entry<String, Double>>>> theRealFunctionToApply = fe.javaConverter();

        // but before returning that function we wrap it so we can detect which features in the passed in proto instance
        // are missing or erroneous in some way.
        return new MissingFeaturesFunction(features, theRealFunctionToApply);
    }

*/

    @throws[IOException]
    def buildFromJson[A](
            jsonSpec: JsonSpec,
            preCompiledDirectory: File,
            compiledSemanticsPlugin: AnyRef,
            specType: SpecType,
            numBits: Option[Int]
    ): Spec[A] = {



                /**


public static final <T> Spec<T> buildFromJson(
            JsonSpecPojo jsonSpecPojo,
            File preCompiledDirectory,
            CompiledSemanticsPlugin<T> plugin,
            SpecType specType,
            Optional<Integer> numBits) throws IOException {
        validate(jsonSpecPojo);

        SingleFunctionCreator<T> c = new SingleFunctionCreator<T>(plugin, jsonSpecPojo.imports, preCompiledDirectory);
        List<JavaFeatureSpec> featureSpecs = getFeatureSpecList(jsonSpecPojo);

        Spec<T> spec;
        if (StringUtils.isNotEmpty(jsonSpecPojo.label)) {
            if (specType == SpecType.VW) {
                return new LabelVwSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        jsonSpecPojo.normalizeFeatures,
                        c.createSingleFunction(jsonSpecPojo.label, ""),
                        StringUtils.isEmpty(jsonSpecPojo.importance) ? null : c.createSingleFunction(jsonSpecPojo.importance, ""));
            }
            else {
                return new LabelLibSVMSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        numBits.or(DEFAULT_HASH_BITS),
                        c.createSingleFunction(jsonSpecPojo.label, ""));
            }
        }
        else if (StringUtils.isNotEmpty(jsonSpecPojo.cbAction)) {
            spec = new ContextualBanditsVwSpec<T>(
                    featureSpecs,
                    getNamespaces(jsonSpecPojo),
                    toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                    jsonSpecPojo.normalizeFeatures,
                    c.createSingleFunction(jsonSpecPojo.cbAction, ""),
                    c.createSingleFunction(jsonSpecPojo.cbCost, ""),
                    c.createSingleFunction(jsonSpecPojo.cbProbability, ""));
        }
        else {
            if (specType == SpecType.VW) {
                spec = new VwSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        jsonSpecPojo.normalizeFeatures);
            }
            else {
                spec = new LibSVMSpec<T>(
                        featureSpecs,
                        getNamespaces(jsonSpecPojo),
                        toFunction(jsonSpecPojo.imports, featureSpecs, plugin, preCompiledDirectory),
                        numBits.or(DEFAULT_HASH_BITS));
            }
        }

        return spec;
    }


  */

}
