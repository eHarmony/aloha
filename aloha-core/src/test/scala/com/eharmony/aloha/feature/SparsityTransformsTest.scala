package com.eharmony.aloha.feature

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._
import scala.util.Random

@RunWith(classOf[BlockJUnit4ClassRunner])
class SparsityTransformsTest {
    import SparsityTransforms._

    /** This isn't required but in order to make the IDE not barf and produce red squigglies, we cast to the super type
      * IndexedSeq.  This is because IDEs don't necessarily know that a Range.Inclusive (with 0 type parameters) work
      * as a domain in the densify functions.  The scala compiler can handle this fine.
      */
    private[this] val _3to6: IndexedSeq[Int] = 3 to 6

    private[this] val _map = (Array(4, 6) zip Array(1, 2)).toMap

    def parIterableInverseLaw[A, B](denseDomain: Iterable[A], sparseKeys: Iterable[A], sparseVals: Iterable[B]): Boolean = {
        val df = densifyPI(denseDomain, sparseKeys, sparseVals.map(Option.apply), None).flatten
        val k = sparseKeys.toSet
        val m = (sparseKeys zip sparseVals).toMap
        val v = denseDomain.filter(k.contains).map(m.apply)
        v == df
    }

    def mapInverseLaw[A, B](denseDomain: Iterable[A], mapping: Map[A, B]): Boolean = {
        val df = densifyMap(denseDomain, mapping.map{ case(k, v) => (k, Option(v)) }, None).flatten
        val k = mapping.keySet
        val v = denseDomain.collect{ case d if k contains d => mapping(d) }
        v == df
    }

    def fnInverseLaw[A, B](denseDomain: Iterable[A], fn: A => Option[B]): Boolean = {
        val df = densifyFn(denseDomain, fn.andThen(Option.apply), None).flatten
        val k = denseDomain.filter(fn(_).nonEmpty)
        val v = k.flatMap(fn.apply)
        v == df
    }

    /** Test that when we
      */
    @Test def testSparifiedDensify() {
        implicit val r = new Random(0)

        (1 to 100) foreach { i => {
            val n = r.nextInt(100)
            val d = Seq.fill(r.nextInt(10000))(r.nextInt(1000))
            val k = Seq.fill(n)(r.nextInt(1000))
            val v = Seq.fill(n)(r.nextDouble())

            val m = k.zip(v).toMap
            val f = m.get _

            assertTrue(s" test $i iterable: ", parIterableInverseLaw(d, k, v))
            assertTrue(s" test $i map: ", mapInverseLaw(d, m))
            assertTrue(s" test $i function: ", fnInverseLaw(d, f))
        }}
    }

    @Test def testDensifyPI() {

        val res = densifyPI(_3to6, Array(4, 6), Array(1, 2), 0)
        assertEquals(Vector(0, 1, 0, 2), res)

        // Show off the cool CBF / functor stuff.  Vector because 3 to 6 is an IndexedSeq.
        assertEquals("scala.collection.immutable.Vector", res.getClass.getCanonicalName)
    }

    @Test def testDensifyPIwithEmptyKeys() {
        val res = densifyPI(_3to6, Seq.empty, Array(1, 2), 0)
        assertEquals(Vector.fill(4)(0), res)
    }

    @Test def testDensifyPIwithEmptyValues() {
        val res = densifyPI(_3to6, Array(4, 6), Seq.empty, 0)
        assertEquals(Vector.fill(4)(0), res)
    }

    @Test def testDensifyF() {
        val f = _map.get _

        val res = densifyFn(_3to6, f, 0)
        assertEquals(Vector(0, 1, 0, 2), res)

        // Show off the cool CBF / functor stuff.  Vector because 3 to 6 is an IndexedSeq.
        assertEquals("scala.collection.immutable.Vector", res.getClass.getCanonicalName)
    }

    @Test def testDensifyMap() {
        val res = densifyMap(_3to6, _map, 0)
        assertEquals(Vector(0, 1, 0, 2), res)

        // Show off the cool CBF / functor stuff.  Vector because 3 to 6 is an IndexedSeq.
        assertEquals("scala.collection.immutable.Vector", res.getClass.getCanonicalName)
    }
}
