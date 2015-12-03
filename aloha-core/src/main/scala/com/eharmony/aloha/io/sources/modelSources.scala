package com.eharmony.aloha.io.sources

import com.eharmony.aloha.io.vfs.{File, Vfs, VfsType}
import org.apache.commons.codec.binary.Base64
import spray.json._

sealed trait ModelSource {
  def localVfs: File
  def shouldDelete: Boolean
}

object ModelSource {

  /**
   * Allows for the following forms.
   * {{{
   * { "model":    "[Base 64 encoded byte array here]" }
   * { "modelUrl": "some://apache.vfs2/url/here" }
   * { "modelUrl": "some://apache.vfs1/url/here", "via": "vfs1" }
   * { "modelUrl": "some://apache.vfs2/url/here", "via": "vfs2" }
   * { "modelUrl": "/absolute/path/here",         "via": "file" }
   * }}}
   */
  implicit object jsonFormat extends RootJsonFormat[ModelSource] with DefaultJsonProtocol {
    override def read(json: JsValue) = {
      val jso = json.asJsObject("Model Source expected to be object")

      val modelVal = jso.getFields("model") match {
        case Seq(JsString(m)) => Some(m)
        case _                => None
      }

      val modelUrlVal = jso.getFields("modelUrl") match {
        case Seq(JsString(m)) => Some(m)
        case _                => None
      }

      val vfsType = jso.getFields("via") match {
        case Seq(via) => Some(jso.convertTo(VfsType.JsonReader("via")))
        case _        => None
      }

      val params = jso.getFields("params") match {
        case Seq(p) => Option(p.convertTo[Either[Seq[String], String]])
        case Nil    => None
      }

      val paramStr = params.map(_.fold(_.mkString(" "), identity)) getOrElse ""

      // Default to VFS 2 for external source.
      val modelSource = (modelVal, modelUrlVal, vfsType) match {
        case (None, Some(u), t)                  => ExternalSource(Vfs.fromVfsType(t getOrElse VfsType.vfs2)(u))

        // TODO: Determine if we should error when given an incorrect type, or just ignore.
        case (Some(m), None, _)                  => Base64StringSource(m)
//      case (Some(m), None, Some(VfsType.file)) => Base64StringSource(m)
//      case (Some(m), None, None)               => Base64StringSource(m)
//      case (Some(m), None, Some(t))            => throw new DeserializationException(s"Incorrect type '$t' supplied.  Should be 'file' for base64-encoded model. Given ${json.compactPrint}")

        case (Some(m), Some(u), _)               => throw new DeserializationException("Exactly one of 'model' and 'modelUrl' should be supplied. Both supplied: " + json.compactPrint)
        case (None, None, _)                     => throw new DeserializationException("Exactly one of 'model' and 'modelUrl' should be supplied. Neither supplied: " + json.compactPrint)
      }

      modelSource
    }

    /**
      * If the model source is a base64-encoded string, then it is copied to a local for use. Therefore,
      * the VFS type can be ''file''.  This is OK because it selects neither VFS 1 nor VFS 2 so it
      * shouldn't have an effect on when someone is already using VFS 2 ''OR'' VFS 1.
      * @param modelSource a model source.
      * @return
      */
    def modelFields(modelSource: ModelSource) = {
      val ms = modelSource match {
        case Base64StringSource(b64) => Seq("model" -> JsString(b64))
        case ExternalSource(fs)      => Seq("modelUrl" -> JsString(fs.descriptor),
                                            "via"      -> JsString(fs.vfsType.toString))
      }
      scala.collection.immutable.ListMap(ms:_*)
    }

    override def write(modelSource: ModelSource) = JsObject(modelFields(modelSource))
  }
}

final case class Base64StringSource(b64EncodedData: String) extends ModelSource {

  /**
   * Get a Vfs instance pointing to a local file containing the base64-decoded b64EncodedData.
   * @return
   */
  def localVfs = {
    val f = java.io.File.createTempFile("aloha-", ".model.binary")
    f.deleteOnExit()
    val file = File(f)
    file.fromByteArray(Base64.decodeBase64(b64EncodedData))
    file
  }

  def shouldDelete = true
}

final case class ExternalSource(vfs: Vfs) extends ModelSource {
  def shouldDelete = !vfs.isLocal
  def localVfs = vfs.replicatedToLocal()
}
