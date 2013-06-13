package com.eharmony.matching.aloha.io

import java.net.URL
import java.io.{File, FileInputStream}
import org.apache.commons.{vfs2, vfs}

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
      * @param foVfs1 an Apache v1 VFS FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs1(foVfs1: vfs.FileObject): A = fromInputStream(foVfs1.getContent.getInputStream)

    /** Read from an Apache FileObject.
      *
      * @param foVfs2 an Apache VFS v2 FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs2(foVfs2: vfs2.FileObject): A = fromInputStream(foVfs2.getContent.getInputStream)

    /** Read from a resource.  This uses fromFileObject under the hood.
      * @param r a resource path.
      * @return
      */
    def fromResource(r: String): A = fromVfs2(vfs2.VFS.getManager.resolveFile("res:" + r.replaceAll("""^/""", "")))

    /** Read from a classpath resource.  This uses fromFileObject under the hood.
      * @param r a resource path.
      * @return
      */
    def fromClasspathResource(r: String): A = fromVfs2(vfs2.VFS.getManager.resolveFile("classpath:" + r.replaceAll("""^/""", "")))
}
