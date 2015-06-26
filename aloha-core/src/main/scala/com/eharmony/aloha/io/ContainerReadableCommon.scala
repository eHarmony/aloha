package com.eharmony.aloha.io

import scala.language.higherKinds
import java.io.{FileInputStream, File}
import java.net.URL
import org.apache.commons.{vfs, vfs2}

trait ContainerReadableCommon[C[_]] extends ContainerReadable[C] {

    /** The character set that is used to decode the input streams in fromInputStream in the derived classes.
      */
    protected val inputCharset: String = io.Codec.UTF8.charSet.name

    /** Read from a File.
      *
      * @param f a File to read.  The File's InputStream is automatically closed.
      * @return the result
      */
    def fromFile[A](f: File): C[A] = fromInputStream[A](new FileInputStream(f))

    /** Read from a URL.
      *
      * @param u a URL to read.  The URL's InputStream is automatically closed.
      * @return the result
      */
    def fromUrl[A](u: URL): C[A] = fromInputStream[A](u.openStream)

    /** Read from an Apache VFS v1 FileObject.
      *
      * @param foVfs1 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs1[A](foVfs1: vfs.FileObject): C[A] = fromInputStream[A](foVfs1.getContent.getInputStream)

    /** Read from an Apache VFS v1 FileObject.
      *
      * @param foVfs2 an Apache VFS v1 FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs2[A](foVfs2: vfs2.FileObject): C[A] = fromInputStream[A](foVfs2.getContent.getInputStream)

    /** Read from a resource.  This uses fromFileObject under the hood.
      * @param r a resource path
      * @return
      */
    def fromResource[A](r: String): C[A] = fromVfs2[A](vfs2.VFS.getManager.resolveFile("res:" + r.replaceAll("""^/""", "")))

    /** Read from a classpath resource.  This uses fromFileObject under the hood.
      * @param r a resource path
      * @return
      */
    def fromClasspathResource[A](r: String): C[A] = fromVfs2[A](vfs2.VFS.getManager.resolveFile("classpath:" + r.replaceAll("""^/""", "")))
}
