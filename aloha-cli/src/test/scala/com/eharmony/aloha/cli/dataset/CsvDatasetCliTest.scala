package com.eharmony.aloha.cli.dataset

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.test.proto.Testing.{GenderProto, PhotoProto, UserProto}
import org.apache.commons.codec.binary.Base64
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvDatasetCliTest extends CliTestHelpers {
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

    @Test def testCsvInCsvOutWithHeadersHotOneEncodingRealEnum(): Unit = {
        val inFile = "ram://csvInCsvOut.in"
        val outFile = "ram://csvInCsvOutWithHeadersHotOneEncodingRealEnum.out"

        val inData = Seq(
            UserProto.newBuilder().
                setId(1).
                setGender(GenderProto.MALE).
                setBmi(15).
                addPhotos(PhotoProto.newBuilder().setId(1).setAspectRatio(0.5625)). // 9 / 16
                build().
                toByteArray,
            UserProto.newBuilder().
                setId(2).
                setGender(GenderProto.FEMALE).
                setBmi(20).
                addPhotos(PhotoProto.newBuilder().setId(1).setAspectRatio(0.75)).
                build().
                toByteArray
        ).map(d => new String(Base64.encodeBase64(d)))

        input(inFile, inData:_*)

        val expected = Seq(
            "uid,gender_MALE,gender_FEMALE,bmi,aspect_ratio",
            "1,1,0,15.0,0.5625",
            "2,0,1,20.0,0.75"
        ).mkString("\n")

        val args = Array[String](
            "-i", inFile,
            "-p", "com.eharmony.aloha.test.proto.Testing.UserProto",
            "-s", "res:com/eharmony/aloha/cli/dataset/csv_spec4.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv-headers",
            "--csv", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }
}
