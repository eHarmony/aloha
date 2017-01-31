package com.eharmony.aloha.models

import com.eharmony.aloha.id.ModelIdentity

object TransformedModelOps {

  /** Produces a new model that has the desired domain.  This method came about because a situation arose where the
    * data passed to the model was not in the most computationally efficient format.  This method creates a stop-gap
    * solution in the case that it's not worth rewriting an entire semantics implementation.
    *
    * '''NOTE''': This function requires that both ''model'' and ''f'' are non-null and will throw if they are.
    *
    * @param model a model whose domain is not
    * @param f a function that maps the domain desired in the new model to the domain in the current model.
    * @tparam A input type of the model
    * @tparam B input type of the model we want to create
    * @tparam C output type of the model
    * @return a new model with the new desired domain that creates the domain object using ''f'' and delegates to
    *         ''model''.
    */
  def mapDomain[A, B, C](model: Model[A, C], f: B => A): Model[B, C] = new Model[B, C] {
    require(null != model, "model cannot be null")
    require(null != f, "f cannot be null")
    def modelId: ModelIdentity = model.modelId
    override def toString(): String = model.toString()
    override def apply(b: B): C = model(f(b))
    override def close(): Unit = model.close()
  }
}
