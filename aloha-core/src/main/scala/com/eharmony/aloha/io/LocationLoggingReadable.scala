package com.eharmony.aloha.io

import java.io.{ FileInputStream, File }
import java.net.URL
import org.apache.commons.{ vfs2, vfs }
import com.eharmony.aloha.util.Logging

/**
 * Logs the locations of identifiable resources from which the objects are read.
 * @tparam A type of the values returned by the reading functions.
 */
trait LocationLoggingReadable[A] extends FileReadableByInputStream[A] { self: Logging =>

  /**
   * Read from a file.  Logs the file.
   *
   * @param f a file to read.  The file's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromFile(f: File): A = {
    info(s"Reading object from file: ${f.getAbsoluteFile}")
    val v = super.fromFile(f)
    debug(s"object read from file (${f.getAbsoluteFile}): $v")
    v
  }

  /**
   * Read from a URL.  Logs the URL.
   *
   * @param u a URL to read.  The URL's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromUrl(u: URL): A = {
    info(s"Reading object from URL: $u")
    val v = super.fromUrl(u)
    debug(s"object read from URL $u: $v")
    v
  }

  /**
   * Read from an Apache VFS v1 FileObject.  Logs the file object location.
   *
   * @param foVfs1 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromVfs1(foVfs1: vfs.FileObject): A = {
    info(s"Reading object from Apache VFS 1 file object: ${foVfs1.getName.getFriendlyURI}")
    val v = super.fromVfs1(foVfs1)
    debug(s"object read from Apache VFS 1 file object (${foVfs1.getName.getFriendlyURI}): $v")
    v
  }

  /**
   * Read from an Apache VFS v1 FileObject.  Logs the file object location.
   *
   * @param foVfs2 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
   * @return the result
   */
  abstract override def fromVfs2(foVfs2: vfs2.FileObject): A = {
    info(s"Reading object from Apache VFS 2 file object: ${foVfs2.getName.getFriendlyURI}")
    val v = super.fromVfs2(foVfs2)
    debug(s"object read from Apache VFS 2 file object (${foVfs2.getName.getFriendlyURI}): $v")
    v
  }
}
