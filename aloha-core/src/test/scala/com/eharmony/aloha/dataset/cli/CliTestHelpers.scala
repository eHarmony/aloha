package com.eharmony.aloha.dataset.cli

import java.io.OutputStreamWriter

import com.eharmony.aloha.io.StringReadable
import org.apache.commons.vfs2.VFS

trait CliTestHelpers {
    def input(vfsUrl: String, data: String*): Unit = {
        val content = VFS.getManager.resolveFile(vfsUrl).getContent
        new OutputStreamWriter(content.getOutputStream).append(data.mkString("\n")).close()
    }

    def output(vfsUrl: String) = StringReadable.fromVfs2(VFS.getManager.resolveFile(vfsUrl))
}
