package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.id.ModelId
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
    case class Config(spec: FileObject = null, model: FileObject = null, id: Long = 0, name: String = "", vwArgs: Option[String]= None)

    def main(args: Array[String]) {
        cliParser.parse(args, Config()) match {
            case Some(Config(spec, model, id, name, vwArgs)) => println(VwJniModelCreator.buildModel(spec, model, ModelId(id, name), vwArgs, None, None).compactPrint)
            case None => // Will be taken care of by scopt.
        }
    }

    private[this] def cliParser = {
        new scopt.OptionParser[Config](CommandName) {
            head(CommandName, aloha.version)
            opt[String]('s', "spec") action { (x, c) =>
                c.copy(spec = file(x))
            } text "spec is an Apache VFS URL to an aloha spec file." required()
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
                c.copy(vwArgs = Some(x))
            } text "arguments to vw" optional()
        }
    }

    private[this] def file(path: String) = VFS.getManager.resolveFile(path)
}
