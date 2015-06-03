package com.eharmony.matching.aloha.models.vw.jni

import spray.json.RootJsonFormat

import scala.collection.immutable.ListMap
import spray.json.DefaultJsonProtocol._

import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.models.reg.json.SpecJson
import com.eharmony.matching.aloha.factory.Formats.listMapFormat

/**
 * Components of the JSON protocol for VwJniModel
 */
trait VwJniModelJson extends SpecJson {

    /**
     *
     * Note that as is, this declaration will cause a compiler warning:
     *
     *     "The outer reference in this type test cannot be checked at run time."
     *
     * This is a known issue and is a scala bug.  See:
     * - https://issues.scala-lang.org/browse/SI-4440
     * - http://stackoverflow.com/questions/16450008/typesafe-swing-events-the-outer-reference-in-this-type-test-cannot-be-checked-a
     *
     * A solution that would remove the warning is to make the class not ''final''.  Not doing this just to remove a
     * warning.
     * @param params VW initialization parameters.  This is either a sequence of parameters that will be made into a
     *               single string by imploding the list with a " " separator or it is one string.  If None,
     */
    protected[this] final case class Vw(params: Option[Either[Seq[String], String]] = Some(Right(""))) {
        def getParams = params getOrElse Right("")
    }

    /**
     * Note that as is, this declaration will cause a compiler warning:
     *
     *     "The outer reference in this type test cannot be checked at run time."
     *
     * This is a known issue and is a scala bug.  See:
     * - https://issues.scala-lang.org/browse/SI-4440
     * - http://stackoverflow.com/questions/16450008/typesafe-swing-events-the-outer-reference-in-this-type-test-cannot-be-checked-a
     *
     * A solution that would remove the warning is to make the class not ''final''.  Not doing this just to remove a
     * warning.
     * @param modelType The model type (Should be VwJNI).
     * @param modelId a model ID
     * @param features an map of features (whose iteration order is the declaration order).
     * @param vw an object for configuring the VwScorer object that will be embedded in the VwJniModel.
     * @param namespaces an map of namespace name to sequence of feature names in the namespace.
     * @param numMissingThreshold A threshold dictating how many missing features to allow before making
     *                            the prediction fail.  None means the threshold is &infin;.  If, when mapping
     *                            feature functions over the input, the resulting sequence contains more than
     *                            ''numMissingThreshold'' values that are empty Iterable values, then the
     *                            ''Features.missingOk'' value returned by ''constructFeatures'' will be
     *                            '''false'''; otherwise, it will be '''true'''.
     */
    protected[this] final case class VwJNIAst(
        modelType: String,
        modelId: ModelId,
        features: ListMap[String, Spec],
        vw: Vw,
        namespaces: Option[ListMap[String, Seq[String]]] = Some(ListMap.empty),
        numMissingThreshold: Option[Int] = None)

    protected[this] final implicit val vwFormat: RootJsonFormat[Vw] = jsonFormat1(Vw.apply)
    protected[this] final implicit val vwJNIAstFormat: RootJsonFormat[VwJNIAst] = jsonFormat6(VwJNIAst.apply)
}
