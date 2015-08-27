package com.eharmony.aloha.io

import java.io._

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
package object fs {
    object FsType extends Enumeration {
        type FsType = Value
        val file, vfs1, vfs2 = Value

        case class JsonReader(viaField: String) extends RootJsonReader[FsType] {
            override def read(json: JsValue) = {
                json.asJsObject("IoType expects an object").getFields(viaField) match {
                    case Seq(JsString(via)) => FsType.withName(via)
                    case _ => throw new DeserializationException(s"FsType expects the field '$viaField' (string) to be present.")
                }
            }
        }
    }

    sealed trait FsInstance {
        def fsType: FsType.FsType
        def inputStream: InputStream
        def outputStream(append: Boolean = false): OutputStream
        def localFile: Option[File]
        def existsLocally = localFile exists (_.exists)
        def descriptor: String
    }

    object FsInstance {
        import com.eharmony.aloha.io.fs.FsType.FsType

        def fromFsType(tpe: FsType) = tpe match {
            case FsType.file => (d: String) => FileFsInstance(d)
            case FsType.vfs1 => (d: String) => Vfs1FsInstance(d)
            case FsType.vfs2 => (d: String) => Vfs2FsInstance(d)
        }

        case class JsonReader(viaField: String, descriptorField: String) extends RootJsonReader[FsInstance] {
            override def read(json: JsValue): FsInstance = {
                json.asJsObject("IoType expects an object").getFields(viaField, descriptorField) match {
                    case Seq(via, JsString(descriptor)) => fromFsType(via.convertTo(FsType.JsonReader(viaField)))(descriptorField)
                    case _ => throw new DeserializationException(s"IoInstance expects the fields '$viaField' and $descriptorField (string) to be present.")
                }
            }
        }
    }

    case class FileFsInstance(descriptor: String) extends FsInstance {
        def fileObj: File = new File(descriptor)
        def fsType = FsType.file
        def inputStream = new FileInputStream(fileObj)
        def outputStream(append: Boolean = false) = new FileOutputStream(fileObj, append)
        def isLocalFile: Boolean = true
        def localFile = Option(fileObj)
    }

    case class Vfs1FsInstance(descriptor: String) extends FsInstance {
        def fileObj: vfs1.FileObject = vfs1.VFS.getManager.resolveFile(descriptor)
        def fsType = FsType.vfs1
        def inputStream = fileObj.getContent.getInputStream
        def outputStream(append: Boolean = false) = fileObj.getContent.getOutputStream(append)

        // TODO: Need to find a less brittle way to test this
        def localFile =
            if ("file" == fileObj.getName.getScheme.toLowerCase)
                Option(new File(fileObj.getName.getPath))
            else None
    }

    case class Vfs2FsInstance(descriptor: String) extends FsInstance {
        def fileObj: vfs2.FileObject = vfs2.VFS.getManager.resolveFile(descriptor)
        def fsType = FsType.vfs2
        def inputStream = fileObj.getContent.getInputStream
        def outputStream(append: Boolean = false) = fileObj.getContent.getOutputStream(append)

        // TODO: Need to find a less brittle way to test this
        def localFile =
            if ("file" == fileObj.getName.getScheme.toLowerCase)
                Option(new File(fileObj.getName.getPath))
            else None
    }
}
