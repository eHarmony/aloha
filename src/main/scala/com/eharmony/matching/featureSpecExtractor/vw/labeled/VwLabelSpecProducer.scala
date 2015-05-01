package com.eharmony.matching.featureSpecExtractor.vw.labeled
//import com.eharmony.matching.featureSpecExtractor.json.JsonSpec

//class VwLabelSpecProducer extends VwSpecProducer with DvProducer {
//    override def specProducerName = getClass.getSimpleName
//    override def appliesTo(jsonSpec: JsonSpec): Boolean = jsonSpec.specType.exists(_ == Vw.identifier) && jsonSpec.label.isDefined
//    override def getSpec[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec): Try[Spec[A]] = {
//        val (covariates, default, nss, normalizer) = getInputs(semantics, jsonSpec)
//
//        val spec = for {
//            cov <- covariates
//            sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
//            lab <- getLabel(sem, jsonSpec.label)
//            importance = getImportance(semantics, jsonSpec.importance)
//        } yield new VwLabelSpec(cov, default, nss, normalizer, lab, importance)
//
//        spec
//    }
//
//    protected[this] def getLabel[A](semantics: CompiledSemantics[A], spec: Option[String]): Try[GenAggFunc[A, String]] =
//        getDv(semantics, "label", spec, Some(""))
//
//    protected[this] def getImportance[A](semantics: CompiledSemantics[A], spec: Option[String]): Option[GenAggFunc[A, String]] = {
//        getDv(semantics, "importance", spec, Some("")).map(Option.apply).getOrElse(None)
//    }
//}
