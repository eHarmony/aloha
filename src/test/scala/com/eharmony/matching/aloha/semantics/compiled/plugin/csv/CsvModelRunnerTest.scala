package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.{Ignore, Test}

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvModelRunnerTest {
    @Ignore @Test def test1() {
        val args = Array(
            "--missing", "",
            "--sep",     "\t",
            "--ifs",     ",",
            "-E",        "com.eh.Names=Bill:1,James:2,Dan:4",
            "-e",        "user.name=com.eh.Names",
            "-b",        "user.isMale",
            "-i",        "user.age",
            "-l",        "user.birthdate.unixtime",
            "-f",        "user.face.ratio",
            "-d",        "user.xyz",
            "-s",        "user.name.first",
            "/Users/rdeak/Desktop/chef/cookbooks/matching:scorer-service/templates/default/data/svc/scorer-service/INSTANCE/conf/models/resolved_models/50127.json"
        )

        CsvModelRunner.main(args)
    }

    @Test def test2() {
        val args = Array(
            "--missing",     "",
            "--sep",         "\t",
            "--ifs",         ",",
            "--imports",     "scala.math._,com.eharmony.matching.aloha.feature.BasicFunctions._",
            "--input-file",  "res:fizzbuzz_testinput.csv",
            "-i",            "profile.user_id",
            "res:fizzbuzz.json"
        )

        CsvModelRunner.main(args)
    }
}
