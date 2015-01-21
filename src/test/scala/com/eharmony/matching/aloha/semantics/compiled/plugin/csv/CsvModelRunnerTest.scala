package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.{ Ignore, Test }
import org.junit.Assert._
import org.apache.commons.vfs2.VFS

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvModelRunnerTest {
  @Test def test() {
    // Use a temp file so we can get at the results for comparison.  Also shows how to write to a VFS file.
    val tmpFile = "ram://results.txt"

    val expected = "1\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t1\n2\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t2\n3\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-2\n4\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t4\n5\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-4\n6\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-2\n7\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t7\n8\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t8\n9\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-2\n10\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-4\n11\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t11\n12\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-2\n13\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t13\n14\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t14\n15\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-6\n16\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t16\n17\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t17\n18\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-2\n19\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t19\n20\tBill\ttrue\t13\t1417845974683\t1.2\t-1.7976931348623157\tJones\t-4"

    val args = Array(

      "--missing", "", // The missing field indicator.
      "--sep", "\t", // The field separator character.
      "--ifs", ",", // The intra-field separator string.

      // imports to statically import user defined functions.
      "--imports", "scala.math._,com.eharmony.matching.aloha.feature.BasicFunctions._",
      "--input-file", "res:fizzbuzz_testinput.csv", // File to read from.  If not provided, read STDIN.
      "--output-file", tmpFile, // File to write to.  If not provided, write STDOUT.
      "--output-type", "Double", // Specify a Type for the output.
      "-B", // Output input before the model result.
      "-E", "com.eh.Names=Bill:1,James:2,Dan:4", // Define an enumerated type.

      // Define the first column of input as a field with integer type and the name "profile.user_id".
      // The data here will be available to the model as an Int when a variable "${profile.user_id}" is
      // referenced in the model.  Each subsequent column index is defined implicitly based on order in
      // parameter list.
      "-i", "profile.user_id",
      "-e", "user.name=com.eh.Names",
      "-b", "user.isMale",
      "-i", "user.age",
      "-l", "user.birthdate.unixtime",
      "-f", "user.face.ratio",
      "-d", "user.xyz",
      "-s", "user.name.last",
      "res:fizzbuzz.json" // The VFS URL to the model.
      )

    CsvModelRunner.main(args)

    // Read the result to a string.
    val result = io.Source.fromInputStream(VFS.getManager.resolveFile(tmpFile).getContent.getInputStream).getLines().mkString("\n")

    assertEquals(expected.trim, result.trim)
  }
}
