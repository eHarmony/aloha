package com.eharmony.matching.aloha.io

import java.io.{Reader, InputStream, File}
import java.net.URL
import org.apache.commons.{vfs, vfs2}

trait AlohaReadable[A] {
    def fromString(s: String): A
    def fromFile(f: File): A
    def fromInputStream(is: InputStream): A
    def fromUrl(u: URL): A
    def fromReader(r: Reader): A
    def fromVfs1(foVfs1: vfs.FileObject): A
    def fromVfs2(foVfs2: vfs2.FileObject): A
    def fromResource(s: String): A
    def fromClasspathResource(s: String): A
}
