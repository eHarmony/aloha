package com.eharmony.aloha.io.vfs

import java.io._

import org.apache.commons.io.IOUtils
import org.apache.commons.{vfs => vfs1, vfs2}
import spray.json._

/**
 * This provides a standard way to deal with different file systems.  Specifically, this is package should help
 * dealing with using Apache VFS 1 and Apache VFS 2.  The need for this stems from the fact that the different
 * versions are not binary compatible and they offer different plugins.  Therefore, we want build an abstraction
 * layer on top that allows us to use whichever one we want.
 *
 * @author R M Deak
 */

object VfsType extends Enumeration {
    type VfsType = Value
    val file, vfs1, vfs2 = Value

    case class JsonReader(viaField: String) extends RootJsonReader[VfsType] {
        override def read(json: JsValue) = {
            json.asJsObject("IoType expects an object").getFields(viaField) match {
                case Seq(JsString(via)) => VfsType.withName(via)
                case _ => throw new DeserializationException(s"VfsType expects the field '$viaField' (string) to be present.")
            }
        }
    }
}

sealed trait Vfs { self =>
    def vfsType: VfsType.VfsType
    def descriptor: String
    def inputStream: InputStream
    def outputStream(append: Boolean = false): OutputStream

    /**
     * If the content DOES NOT exists on the local disk, copy to local disk and return the file descriptor
     * wrapped in a Vfs.  If the content exists locally, do not copy but
     * @return
     */
    def replicatedToLocal(): File

    def isLocal: Boolean

    def delete(): Unit
    def exists(): Boolean

    def fromByteArray(a: Array[Byte]): Unit = writeToOutStr(IOUtils.write(a, _))

    protected[vfs] def writeToOutStr(f: OutputStream => Unit): Unit = {
        val os = outputStream()
        try f(os) finally IOUtils.closeQuietly(os)
    }

    def asString(): String = fromInputStream(IOUtils.toString)

    def asByteArray(): Array[Byte] = fromInputStream(IOUtils.toByteArray)

    /**
     * Get the file content in the form of a String after applying a transformation to the
     * original data.  This is especially useful when the data is base64 encoded or gzipped
     * or something.  For instance, if data was base64 encoded, then gzipped, we can do the
     * following:
     * {{{
     * import java.util.zip.GZIPInputStream
     * import org.apache.commons.codec.binary.Base64InputStream
     *
     * val content = getString(is => new Base64InputStream(new GZIPInputStream(is)))
     * }}}
     *
     * @param decoder Provides a means of decode the file contents.
     * @return
     */
    def asString(decoder: InputStream => InputStream): String =
        fromInputStream(is => IOUtils.toString(decoder(is)))

    def asByteArray(decoder: InputStream => InputStream): Array[Byte] =
        fromInputStream(is => IOUtils.toByteArray(decoder(is)))

    protected[vfs] def fromInputStream[A](f: InputStream => A) = {
        val is = inputStream
        try f(is) finally IOUtils.closeQuietly(is)
    }
}

object Vfs {
    import com.eharmony.aloha.io.vfs.VfsType.VfsType

    def fromVfsType(tpe: VfsType): String => Vfs = tpe match {
        case VfsType.vfs1 => (d: String) => Vfs1(vfs1.VFS.getManager.resolveFile(d))
        case VfsType.vfs2 => (d: String) => Vfs2(vfs2.VFS.getManager.resolveFile(d))
        case VfsType.file => (d: String) => File(new java.io.File(d))
    }

    def localSources = Set("file", "tmp")

    case class JsonReader(viaField: String, descriptorField: String) extends RootJsonReader[Vfs] {
        override def read(json: JsValue): Vfs = {
            json.asJsObject("IoType expects an object").getFields(viaField, descriptorField) match {
                case Seq(via, JsString(descriptor)) => fromVfsType(via.convertTo(VfsType.JsonReader(viaField)))(descriptorField)
                case _ => throw new DeserializationException(s"IoInstance expects the fields '$viaField' and $descriptorField (string) to be present.")
            }
        }
    }
}

case class File private[File](descriptor: String) extends Vfs {
    def vfsType = VfsType.file
    def fileObj = new java.io.File(descriptor)
    def inputStream = new FileInputStream(fileObj)
    def isLocal: Boolean = true
    def outputStream(append: Boolean = false): OutputStream = new FileOutputStream(fileObj, append)
    def replicatedToLocal() = File(new java.io.File(descriptor))
    def delete(): Unit = fileObj.delete()
    def exists(): Boolean = fileObj.exists()
}

object File {
    def apply(file: java.io.File) = {
        require(file.exists(), s"File($file) not accepted because $file doesn't exist.")
        new File(file.getCanonicalPath)
    }
}

case class Vfs1 private[Vfs1](descriptor: String) extends Vfs {
    def vfsType = VfsType.vfs1
    def fileObj = vfs1.VFS.getManager.resolveFile(descriptor)
    def inputStream = fileObj.getContent.getInputStream
    def isLocal: Boolean = Vfs.localSources contains fileObj.getName.getScheme.toLowerCase
    def outputStream(append: Boolean = false): OutputStream = fileObj.getContent.getOutputStream(append)

    def replicatedToLocal() = {
        val fo = fileObj
        File(fo.getFileSystem.replicateFile(fo, vfs1.Selectors.SELECT_SELF))
    }

    def delete(): Unit = fileObj.delete()
    def exists(): Boolean = fileObj.exists()
}

object Vfs1 {
    def apply(fo: vfs1.FileObject): Vfs1 = new Vfs1(fo.getName.toString)
}

case class Vfs2 private[Vfs2](descriptor: String) extends Vfs {
    def vfsType = VfsType.vfs2
    def fileObj = vfs2.VFS.getManager.resolveFile(descriptor)
    def inputStream = fileObj.getContent.getInputStream
    def isLocal: Boolean = Vfs.localSources contains fileObj.getName.getScheme.toLowerCase
    def outputStream(append: Boolean = false): OutputStream = fileObj.getContent.getOutputStream(append)

    def replicatedToLocal() = {
        val fo = fileObj
        File(fo.getFileSystem.replicateFile(fo, vfs2.Selectors.SELECT_SELF))
    }
    def delete(): Unit = fileObj.delete()
    def exists(): Boolean = fileObj.exists()
}

object Vfs2 {
    def apply(fo: vfs2.FileObject): Vfs2 = new Vfs2(fo.getName.toString)
}
