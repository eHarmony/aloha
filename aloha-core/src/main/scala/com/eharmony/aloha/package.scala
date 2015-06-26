package com.eharmony

import java.util.Properties

import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.VFS


package object aloha {
    def pkgName = getClass.getPackage.getName
    def version: String = _version

    private[this] lazy val _version: String = {
        val is = VFS.getManager.resolveFile("res:" + pkgName.replaceAll("\\.", "/") + "/version.properties").getContent.getInputStream
        try {
            val p = new Properties()
            p.load(is)
            p.getProperty("aloha.version")
        }
        finally {
            IOUtils closeQuietly is
        }
    }
}
