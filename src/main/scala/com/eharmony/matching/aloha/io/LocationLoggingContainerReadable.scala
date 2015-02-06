package com.eharmony.matching.aloha.io

import scala.language.higherKinds
import java.io.File
import java.net.URL
import org.apache.commons.{ vfs2, vfs }
import com.eharmony.matching.aloha.util.Logging

/**
 * Logs the locations of identifiable resources from which the objects are read.
 * @tparam C the container type
 */
trait LocationLoggingContainerReadable[C[_]] extends ContainerReadable[C] { self: Logging =>

  /**
   * Read from a file.  Logs the file.
   *
   * @param f a file to read.  The file's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromFile[A](f: File): C[A] = {
    info(s"Reading object from file: ${f.getAbsoluteFile}")
    val v = super.fromFile[A](f)
    debug(s"object read from file (${f.getAbsoluteFile}): $v")
    v
  }

  /**
   * Read from a URL.  Logs the URL.
   *
   * @param u a URL to read.  The URL's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromUrl[A](u: URL): C[A] = {
    info(s"Reading object from URL: $u")
    val v = super.fromUrl[A](u)
    debug(s"object read from URL $u: $v")
    v
  }

  /**
   * Read from an Apache VFS v1 FileObject.  Logs the file object location.
   *
   * @param foVfs1 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromVfs1[A](foVfs1: vfs.FileObject): C[A] = {
    info(s"Reading object from Apache VFS 1 file object: ${foVfs1.getName.getFriendlyURI}")
    val v = super.fromVfs1[A](foVfs1)
    debug(s"object read from Apache VFS 1 file object (${foVfs1.getName.getFriendlyURI}): $v")
    v
  }

  /**
   * Read from an Apache VFS v1 FileObject.  Logs the file object location.
   *
   * @param foVfs2 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromVfs2[A](foVfs2: vfs2.FileObject): C[A] = {
    info(s"Reading object from Apache VFS 1 file object: ${foVfs2.getName.getFriendlyURI}")
    val v = super.fromVfs2[A](foVfs2)
    debug(s"object read from Apache VFS 1 file object (${foVfs2.getName.getFriendlyURI}): $v")
    v
  }
}
