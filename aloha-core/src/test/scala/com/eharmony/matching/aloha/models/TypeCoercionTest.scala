package com.eharmony.matching.aloha.models

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class TypeCoercionTest {
    @Test def test1(): Unit = {
        val sof = TypeCoercion[String, Option[Float]]
        assertEquals(Option(1234.567f), sof.get.apply("1234.567"))

        val sof1 = TypeCoercion[Option[String], Option[Float]]
        assertEquals(Option(1234.567f), sof1.get.apply(Option("1234.567")))
    }
}
