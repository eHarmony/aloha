package com.eharmony.aloha.dataset.vw.labeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.labeled.json.VwLabeledJson
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc0}
import com.eharmony.aloha.util.Logging
import spray.json.JsValue

import scala.util.Try


final case class VwLabelRowCreator[-A](
        override val featuresFunction: FeatureExtractorFunction[A, Sparse],
        override val defaultNamespace: List[Int],
        override val namespaces: List[(String, List[Int])],
        override val normalizer: Option[CharSequence => CharSequence],
        label: GenAggFunc[A, Option[Double]],
        importance: GenAggFunc[A, Option[Double]],
        tag: GenAggFunc[A, Option[String]],
        override val includeZeroValues: Boolean = false)
extends VwRowCreator[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
   with LabelRowCreator[A, CharSequence]
   with Logging {
    override def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val (missing, iv) = super.apply(data)

        // If importance or label is missing, this will return None.
        // Otherwise, It'll be the entire line with dep vars and indep vars.
        val lineOpt = for {
            imp <- importance(data)
            lab <- label(data)
            t = tag(data).getOrElse("").trim
        } yield {
            val sb = new StringBuilder().
                append(VwRowCreator.LabelDecimalFormatter.format(lab)).  // VW input format [Label].
                append(" ")
            (if (imp == 1) sb
             else sb.
                append(VwRowCreator.LabelDecimalFormatter.format(imp)).  // VW input format [Importance].
                append(" ")
            ).append(t).     // VW input format [Tag].
              append(if (0 == iv.length()) "|" else iv)
        }

        if (lineOpt.isEmpty) debug("Label information is missing. Creating a line with no label.")

        // If label information is missing, just return the indep vars.  This will cause vw
        // to ignore the line for training.
        val line = lineOpt.getOrElse(iv)
        (missing, line)
    }

    override val stringLabel: GenAggFunc[A, Option[String]] =
        label.andThenGenAggFunc(labOpt => labOpt.map(lab => VwRowCreator.LabelDecimalFormatter.format(lab)))
}

final object VwLabelRowCreator {
    final class Producer[A]
        extends RowCreatorProducer[A, CharSequence, VwLabelRowCreator[A]]
        with RowCreatorProducerName
        with VwCovariateProducer[A]
        with DvProducer
        with SparseCovariateProducer
        with CompilerFailureMessages {

        type JsonType = VwLabeledJson

        def parse(json: JsValue): Try[VwLabeledJson] = Try { json.convertTo[VwLabeledJson] }

        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: VwLabeledJson): Try[VwLabelRowCreator[A]] = {
            val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

            val importance = getImportance(semantics, jsonSpec.importance)

            // Notice that we create a new semantics for the tag because it has a string output type.  This new
            // semantics implicitly coerces values of any type to string by calling toString since all objects
            // have a toString method.  This allows more sloppy specifications but makes them work.
            val spec = for {
                cov <- covariates
                lab <- getLabel(semantics, jsonSpec.label)
                sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
                tag <- getTag(sem, jsonSpec.tag, lab)
            } yield new VwLabelRowCreator(cov, default, nss, normalizer, lab, importance, tag)

            spec
        }

        protected[this] def getLabel(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
            getDv[A, Option[Double]](semantics, "label", Some(s"Option($spec)"), Some(None))

        protected[this] def getImportance(semantics: CompiledSemantics[A], spec: Option[String]): GenAggFunc[A, Option[Double]] =
            getDv[A, Option[Double]](semantics, "importance", spec.map(s => s"Option($s)"), Some(None)) getOrElse {
                GenFunc0[A, Option[Double]]("1", _ => Some(1d))
            }

        protected[this] def getTag(semantics: CompiledSemantics[A], spec: Option[String], label: GenAggFunc[A, Option[Double]]): Try[GenAggFunc[A, Option[String]]] = {
            spec.fold(Try{label.andThenGenAggFunc(_.map(v => VwRowCreator.LabelDecimalFormatter.format(v)))})(sp =>
                getDv[A, Option[String]](semantics, "tag", Some(s"Option($sp)"), Some(None))
            )
        }
    }

}
