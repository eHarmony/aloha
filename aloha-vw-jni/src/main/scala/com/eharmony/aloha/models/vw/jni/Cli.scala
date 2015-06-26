package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.StringReadable
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json.VwUnlabeledJson
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.vfs2.{FileObject, VFS}
import spray.json.{pimpAny, pimpString}

import scala.collection.immutable.ListMap

/**
 * Created by rdeak on 6/15/15.
 */
@CLI(flag = "--vw")
object Cli extends VwJniModelJson {

    private[this] val CommandName = "vw"

    /**
     * '''NOTE''' null default values is only OK because both parameters are required
     * @param spec
     * @param model
     */
    case class Config(spec: FileObject = null, model: FileObject = null, id: Long = 0, name: String = "", vwArgs: String= "")

    def main(args: Array[String]) {
        cliParser.parse(args, Config()) match {
            case Some(Config(spec, model, id, name, vwArgs)) => println(getModel(spec, model, id, name, vwArgs))
            case None => // Will be taken care of by scopt.
        }
    }

    private[this] def cliParser = {
        new scopt.OptionParser[Config](CommandName) {
            head(CommandName, aloha.version)
            opt[String]('s', "spec") action { (x, c) =>
                c.copy(spec = file(x))
            } text "spec is an Apache VFS URL to an aloha spec file with modelType 'VwJNI'." required()
            opt[String]('m', "model") action { (x, c) =>
                c.copy(model = file(x))
            } text "model is an Apache VFS URL to a VW binary model." required()
            opt[String]('n', "name") action { (x, c) =>
                c.copy(name = x)
            } text "name of the model." optional()
            opt[Long]('i', "id") action { (x, c) =>
                c.copy(id = x)
            } text "numeric id of the model." optional()
            opt[String]("vw-args") action { (x, c) =>
                c.copy(vwArgs = x)
            } text "arguments to vw" optional()
        }
    }

    private[this] def file(path: String) = VFS.getManager.resolveFile(path)

    private[this] def getModel(spec: FileObject, model: FileObject, id: Long, name: String, vwArgs: String) = {
        val json = StringReadable.fromVfs2(spec).parseJson
        val b64Model = VwJniModel.readModel(model.getContent.getInputStream)
        val j = json.asJsObject("Expected JSON object.")
        val vw: VwUnlabeledJson = json.convertTo[VwUnlabeledJson]
        val features = ListMap(vw.features.map(f => f.name -> f.toModelSpec):_*)
        val ns = vw.namespaces.map(nss => ListMap(nss.map(n => n.name -> n.features):_*))
        val vwParams = Option(vwArgs).filter(_.trim.nonEmpty).map(args => Right(StringEscapeUtils.escapeJson(args)))
        val vwObj = Vw(vwParams, Option(b64Model))
        VwJNIAst(VwJniModel.parser.modelType, ModelId(id, name), features, vwObj, ns).toJson.compactPrint
    }
}
