package com.eharmony.matching.aloha.io

import java.io.{Reader, InputStream, File}
import java.net.URL
import org.apache.commons.vfs2.FileObject

trait AlohaReadable[A] {
    def fromString(s: String): A
    def fromFile(f: File): A
    def fromInputStream(is: InputStream): A
    def fromUrl(u: URL): A
    def fromReader(r: Reader): A
    def fromFileObject(fo: FileObject): A
    def fromResource(s: String): A
    def fromClasspathResource(s: String): A
}
