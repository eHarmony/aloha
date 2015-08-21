package com.eharmony.aloha.models.vw.jni

import java.io.{File, FileOutputStream}

import com.eharmony.aloha.io.fs.FsInstance
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import vw.VW


object VwJniModelSource {
    private[jni] val InitialRegressorPresent = """^(.*\s)?-i.*$"""
}

/**
 * Needs to be Serializable
 */
sealed trait VwJniModelSource extends Serializable {
    import VwJniModelSource._
    def params: String
    def timeMillis: Long
    def copyContentToLocalIfNecessary(dest: File): Unit
    def shouldDeleteLocalFile: Boolean

    def localFile(modelId: Long): File

    final def vwModel(modelId: Long): VW = {
        val locFile = localFile(modelId)
        val shouldDelete = shouldDeleteLocalFile

        if (shouldDelete)
            locFile.deleteOnExit()

        copyContentToLocalIfNecessary(locFile)

        val newParams = updatedVwModelParams(modelId)
        val vw = new VW(newParams)

        if (shouldDelete)
            locFile.delete()

        vw
    }

    def updatedVwModelParams(modelId: Long): String = {
        val initialRegressorParam = s" -i ${localFile(modelId).getCanonicalPath} "

        if (params matches InitialRegressorPresent)
            throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

        initialRegressorParam + params
    }

    def tmpDir() = {
        val t = File.createTempFile("abc123", "zyx987")
        t.deleteOnExit()
        val d = t.getParentFile.getCanonicalFile
        t.delete()
        d
    }

}

final case class Base64EncodedBinaryVwModelSource(
        b64EncodedBinaryVwModel: String,
        params: String = "",
        timeMillis: Long = System.currentTimeMillis()
) extends VwJniModelSource {

    def shouldDeleteLocalFile: Boolean = true

    def localFile(modelId: Long): File = new File(tmpDir(), s"aloha.vw.$modelId.$timeMillis.model")

    /**
     * Takes the model ID and base64-encoded binary VW model and creates a temp file with the decoded data
     * and returns the file handle.
     * @return File object for the newly created temp file
     */
    def copyContentToLocalIfNecessary(dest: File): Unit = {
        val decoded = Base64.decodeBase64(b64EncodedBinaryVwModel)

        // Will overwrite the original file.  This is what we want because it's idempotent.
        val fos = new FileOutputStream(dest)
        try {
            fos.write(decoded)
        }
        finally {
            IOUtils closeQuietly fos
        }
    }
}

final case class ExternallyDefinedVwModelSource(
        fsInstance: FsInstance,
        params: String = "",
        timeMillis: Long = System.currentTimeMillis()
) extends VwJniModelSource {
    def shouldDeleteLocalFile: Boolean = !fsInstance.isLocal

    def localFile(modelId: Long): File =
        fsInstance.localFile getOrElse new File(tmpDir(), s"aloha.vw.$modelId.$timeMillis.model")

    def copyContentToLocalIfNecessary(dest: File): Unit = {
        if (!fsInstance.isLocal) {
            val is = fsInstance.inputStream
            val os = new FileOutputStream(dest)
            try {
                IOUtils.copyLarge(is, os)
            }
            finally {
                IOUtils.closeQuietly(is)
                IOUtils.closeQuietly(os)
            }
        }
    }
}
