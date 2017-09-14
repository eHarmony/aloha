package com.eharmony.aloha.dataset.vw.cb

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.cb.json.VwContextualBanditJson
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util._
import spray.json.JsValue

import scala.util.Try

final case class VwContextualBanditRowCreator[-A](
        override val featuresFunction: FeatureExtractorFunction[A, Sparse],
        override val defaultNamespace: List[Int],
        override val namespaces: List[(String, List[Int])],
        override val normalizer: Option[CharSequence => CharSequence],
        cbAction: GenAggFunc[A, Option[Long]],
        cbCost: GenAggFunc[A, Option[Double]],
        cbProbability: GenAggFunc[A, Option[Double]],
        override val includeZeroValues: Boolean = false)
extends VwRowCreator[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
   with Logging
   with java.io.Serializable  {

    override def apply(data: A) = {
        val (missing, iv) = super.apply(data)

        val lineOpt = for {
            a <- action(data)
            c <- cost(data)
            p <- probability(data)
        } yield {
            new StringBuilder().
                append(a).append(":").
                append(VwRowCreator.LabelDecimalFormatter.format(c)).append(":").
                append(VwRowCreator.LabelDecimalFormatter.format(p)).
                append(if (0 == iv.length()) " |" else s" $iv")
        }

        if (lineOpt.isEmpty) debug("Contextual Bandit label information is missing. Creating a line with no label.")

        val line = lineOpt.getOrElse(iv)
        (missing, line)
    }

    private[this] def action(data: A): Option[Long] = cbAction(data).filter(_ > 0)
    private[this] def cost(data: A): Option[Double] = cbCost(data)
    private[this] def probability(data: A): Option[Double] = cbProbability(data).filter(p => 0 <= p && p <= 1)
}

final object VwContextualBanditRowCreator {
  final class Producer[A]
    extends RowCreatorProducer[A, CharSequence, VwContextualBanditRowCreator[A]]
       with RowCreatorProducerName
       with VwCovariateProducer[A]
       with DvProducer
       with SparseCovariateProducer
       with CompilerFailureMessages {

    type JsonType = VwContextualBanditJson

    def parse(json: JsValue): Try[VwContextualBanditJson] = Try { json.convertTo[VwContextualBanditJson] }

    def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: VwContextualBanditJson): Try[VwContextualBanditRowCreator[A]] = {
      val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

      val spec = for {
        cov <- covariates
        action <- getAction(semantics, jsonSpec.cbAction, jsonSpec.classLabels)
        cost <- getCost(semantics, jsonSpec.cbCost)
        prob <- getProbability(semantics, jsonSpec.cbProbability)
      } yield new VwContextualBanditRowCreator(cov, default, nss, normalizer, action, cost, prob)

      spec
    }

    protected[this] def getAction(
        semantics: CompiledSemantics[A],
        spec: String,
        classLabels: Option[SimpleTypeSeq]): Try[GenAggFunc[A, Option[Long]]] =
      classLabels map {
        l => getMappedAction(semantics, l.values, spec)(l.refInfo)
      } getOrElse {
        getDv[A, Option[Long]](semantics, "cbAction", Some(s"Option($spec)"), Some(None))
      }

    protected[this] def getMappedAction[B: RefInfo](semantics: CompiledSemantics[A], vs: Vector[B], spec: String) = {
      val a = getDv[A, Option[B]](semantics, "cbAction", Some(s"Option($spec)"), Some(None))
      val classIdMap = vs.zip(1 to vs.size).map(x => (x._1, x._2.toLong)).toMap
      a.map(f => f.andThenGenAggFunc(_.flatMap(classIdMap.get)))
    }

    protected[this] def getCost(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
      getDv[A, Option[Double]](semantics, "cbCost", Some(s"Option($spec)"), Some(None))

    protected[this] def getProbability(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
      getDv[A, Option[Double]](semantics, "cbProbability", Some(s"Option($spec)"), Some(None))
  }
}
