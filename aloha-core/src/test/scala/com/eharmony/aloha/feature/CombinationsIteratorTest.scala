package com.eharmony.aloha.feature

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class CombinationsIteratorTest {
    @Test def test1(): Unit = {
        val v = CombinationsIterator(1 to 4, 2).toVector
        assertEquals(6, v.size)
        assertEquals(Set(Vector(1, 2), Vector(1, 3), Vector(2, 3), Vector(1, 4), Vector(2, 4), Vector(3, 4)), v.toSet)
    }
}
