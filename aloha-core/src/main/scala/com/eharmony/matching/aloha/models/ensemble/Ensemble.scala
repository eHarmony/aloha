package com.eharmony.matching.aloha.models.ensemble

import scala.collection.GenTraversableOnce
import com.eharmony.matching.aloha.models.{BaseModel, Model}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.score.basic.ModelOutput


trait Ensemble[-A, B, C, +D] { self: BaseModel[A, D] =>

    // This allows us to have an implicit in the trait even though we can't explicitly specify that we need D to have
    // an implicit.  For more, see:
    // http://stackoverflow.com/questions/6983759/how-to-declare-traits-as-taking-implicit-constructor-parameters
    // http://stackoverflow.com/posts/6984823/revisions
    protected[this] case class ScoreConverterW[S >: D]()(implicit val scoreConverter: ScoreConverter[S])
    protected[this] val impl: ScoreConverterW[D]
    import impl.scoreConverter

    val subModels: GenTraversableOnce[Model[A, B]]

    val combiner: EnsembleCombiner[B, C, D]

    private lazy val (firstSubModel, _) :: subModelList = subModels.toList.zipWithIndex

    private[aloha] final def getScore(a: A)(implicit audit: Boolean): (ModelOutput[D], Option[Score]) = {
        if (audit) {
            val zs = firstSubModel.getScore(a)
            var sub = List(zs._2.get)
            val _c = subModelList.foldLeft(combiner.zero(zs._1))((s, x) => {
                val y = x._1.getScore(a)
                sub = y._2.get :: sub
                combiner.seqOp(s, (y._1, x._2))
            })
            scoreTuple(combiner.finalOp(_c), sub)
        }
        else {
            scoreTuple(combiner.finalOp(subModelList.foldLeft(combiner.zero(firstSubModel.scoreAsEither(a)))((s, x) => combiner.seqOp(s, (x._1.scoreAsEither(a), x._2)))))
        }
    }
}
