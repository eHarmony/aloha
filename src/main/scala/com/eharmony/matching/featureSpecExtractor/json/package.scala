package com.eharmony.matching.featureSpecExtractor

import spray.json.pimpString
import spray.json.DefaultJsonProtocol.jsonFormat9
import scala.collection.{immutable => im}
import com.eharmony.matching.aloha.models.reg.RegressionModelJson
import com.eharmony.matching.aloha.factory.ScalaJsonFormats.listMapFormat

/**
 * This is nice because we can reuse a lot of the work in the regression model JSON protocol.  This allows things like
 * simplified syntax for features without defaults, etc.  For instance each of the following are valid features (and
 * valid JSON):
 *
 * {{{
 * val json = """
 *              |{
 *              |  "features": {
 *              |    "f1": "",
 *              |    "f2": { "spec": "" },
 *              |    "f3": { "spec": "", "defVal": [] }
 *              |  }
 *              |}
 *            """.stripMargin.trim
 * }}}
 *
 *
 * @author R M Deak
 */
package object json extends RegressionModelJson {

    /**
     * This is somewhat of a union type containing all of the features needed by any of the types derived from Spec.
     * @param imports
     * @param features
     * @param namespaces
     * @param label
     * @param importance
     * @param cbAction
     * @param cbCost
     * @param cbProbability
     * @param normalizeFeatures
     */
    case class JsonSpec(
        imports: Seq[String] = Seq.empty,
        features: im.ListMap[String, Spec] = im.ListMap.empty,
        namespaces: im.ListMap[String, Seq[String]] = im.ListMap.empty,
        label: Option[String] = None,
        importance: Option[String] = None,
        cbAction: Option[String] = None,
        cbCost:  Option[String] = None,
        cbProbability:  Option[String] = None,
        normalizeFeatures: Option[Boolean] = None
    ) {
        /**
         * indexed sequence of FeatureSpec determined from features.
         */
        lazy val namedFeatures: im.IndexedSeq[FeatureSpec] =
            features.view.map{ case(k, v) => FeatureSpec(k, v.spec, v.defVal.getOrElse(Nil)) }.toIndexedSeq

    }

    implicit val jsonSpecFormat = jsonFormat9(JsonSpec)

    def getJsonSpec(json: String) = json.parseJson.convertTo[JsonSpec]
}
