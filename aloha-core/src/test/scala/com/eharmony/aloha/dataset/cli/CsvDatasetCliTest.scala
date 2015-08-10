package com.eharmony.aloha.dataset.cli

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.test.proto.Testing.{PhotoProto, GenderProto, UserProto}
import org.apache.commons.codec.binary.Base64
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvDatasetCliTest extends CliTestHelpers {
    @Test def testAbalone(): Unit = {
        val outFile = "ram://abalone.out"

        val args = Array[String](
            "-i", "res:com/eharmony/aloha/dataset/cli/abalone.csv",
            "-c", "res:com/eharmony/aloha/dataset/cli/abalone_types.json",
            "-s", "res:com/eharmony/aloha/dataset/cli/abalone_spec.json",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv-headers",
            "--csv", outFile
        )

        val expected =
            """
              |age,sex_M,sex_F,sex_I,length,diameter,height,whole_weight,shucked_weight,viscera_weight,shell_weight
              |16.5,1,0,0,0.455,0.365,0.095,0.514,0.2245,0.101,0.15
              |8.5,1,0,0,0.35,0.265,0.09,0.2255,0.0995,0.0485,0.07
              |10.5,0,1,0,0.53,0.42,0.135,0.677,0.2565,0.1415,0.21
              |11.5,1,0,0,0.44,0.365,0.125,0.516,0.2155,0.114,0.155
              |8.5,0,0,1,0.33,0.255,0.08,0.205,0.0895,0.0395,0.055
              |9.5,0,0,1,0.425,0.3,0.095,0.3515,0.141,0.0775,0.12
              |21.5,0,1,0,0.53,0.415,0.15,0.7775,0.237,0.1415,0.33
              |17.5,0,1,0,0.545,0.425,0.125,0.768,0.294,0.1495,0.26
              |10.5,1,0,0,0.475,0.37,0.125,0.5095,0.2165,0.1125,0.165
              |20.5,0,1,0,0.55,0.44,0.15,0.8945,0.3145,0.151,0.32
              |15.5,0,1,0,0.525,0.38,0.14,0.6065,0.194,0.1475,0.21
              |11.5,1,0,0,0.43,0.35,0.11,0.406,0.1675,0.081,0.135
              |12.5,1,0,0,0.49,0.38,0.135,0.5415,0.2175,0.095,0.19
              |11.5,0,1,0,0.535,0.405,0.145,0.6845,0.2725,0.171,0.205
              |11.5,0,1,0,0.47,0.355,0.1,0.4755,0.1675,0.0805,0.185
              |13.5,1,0,0,0.5,0.4,0.13,0.6645,0.258,0.133,0.24
              |8.5,0,0,1,0.355,0.28,0.085,0.2905,0.095,0.0395,0.115
              |11.5,0,1,0,0.44,0.34,0.1,0.451,0.188,0.087,0.13
              |8.5,1,0,0,0.365,0.295,0.08,0.2555,0.097,0.043,0.1
              |10.5,1,0,0,0.45,0.32,0.1,0.381,0.1705,0.075,0.115
            """.stripMargin

        DatasetCli.main(args)
        assertEquals(expected.trim, output(outFile).trim)
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
            "-s", "res:com/eharmony/aloha/dataset/cli/csv_spec2.js",
            "-c", "res:com/eharmony/aloha/dataset/cli/csv_types1.js",
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
            "-s", "res:com/eharmony/aloha/dataset/cli/csv_spec2.js",
            "-c", "res:com/eharmony/aloha/dataset/cli/csv_types1.js",
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
            "-s", "res:com/eharmony/aloha/dataset/cli/csv_spec3.js",
            "-c", "res:com/eharmony/aloha/dataset/cli/csv_types1.js",
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
            "-s", "res:com/eharmony/aloha/dataset/cli/csv_spec4.js",
            "--cachedir", FileLocations.testGeneratedClasses.getCanonicalPath,
            "--csv-headers",
            "--csv", outFile
        )

        DatasetCli.main(args)
        assertEquals(expected, output(outFile).trim)
    }
}
