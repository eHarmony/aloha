package com.eharmony.aloha.factory

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ModelFactoryTest {
    @Test def test1(): Unit = {
        val defaultFactory = ModelFactory.defaultFactory
        val known = ModelFactory.knownModelParsers()
        val a = 1
    }
}
