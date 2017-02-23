//package com.eharmony.aloha.models.ensemble.maxima
//
//import scala.collection.GenTraversableOnce
//import com.eharmony.aloha.score.basic.ModelOutput
//import ModelOutput.Implicits.modelOutputOrdering
//import com.eharmony.aloha.models.ensemble.tie.TieBreaker
//import com.eharmony.aloha.id.ModelIdentity
//import com.eharmony.aloha.models.ensemble.{Ensemble, EnsembleCombiner}
//import com.eharmony.aloha.score.proto.conversions.ScoreConverter
//
//case class ArgMax[-A, B, +C](
//        subModels: GenTraversableOnce[Model[A, B]],
//        labels: IndexedSeq[C],
//        tieBreaker: TieBreaker[B],
//        modelId: ModelIdentity
//)(implicit o: Ordering[B], c: ScoreConverter[C]) extends Ensemble[A, B, MaximaList[B], C] with BaseModel[A, C] {
//    require(subModels.size == labels.size && subModels.size > 0)
//    val combiner = EnsembleCombiner(Zero.zero[B], new Maxima[B], new ArgMaximaSelector(tieBreaker, labels))
//    protected[this] val impl = ScoreConverterW[C]
//}
//
