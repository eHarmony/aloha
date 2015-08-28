package com.eharmony.aloha.models.vw.jni

import java.io.{File, FileOutputStream}

import com.eharmony.aloha.io.fs.FsInstance
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import vw.VW


object VwJniModelSource {

    /**
     * A regular expression to check whether an initial regressor is present in the VW parameters.
     */
    private[jni] val InitialRegressorPresent = """^(.*\s)?-i.*$"""
}

/**
 *
 * '''NOTE''': ''Must be Serializable.''
 */
sealed trait VwJniModelSource extends Serializable {
    import VwJniModelSource._

    /**
     * The VW parameters.
     * @return
     */
    def params: String

    /**
     * This acts as a secondary piece of information on which the temp file names are based.  It doesn't
     * need to be supplied because a default value is provided: the unixtime in milliseconds.
     * @return
     */
    def timeMillis: Long

    /**
     * The only effectful function to be provided by the subclass.  ''If necessary'', this function copies some
     * representation of the VW model to the destination file.
     * @param dest local destination where the VW binary model will be copied.
     */
    def copyContentToLocalIfNecessary(dest: File): Unit

    /**
     * Indicates whether to attempt to delete the local file was it is determined it is no longer needed.
     * If ''copyContentToLocalIfNecessary'' doesn't copy any content to the local disk, then this should
     * return false.  If it does, this function should return true.
     * @return
     */
    def shouldDeleteLocalFile: Boolean

    /**
     * The local file that will eventually contain the binary VW model.
     * @param modelId The numeric model identifier.
     * @return
     */
    def localFile(modelId: Long): File

    /**
     * Get the VW model.  If necessary, this will create a local file containing a VW binary model
     * and will use the initial regressor flag to use it as the starting point to the model.
     * @param modelId The numeric model identifier.
     * @return
     */
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

    /**
     * Takes the params string and injects the initial regressor parameter.
     * @param modelId The numeric model identifier.
     * @return
     */
    def updatedVwModelParams(modelId: Long): String = {
        val initialRegressorParam = s" -i ${localFile(modelId).getCanonicalPath} "

        if (params matches InitialRegressorPresent)
            throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

        initialRegressorParam + params
    }

    /**
     * Finds the temp directory to be used.
     * @return
     */
    def tmpDir() = {
        val t = File.createTempFile("DOESNT", "MATTER")
        t.deleteOnExit()
        val d = t.getParentFile.getCanonicalFile
        t.delete()
        d
    }

}

/**
 * A model source used when the VW binary model is encoded as a base64-encoded string.  The content will always
 * be copied to a local temp file.
 * @param b64EncodedBinaryVwModel A base64-encoded String representation of the binary VW model content.
 * @param params VW model parameters
 * @param timeMillis timestamp used to help name temp files.
 */
final case class Base64EncodedBinaryVwModelSource(
        b64EncodedBinaryVwModel: String,
        params: String = "",
        timeMillis: Long = System.currentTimeMillis()
) extends VwJniModelSource {

    /**
     * @return true
     */
    def shouldDeleteLocalFile: Boolean = true

    /**
     * The local temp file.
     * @param modelId The numeric model identifier.
     * @return
     */
    def localFile(modelId: Long): File = new File(tmpDir(), s"aloha.vw.$modelId.$timeMillis.model")

    /**
     * Decodes the base64-encoded binary VW model and writes it to a temp file at ''dest''.
     * @param dest where to write the binary VW model.
     * @return
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

/**
 * A model source used when the ''location'' of the VW binary model provided.  If the model is available
 * locally, it will neither be copied nor deleted.  Otherwise, it'll be copied and deleted when no longer
 * needed.  The use case for this is copying a large VW model to each node on a cluster and using it over
 * many batched runs without deleting.  This avoids the network overhead when copying from somewhere (like
 * HDFS).
 * @param fsInstance A file system instance pointing to a binary VW model file.  This could be backed by
 *                   Apache VFS1, VFS2 or a File.  The reason to provide these different abstractions is
 *                   that different file system abstractions have different plugins that allow different
 *                   virtual file systems to be used.  For instance, the HDFS plugin used by eH is currently
 *                   available for VFS1.  An S3 plugin might be on VFS2.  But VFS1 and VFS2 used in concert
 *                   with each other may through exceptions.  Therefore, we allow the user to specify what
 *                   to use so that automatic plugin support is available.
 * @param params VW model parameters
 * @param timeMillis timestamp used to help name temp files.
 */
final case class ExternallyDefinedVwModelSource(
        fsInstance: FsInstance,
        params: String = "",
        timeMillis: Long = System.currentTimeMillis()
) extends VwJniModelSource {
    def shouldDeleteLocalFile: Boolean = !fsInstance.existsLocally

    /**
     * Points to either a non-temp file that won't be deleted or a temp file that will be deleted.
     * @param modelId The numeric model identifier.
     * @return
     */
    def localFile(modelId: Long): File =
        fsInstance.localFile getOrElse new File(tmpDir(), s"aloha.vw.$modelId.$timeMillis.model")

    /**
     * Only copies in the file system instance points to a file that is not local.
     * @param dest local destination where the VW binary model will be copied.
     */
    def copyContentToLocalIfNecessary(dest: File): Unit = {
        if (!fsInstance.existsLocally) {
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
