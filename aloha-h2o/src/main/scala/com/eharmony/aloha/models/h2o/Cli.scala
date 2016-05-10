package com.eharmony.aloha.models.h2o

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.vfs.VfsType.VfsType
import com.eharmony.aloha.io.vfs.{Vfs, VfsType}

/**
  * Created by rdeak on 11/17/15.
  */
@CLI(flag = "--h2o")
object Cli {

  private[this] val CommandName = "h2o"

  /**
    * '''NOTE''' null default values is only OK because both parameters are required
    * @param spec
    * @param model
    * @param id
    * @param name
    * @param externalModel
    * @param numMissingThreshold
    * @param notes
    * @param vfsType
    */
  case class Config(spec: String = null,
                    model: String = null,
                    id: Long = 0,
                    name: String = "",
                    externalModel: Boolean = false,
                    numMissingThreshold: Option[Int] = None,
                    notes: Vector[String] = Vector.empty,
                    vfsType: VfsType = VfsType.vfs2)

  def main(args: Array[String]) {
    cliParser.parse(args, Config()) match {
      case Some(Config(spec, model, id, name, externalModel, numMissingThresh, notes, vfsType)) =>

        val specVfs = Vfs.fromVfsType(vfsType)(spec)
        val modelVfs = Vfs.fromVfsType(vfsType)(model)
        val json = H2oModel.json(specVfs, modelVfs, ModelId(id, name), externalModel, numMissingThresh, Option(notes))
        println(json.compactPrint)
      case None => // Will be taken care of by scopt.
    }
  }

  private[this] def cliParser = {
    new scopt.OptionParser[Config](CommandName) {
      head(CommandName, aloha.version)
      opt[String]('s', "spec") action { (x, c) =>
        c.copy(spec = x)
      } text "spec is an Apache VFS URL to an aloha spec file." required()
      opt[String]('m', "model") action { (x, c) =>
        c.copy(model = x)
      } text "model is an Apache VFS URL to a VW binary model." required()
      opt[String]("fs-type") action { (x, c) =>
        c.copy(vfsType = VfsType.withName(x))
      } text "file system type: vfs1, vfs2, file. default = vfs2." optional()
      opt[String]('n', "name") action { (x, c) =>
        c.copy(name = x)
      } text "name of the model." optional()
      opt[Long]('i', "id") action { (x, c) =>
        c.copy(id = x)
      } text "numeric id of the model." optional()
      opt[Unit]("external") action { (x, c) =>
        c.copy(externalModel = true)
      } text "link to a binary VW model rather than embedding it inline in the aloha model." optional()
      opt[Int]("num-missing-thresh") action { (x, c) =>
        c.copy(numMissingThreshold = Option(x))
      } text "number of missing features to allow before returning a 'no-prediction'." optional()
      opt[String]("note") action { (x, c) =>
        c.copy(notes = c.notes :+ x)
      } text "notes to add to the model. Can provide this many parameter times." unbounded() optional()
      checkConfig { c =>
        success
      }
    }
  }
}
