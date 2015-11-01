package com.eharmony.aloha.models.reg

import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.EitherHelpers

/**
 * Created by deak on 11/1/15.
 */
trait RegFeatureCompiler { self: EitherHelpers =>

  /**
   * Translate the feature specification into features.  This is done in a short circuiting way so that it
   * stops when the any feature cannot be produced.
   *
   * @param featureMap a map of feature name to feature specification
   * @param semantics a semantics with which feature specifications should be interpretted.
   * @tparam A model input type
   * @return a mapping from feature name to feature function.  Note that the indices matter and that's why we
   *         don't want to use a map.
   */
  protected[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]): ENS[Seq[(String, GenAggFunc[A, Iterable[(String, Double)]])]] =
    mapSeq(featureMap) {
      case (k, Spec(spec, default)) =>
        semantics.createFunction[Iterable[(String, Double)]](spec, default).
          left.map { Seq(s"Error processing spec '$spec'") ++ _ }. // Add the spec that errored.
          right.map { f => (k, f) }
    }
}
