package com.eharmony.matching.aloha

import java.util.Properties
import org.apache.commons.vfs2.{FileObject, VFS}
import java.io.{IOException, File, StringReader}

/** This should probably be moved over to aloha-compiled-semantics
  *
  */
object FileLocations {
    private[this] val fileName = "mvn_gen_test.properties"
    private[this] val testGeneratedClassesVar = "testGeneratedClasses"
    private[this] val buildDirectoryVar = "targetDirectory"
    private[this] val testClassesDirectoryVar = "testClassesDirectory"
    private[this] val testDirectoryVar = "testDirectory"  // ./src/test

    private[this] val props = {
        // Get the string representation and let Source close everything.
        val s = scala.io.Source.fromInputStream(VFS.getManager.resolveFile(s"res:$fileName").getContent.getInputStream).mkString
        val p = new Properties
        p.load(new StringReader(s))
        p
    }

    private[this] def getFilesForVariable(varName: String): (File, FileObject) = {
        val p = Option(props.getProperty(varName)) getOrElse {
            throw new IllegalArgumentException(s"No property '$varName' in $fileName")
        }

        val d = new File(p)
        if (d.exists) {
            if (!(d.isDirectory && d.canRead && d.canWrite && d.canExecute)) {
                throw new IOException(s"$varName directory: ${d.getCanonicalPath} is not RWXable")
            }
        }
        else if (!d.mkdir)
            throw new IOException(s"Couldn't create $varName directory: " + d.getCanonicalPath)

        val vfs = VFS.getManager.resolveFile(p)

        (d, vfs)
    }

    /** Provides File and VFS file locations for a directory where Aloha generated classes created for testing can be
      * placed.  This comes from the test.properties file whose values get injected by Maven's resource plugin.
      */
    val (testGeneratedClasses, testGeneratedClassesVfs) = getFilesForVariable(testGeneratedClassesVar)

    /** The build directory.
      */
    val (buildDirectory, buildDirectoryVfs) = getFilesForVariable(buildDirectoryVar)

    /** The test classes directory.
      */
    val (testClassesDirectory, testClassesDirectoryVfs) = getFilesForVariable(testClassesDirectoryVar)

    /** The src/test directory.
      */
    val (testDirectory, testDirectoryVfs) = getFilesForVariable(testDirectoryVar)
}
