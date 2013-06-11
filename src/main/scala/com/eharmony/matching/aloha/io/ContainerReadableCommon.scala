package com.eharmony.matching.aloha.io

import scala.language.higherKinds
import java.io.{FileInputStream, File}
import java.net.URL
import org.apache.commons.vfs2.{VFS, FileObject}

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

    /** Read from an Apache FileObject.
      *
      * @param fo an Apache VFS FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromFileObject[A](fo: FileObject): C[A] = fromInputStream[A](fo.getContent.getInputStream)

    /** Read from a resource.  This uses fromFileObject under the hood.
      * @param r a resource path
      * @return
      */
    def fromResource[A](r: String): C[A] = fromFileObject[A](VFS.getManager.resolveFile("res:" + r.replaceAll("""^/""", "")))

    /** Read from a classpath resource.  This uses fromFileObject under the hood.
      * @param r a resource path
      * @return
      */
    def fromClasspathResource[A](r: String): C[A] = fromFileObject[A](VFS.getManager.resolveFile("classpath:" + r.replaceAll("""^/""", "")))
}
