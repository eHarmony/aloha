package com.eharmony.matching.aloha

import java.util.Properties
import org.apache.commons.vfs2.VFS
import java.io.{IOException, File, StringReader}

/** This should probably be moved over to aloha-compiled-semantics
  *
  */
object FileLocations {
    private[this] val props = {
        // Get the string representation and let Source close everything.
        val s = scala.io.Source.fromInputStream(VFS.getManager.resolveFile("res:mvn_gen_test.properties").getContent.getInputStream).mkString
        val p = new Properties
        p.load(new StringReader(s))
        p
    }

    /** Provides File and VFS file locations for a directory where Aloha generated classes created for testing can be
      * placed.  This comes from the test.properties file whose values get injected by Maven's resource plugin.
      */
    val (testGeneratedClasses, testGeneratedClassesVfs) = {
        val p = props.getProperty("testGeneratedClasses")
        val d = new File(p)
        if (d.exists) {
            if (!(d.isDirectory && d.canRead && d.canWrite && d.canExecute)) {
                throw new IOException(s"test generated directory: ${d.getCanonicalPath} is not RWXable")
            }
        }
        else if (!d.mkdir)
            throw new IOException("Couldn't create test generated directory: " + d.getCanonicalPath)

        val vfs = VFS.getManager.resolveFile(p)

        (d, vfs)
    }
}
