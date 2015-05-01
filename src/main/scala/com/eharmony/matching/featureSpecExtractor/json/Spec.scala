package com.eharmony.matching.featureSpecExtractor.json

import com.eharmony.matching.featureSpecExtractor.density.{Dense, Sparse}
import spray.json.DefaultJsonProtocol

/**
 *
 * @tparam Density
 */
trait Spec[Density] {

    /**
     * The feature name.
     */
    val name: String

    /**
     * The feature specification.
     */
    val spec: String

    /**
     * A default value.
     */
    val defVal: Option[Density]
}

final case class SparseSpec(name: String, spec: String, defVal: Option[Sparse] = Option(Nil)) extends Spec[Sparse]

object SparseSpec extends DefaultJsonProtocol {
    implicit val sparseSpecFormat = jsonFormat3(SparseSpec.apply)
}

final case class DenseSpec(name: String, spec: String, defVal: Option[Dense] = None) extends Spec[Dense]

object DenseSpec extends DefaultJsonProtocol {
    implicit val denseSpecFormat = jsonFormat3(DenseSpec.apply)
}
