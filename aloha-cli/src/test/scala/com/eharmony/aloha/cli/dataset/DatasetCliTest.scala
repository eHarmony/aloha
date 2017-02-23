package com.eharmony.aloha.cli.dataset

import com.eharmony.aloha.FileLocations
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Not testing STDIN here.  It hangs in IDE due to lack of test isolation.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class DatasetCliTest extends CliTestHelpers {

    @Test def testCsvInVwOut(): Unit = {
        val inFile = "ram://csvInVwOut.in"
        val outFile = "ram://csvInVwOut.out"

        input(inFile,
            "MALE,205,stuff|things,,",
            "FEMALE,115,,,"
        )

        val expected = Seq(
            "| gender=MALE weight=200 num_likes=2",
            "| gender=FEMALE weight=110 num_likes=0"
        ).mkString("\n")

        val args = Array[String](
            "-i", inFile,
            "-s", "res:com/eharmony/aloha/cli/dataset/csv_spec1.js",
            "-c", "res:com/eharmony/aloha/cli/dataset/csv_types1.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--vw", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }

    @Test def testProtoInVwOut(): Unit = {
        val outFile = "ram://protoInVwOut.out"

        val expected = Seq(
            "| name=Alan gender=MALE bmi:23 num_photos:2",
            "| name=Kate gender=FEMALE bmi=UNK num_photos"
        ).mkString("\n")

        val args = Array[String](
            "-i", "res:fizz_buzzs.proto",
            "-s", "res:com/eharmony/aloha/cli/dataset/proto_spec1.js",
            "-p", "com.eharmony.aloha.test.proto.Testing.UserProto",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--vw", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }

    @Test def testProtoInVwOut2(): Unit = {
        val outFile = "ram://protoInVwOut2.out"

        val expected = Seq(
            "1 1| name=Alan gender=MALE bmi:23 |photos num_photos:2 avg_photo_height",
            "1 1| name=Kate gender=FEMALE bmi=UNK |photos num_photos avg_photo_height:3"
        ).mkString("\n")

        val args = Array[String](
            "-i", "res:fizz_buzzs.proto",
            "-s", "res:com/eharmony/aloha/cli/dataset/proto_spec2.json",
            "-p", "com.eharmony.aloha.test.proto.Testing.UserProto",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--vw_labeled", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }


    @Test def testCsvInCsvOut(): Unit = {
        val inFile = "ram://csvInCsvOut.in"
        val outFile = "ram://csvInCsvOut.out"

        input(inFile,
            "MALE,175,,,",
            "FEMALE,,books|films|chinese food,,"
        )

        val expected = Seq(
            "MALE,170,0",
            "FEMALE,NULL,3"
        ).mkString("\n")

        val args = Array[String](
            "-i", inFile,
            "-s", "res:com/eharmony/aloha/cli/dataset/csv_spec2.js",
            "-c", "res:com/eharmony/aloha/cli/dataset/csv_types1.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }

    @Test def testCsvInCsvOutWithHeadersRegularEncoding(): Unit = {
        val inFile = "ram://csvInCsvOut.in"
        val outFile = "ram://csvInCsvOutWithHeadersRegularEncoding.out"

        input(inFile,
            "MALE,175,,,",
            "FEMALE,,books|films|chinese food,,"
        )

        val expected = Seq(
            "gender,weight,num_likes",
            "MALE,170,0",
            "FEMALE,NULL,3"
        ).mkString("\n")

        val args = Array[String](
            "-i", inFile,
            "-s", "res:com/eharmony/aloha/cli/dataset/csv_spec2.js",
            "-c", "res:com/eharmony/aloha/cli/dataset/csv_types1.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv-headers",
            "--csv", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }

    @Test def testCsvInCsvOutWithHeadersHotOneEncoding(): Unit = {
        val inFile = "ram://csvInCsvOut.in"
        val outFile = "ram://csvInCsvOutWithHeadersHotOneEncoding.out"

        input(inFile,
            "MALE,175,,,",
            "FEMALE,,books|films|chinese food,,"
        )

        val expected = Seq(
            "gender_MALE,gender_FEMALE,weight,num_likes",
            "1,0,170,0",
            "0,1,NULL,3"
        ).mkString("\n")

        val args = Array[String](
            "-i", inFile,
            "-s", "res:com/eharmony/aloha/cli/dataset/csv_spec3.js",
            "-c", "res:com/eharmony/aloha/cli/dataset/csv_types1.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv-headers",
            "--csv", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }


}
