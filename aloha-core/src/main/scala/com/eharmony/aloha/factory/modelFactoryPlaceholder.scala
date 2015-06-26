package com.eharmony.aloha.factory

import java.io.File
import scala.util.{Failure, Try}

import org.apache.commons.{vfs, vfs2}

import spray.json.JsValue
import spray.json.pimpString

import com.eharmony.aloha.factory.ex.AlohaFactoryException
import com.eharmony.aloha.io.StringReadable

/** Get
  *
  * @param `import`
  * @param via
  * @author R. M. Deak
  */
private[factory] case class ImportedModelPlaceholderAst(`import`: String, via: Option[String]) {
    def toJsValue: Try[ImportedModelPlaceholder] = via match {
        case None => Try { Vfs2ImportedModelPlaceholder(`import`) }
        case Some("vfs1") => Try { Vfs1ImportedModelPlaceholder(`import`) }
        case Some("vfs2") => Try { Vfs2ImportedModelPlaceholder(`import`) }
        case Some("file") => Try { FileImportedModelPlaceholder(`import`) }
        case other => Failure { new AlohaFactoryException(s"unrecognized import type: $other") }
    }
}

/**
  *
  * @author R. M. Deak
  */
private[factory] sealed trait ImportedModelPlaceholder {

    /**
      *
      */
    val fileDescriptor: String

    /**
      *
      * @return
      */
    def resolveFileContents(): Try[JsValue]
}

private[factory] case class Vfs2ImportedModelPlaceholder(fileDescriptor: String) extends ImportedModelPlaceholder {
    def resolveFileContents() = for {
        file <- Try {
            vfs2.VFS.getManager.resolveFile(fileDescriptor)
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't resolve VFS2 file: $fileDescriptor", f) }
        }
        json <- Try {
            StringReadable.fromVfs2(file).parseJson
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't get JSON for VFS2 file: $file", f) }
        }
    } yield json
}

private[factory] case class Vfs1ImportedModelPlaceholder(fileDescriptor: String) extends ImportedModelPlaceholder {
    def resolveFileContents() = for {
        file <- Try {
            vfs.VFS.getManager.resolveFile(fileDescriptor)
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't resolve VFS1 file: $fileDescriptor", f) }
        }
        json <- Try {
            StringReadable.fromVfs1(file).parseJson
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't get JSON for VFS1 file: $file", f) }
        }
    } yield json
}

private[factory] case class FileImportedModelPlaceholder(fileDescriptor: String) extends ImportedModelPlaceholder {
    def resolveFileContents() = for {
        file <- Try {
            new File(fileDescriptor)
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't resolve file: $fileDescriptor", f) }
        }
        json <- Try {
            StringReadable.fromFile(file).parseJson
        } recoverWith {
            case f => Failure { new AlohaFactoryException(s"Couldn't get JSON for file: $file", f) }
        }
    } yield json
}
