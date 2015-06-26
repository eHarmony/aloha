package com.eharmony.matching.featureSpecExtractor.json

import com.eharmony.matching.featureSpecExtractor.density.{Dense, Sparse}
import spray.json.DefaultJsonProtocol

/**
 *
 * @tparam Density
 */
trait Spec[+Density] {

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


final case class SparseSpec(name: String, spec: String, defVal: Option[Sparse] = SparseSpec.defVal) extends Spec[Sparse]

object SparseSpec extends DefaultJsonProtocol {
    val defVal: Option[Sparse] = Option(Nil)
    implicit val sparseSpecFormat = jsonFormat3(SparseSpec.apply)

    implicit class SparseSpecOps(val spec: Spec[Sparse]) extends AnyVal {
        def toModelSpec = com.eharmony.aloha.models.reg.json.Spec(spec.spec, spec.defVal.map(_.toSeq))
    }
//    val modelSpecWriter = new RootJsonWriter[SparseSpec] {
//        override def write(ss: SparseSpec) = {
//            import spray.json.pimpAny
//            ss.defVal.map(d => JsObject("spec" -> ss.name.toJson, "defVal" -> d.toJson)) getOrElse ss.name.toJson
//        }
//    }
}

final case class DenseSpec(name: String, spec: String, defVal: Option[Dense] = None) extends Spec[Dense]

object DenseSpec extends DefaultJsonProtocol {
    implicit val denseSpecFormat = jsonFormat3(DenseSpec.apply)
}
