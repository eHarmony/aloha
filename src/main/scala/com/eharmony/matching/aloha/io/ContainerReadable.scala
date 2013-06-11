package com.eharmony.matching.aloha.io

import scala.language.higherKinds

import org.apache.commons.vfs2.FileObject
import java.io.{File, InputStream, Reader}
import java.net.URL

trait ContainerReadable[C[_]] {
    def fromString[A](s: String): C[A]
    def fromFile[A](f: File): C[A]
    def fromInputStream[A](is: InputStream): C[A]
    def fromUrl[A](u: URL): C[A]
    def fromReader[A](r: Reader): C[A]
    def fromFileObject[A](fo: FileObject): C[A]
    def fromResource[A](s: String): C[A]
    def fromClasspathResource[A](s: String): C[A]
}
