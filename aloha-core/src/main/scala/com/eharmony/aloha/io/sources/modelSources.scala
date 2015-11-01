package com.eharmony.aloha.io.sources

import java.net.URLEncoder
import java.security.MessageDigest

import com.eharmony.aloha.io.vfs.VfsType.VfsType
import com.eharmony.aloha.io.vfs.{File, Vfs, VfsType}
import org.apache.commons.codec.binary.Base64
import spray.json._

import scala.util.Try


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

      // Default to VFS 2.
      val vfsType = jso.getFields("via") match {
        case Seq(via) => jso.convertTo(VfsType.JsonReader("via"))
        case _        => VfsType.vfs2
      }

      val params = jso.getFields("params") match {
        case Seq(p) => Option(p.convertTo[Either[Seq[String], String]])
        case Nil    => None
      }

      val paramStr = params.map(_.fold(_.mkString(" "), identity)) getOrElse ""

      val modelSource = (modelVal, modelUrlVal, vfsType) match {
        case (None, Some(u), t)    => ExternalSource(Vfs.fromVfsType(t)(u))
        case (Some(m), None, t)    => Base64StringSource(m, t)
        case (Some(m), Some(u), _) => throw new DeserializationException("Exactly one of 'model' and 'modelUrl' should be supplied. Both supplied: " + json.compactPrint)
        case (None, None, _)       => throw new DeserializationException("Exactly one of 'model' and 'modelUrl' should be supplied. Neither supplied: " + json.compactPrint)
      }

      modelSource
    }

    def modelFields(modelSource: ModelSource) = {
      val ms = modelSource match {
        case Base64StringSource(b64, t) => Seq("model" -> JsString(b64))
        case ExternalSource(fs)         => Seq("modelUrl" -> JsString(fs.descriptor), "via" -> JsString(fs.vfsType.toString))
      }
      scala.collection.immutable.ListMap(ms:_*)
    }

    override def write(modelSource: ModelSource) = JsObject(modelFields(modelSource))
  }
}

final case class Base64StringSource(b64EncodedData: String, vfsType: VfsType = VfsType.vfs2) extends ModelSource {

  /**
   * Get a Vfs instance pointing to a local file containing the base64-decoded b64EncodedData.
   * @return
   */
  def localVfs = {
    val localVfs = for {
      tmpName <- getTmpFileName
      v = getTmpVfs(tmpName)
      _ <- copyToTmpFile(v)
      local <- Try { v.replicatedToLocal() }
    } yield local

    localVfs.get // Will throw here if there was an issue.
  }

  def shouldDelete = true

  private[this] def copyToTmpFile(v: Vfs) = Try { v.fromByteArray(Base64.decodeBase64(b64EncodedData)) }

  private[this] def getTmpVfs(tmpName: String) = Vfs.fromVfsType(vfsType)(tmpName)

  /**
   * This is based on a URL-encoded URL of the base64 encoding of a hash of the data.  The reason this is
   * necessary is that we want a short, safe URL description of the content.
   *
   * NOTE: It's possible that hash collisions could occur.  This must be dealt with by the calling code.
   * @return
   */
  private[this] def getTmpFileName = for {
    sha1    <- Try { MessageDigest.getInstance("SHA-1") }
    digest  <- Try { sha1.digest(b64EncodedData.getBytes) }
    b64     <- Try { new String(Base64.encodeBase64(digest)) }
    encoded <- Try { URLEncoder.encode(b64, "UTF-8") }
  } yield "tmp://" + encoded
}


final case class ExternalSource(vfs: Vfs) extends ModelSource {
  def shouldDelete = !vfs.isLocal
  def localVfs = vfs.replicatedToLocal()
}
