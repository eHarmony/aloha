package com.eharmony.aloha.io

import java.io.File
import java.net.URL
import org.apache.commons.{vfs => vfs1, vfs2}

trait FileReadable[A] {

    /** Read from a File.
      *
      * @param f a File to read.  The File's InputStream is automatically closed.
      * @return the result
      */
    def fromFile(f: File): A

    /** Read from a URL.
      *
      * @param u a URL to read.  The URL's InputStream is automatically closed.
      * @return the result
      */
    def fromUrl(u: URL): A

    /** Read from an Apache FileObject.
      *
      * @param foVfs1 an Apache v1 VFS FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs1(foVfs1: vfs1.FileObject): A

    /** Read from an Apache FileObject.
      *
      * @param foVfs2 an Apache VFS v2 FileObject to read.  The FileObject's InputStream is automatically closed.
      * @return the result
      */
    def fromVfs2(foVfs2: vfs2.FileObject): A

    /** Read from a resource.  This uses fromVfs2 under the hood.
      * @param r a resource path.
      * @return
      */
    def fromResource(r: String): A

    /** Read from a classpath resource.  This uses fromVfs2 under the hood.
      * @param r a classpath resource path.
      * @return
      */
    def fromClasspathResource(r: String): A
}
