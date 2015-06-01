package com.eharmony.matching.aloha.io

import scala.language.higherKinds

import java.io.{File, InputStream, Reader}
import java.net.URL
import org.apache.commons.{vfs2, vfs}

trait ContainerReadable[C[_]] {
    def fromString[A](s: String): C[A]
    def fromFile[A](f: File): C[A]
    def fromInputStream[A](is: InputStream): C[A]
    def fromUrl[A](u: URL): C[A]
    def fromReader[A](r: Reader): C[A]
    def fromVfs1[A](foVfs1: vfs.FileObject): C[A]
    def fromVfs2[A](foVfs2: vfs2.FileObject): C[A]
    def fromResource[A](s: String): C[A]
    def fromClasspathResource[A](s: String): C[A]
}
