package com.eharmony.matching.aloha.io

import org.apache.commons.vfs2.{VFS, FileObject}
import java.net.URL
import java.io.{File, FileInputStream}

trait ReadableCommon[A] extends AlohaReadable[A] {

    /** The character set that is used to decode the input streams in fromInputStream in the derived classes.
      */
    protected val inputCharset: String = io.Codec.UTF8.charSet.name

    /** Read from a File.
      *
      * @param f a File to read.  The File's InputStream is automatically closed.
      * @return the result
      */
    def fromFile(f: File): A = fromInputStream(new FileInputStream(f))

    /** Read from a URL.
      *
      * @param u a URL to read.  The URL's InputStream is automatically closed.
      * @return the result
      */
    def fromUrl(u: URL): A = fromInputStream(u.openStream)

    /** Read from an Apache FileObject.
      *
      * @param fo an Apache VFS FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromFileObject(fo: FileObject): A = fromInputStream(fo.getContent.getInputStream)

    /** Read from a resource.  This uses fromFileObject under the hood.
      * @param r a resource path.
      * @return
      */
    def fromResource(r: String): A = fromFileObject(VFS.getManager.resolveFile("res:" + r.replaceAll("""^/""", "")))

    /** Read from a classpath resource.  This uses fromFileObject under the hood.
      * @param r a resource path.
      * @return
      */
    def fromClasspathResource(r: String): A = fromFileObject(VFS.getManager.resolveFile("classpath:" + r.replaceAll("""^/""", "")))
}
