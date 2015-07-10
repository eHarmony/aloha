package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.models.reg.ConstantDeltaSpline

import scala.collection.immutable.ListMap

// import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.vfs2.FileObject
import spray.json.{JsValue, pimpAny, pimpString}

/**
 * Created by jmorra on 7/10/15.
 */
object VwJniModelCreator extends VwJniModelJson {
  def buildModel(spec: FileObject, model: FileObject, id: ModelId, vwArgs: Option[String], notes: Option[Seq[String]], spline: Option[ConstantDeltaSpline]): JsValue = {
    val json = StringReadable.fromVfs2(spec).parseJson
    val vw = json.convertTo[VwUnlabeledJson]
    buildModel(vw, model, id, vwArgs, notes, spline)
  }

  def buildModel(vw: VwUnlabeledJson, model: FileObject, id: ModelId, vwArgs: Option[String], notes: Option[Seq[String]], spline: Option[ConstantDeltaSpline]): JsValue = {
    val b64Model = VwJniModel.readModel(model.getContent.getInputStream)
    val features = ListMap(vw.features.map(f => f.name -> f.toModelSpec):_*)
    val ns = vw.namespaces.map(nss => ListMap(nss.map(n => n.name -> n.features):_*))
    // TODO: If this doesn't work, use commons-lang3 StringEscapeUtils.unescapeJava for unescaping.
    //       Removed commons-lang3 as a dependency because it's only used in 2 places.  Here and
    //       aloha-core CsvModelRunner class.  Here's how it was originally.
    //
    //         val vwParams = Option(vwArgs).filter(_.trim.nonEmpty).map(args => Right(StringEscapeUtils.escapeJson(args)))
    val vwParams = vwArgs.filter(_.trim.nonEmpty).map(args => Right(escape(args)))
    val vwObj = Vw(vwParams, Option(b64Model))
    VwJNIAst(VwJniModel.parser.modelType, id, features, vwObj, ns).toJson
  }

  private[this] def escape(s: String) = s.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")
}
