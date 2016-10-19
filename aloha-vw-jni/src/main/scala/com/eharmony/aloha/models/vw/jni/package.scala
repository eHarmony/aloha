package com.eharmony.aloha.models.vw

import java.util
import java.lang.Float

import vowpalWabbit.responses.ActionProbs

/**
  * Created by sagoyal on 10/12/16.
  */
package object jni {

  implicit class ActionProbsExtensions(val actionProbs: ActionProbs) extends AnyVal {

    def probsAsList: util.ArrayList[Float] = {
      val weights = new util.ArrayList[Float](actionProbs.getActionProbs.length)
      actionProbs.getActionProbs.foreach(ap => weights.add(new Float(ap.getProbability)))
      weights
    }

  }

}
