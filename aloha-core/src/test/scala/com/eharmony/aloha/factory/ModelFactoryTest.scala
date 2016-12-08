package com.eharmony.aloha.factory

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ModelFactoryTest {
    @Test def test1(): Unit = {
        val defaultFactory = ModelFactory.defaultFactory
        val parsersInDefault = defaultFactory.availableParsers.values.toSet
        val knownParsers = ModelFactory.knownModelParsers().toSet
        assertEquals(knownParsers, parsersInDefault)
    }
}
