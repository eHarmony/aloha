package com.eharmony.matching.aloha.io

import java.io.{InputStream, FileInputStream, File}
import java.net.URL
import org.apache.commons.{vfs2, vfs}

trait FileReadableByInputStream[A] extends FileReadable[A] {
    final def fromFile(f: File): A = fromInputStream(new FileInputStream(f))
    final def fromUrl(u: URL): A = fromInputStream(u.openStream)
    final def fromVfs1(foVfs1: vfs.FileObject): A = fromInputStream(foVfs1.getContent.getInputStream)
    final def fromVfs2(foVfs2: vfs2.FileObject): A = fromInputStream(foVfs2.getContent.getInputStream)
    final def fromResource(r: String): A = fromInputStream(vfs2.VFS.getManager.resolveFile("res:" + r.replaceAll("""^/""", "")).getContent.getInputStream)
    final def fromClasspathResource(r: String): A = fromInputStream(vfs2.VFS.getManager.resolveFile("classpath:" + r.replaceAll("""^/""", "")).getContent.getInputStream)

    def fromInputStream(is: InputStream): A
}
