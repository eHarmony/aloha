package com.eharmony.matching.aloha.models.vw.jni

import com.eharmony.matching.aloha
import com.eharmony.matching.aloha.annotate.CLI
import spray.json.{DeserializationException, JsString, JsObject, pimpString}
import com.eharmony.matching.aloha.io.StringReadable
import org.apache.commons.vfs2.{FileObject, VFS}

/**
 * Created by rdeak on 6/15/15.
 */
@CLI(flag = "--vw")
object Cli {

    private[this] val CommandName = "vw"

    /**
     * '''NOTE''' null default values is only OK because both parameters are required
     * @param spec
     * @param model
     */
    case class Config(spec: FileObject = null, model: FileObject = null)

    def main(args: Array[String]) {
        cliParser.parse(args, Config()) match {
            case Some(Config(spec, model)) => println(getModel(spec, model))
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
        }
    }

    private[this] def file(path: String) = VFS.getManager.resolveFile(path)

    private[this] def getModel(spec: FileObject, model: FileObject) = {
        val json = StringReadable.fromVfs2(spec).parseJson
        val b64Model = VwJniModel.readModel(model.getContent.getInputStream)

        val j = json.asJsObject("Expected JSON object.")
        val updatedJson = j.getFields("vw") collectFirst {
            case vw@JsObject(fields) if !fields.contains("model") =>
                j.copy(fields = j.fields + ("vw" -> vw.copy(fields = fields + ("model" -> JsString(b64Model)))))
            case vw@JsObject(fields) if fields.contains("model") =>
                throw new DeserializationException("JSON should not contain the path 'vw.model'.")
        }

        updatedJson.
            getOrElse { throw new DeserializationException("JSON does not contain a 'vw' object.") }.
            compactPrint
    }
}
