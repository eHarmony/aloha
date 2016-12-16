package com.eharmony.aloha.score.audit.take2


import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.score.audit.Semantics

/**
  * Created by ryan on 12/15/16.
  */
case class Factory() {
  /**
    *
    * @param sem
    * @param tc
    * @param aud
    * @param constant
    * @tparam A RefInfo should be in sem
    * @tparam T
    * @tparam N RefInfo should be in tc
    * @tparam B
    * @return
    */
  def createConstantModel[A, T <: TypeCtor, N, B <: T#TC[N]: RefInfo](
      sem: Semantics[A],
      tc: T,
      aud: TypedAuditor[ModelIdentity, T, N, B],
      constant: N):Either[String, Model[A, B]] = {

    implicit val ria = sem.refInfo
    implicit val rin = aud.refInfo
    implicit val ritcn = tc.refInfo[N]
    val mritc = RefInfo[Model[A, tc.TC[N]]]
    val rib = RefInfo[Model[A, B]]

    if (!RefInfoOps.isSubType(mritc, rib))
      Left(s"model doesn't have correct output type. Desired: $rib, found: $mritc")
    else {
      val model: ConstantModel[T, Any, N] = ConstantModel(ModelId(), constant, tc, aud)
      val typedModel: Option[Model[A, B]] = rib.unapply(model)
      typedModel.toRight(s"model is not a RefInfo[Model[A, B]]")
    }
  }
}
