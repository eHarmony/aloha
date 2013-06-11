package com.eharmony.matching.aloha.models.ensemble.maxima

import scala.collection.GenTraversableOnce

import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.score.basic.ModelOutput
import ModelOutput.Implicits.modelOutputOrdering
import com.eharmony.matching.aloha.models.ensemble.tie.TieBreaker
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.models.ensemble.{Ensemble, EnsembleCombiner}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter

case class ArgMin[-A, B, +C](
        subModels: GenTraversableOnce[Model[A, B]],
        labels: IndexedSeq[C],
        tieBreaker: TieBreaker[B],
        modelId: ModelIdentity
)(implicit o: Ordering[B], c: ScoreConverter[C]) extends Ensemble[A, B, MaximaList[B], C] with Model[A, C] {
    require(subModels.size == labels.size && subModels.size > 0)
    val combiner = EnsembleCombiner(Zero.zero[B], new Minima[B], new ArgMaximaSelector(tieBreaker, labels))
    protected[this] val impl = ScoreConverterW[C]
}
