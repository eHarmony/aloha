package com.eharmony.aloha.models.vw

import java.util
import java.{lang => jl}

import vowpalWabbit.responses.ActionProbs

/**
  * Created by sahil-goyal on 10/12/16.
  */
package object jni {

  implicit class ActionProbsExtensions(val actionProbs: ActionProbs) extends AnyVal {

    def probsAsList: util.ArrayList[jl.Float] = {
      val weights = new util.ArrayList[jl.Float](actionProbs.getActionProbs.length)
      actionProbs.getActionProbs.foreach(ap => weights.add(new jl.Float(ap.getProbability)))
      weights
    }

  }

}
