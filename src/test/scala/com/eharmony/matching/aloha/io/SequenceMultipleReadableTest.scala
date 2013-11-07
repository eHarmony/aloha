package com.eharmony.matching.aloha.io

import scala.collection.JavaConversions.{asJavaCollection, collectionAsScalaIterable}
import scala.collection.immutable
import scala.util.Try

import scalaz.Functor

import java.{util => ju}
import ju.concurrent.atomic.AtomicReference

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import com.eharmony.matching.aloha.io.sources.ReadableSource
import com.eharmony.matching.aloha.io.sources.ReadableSourceConverters.stringToStringReadableConverter
import com.eharmony.matching.aloha.io.multiple.{SequenceMultipleReadable, MultipleAlohaReadable}

import scalaz.std.list.listInstance                      // Provides Traverse type class for List
import scalaz.std.indexedSeq.indexedSeqInstance          // Provides Traverse type class for IndexedSeq
import scalaz.std.stream.streamInstance                  // Provides (non-strict) Traverse type class for Stream
import com.eharmony.matching.aloha.util.TryScalazMonad   // Provides Applicative type class for Try


@RunWith(classOf[BlockJUnit4ClassRunner])
class SequenceMultipleReadableTest {
    import SequenceMultipleReadableTest._

    @Test def testGoodUnSequencedList() {
        val r = new StringToIntListSequencedMultipleReadable
        val tries: List[ContainerType[InnerType]] = r.fromMultipleSources(GoodGoodGoodReadableList)

        // Determine if there are any failures.  If so, succeeded is false; otherwise, true.
        val succeeded = tries.aggregate(true)(_ && _.isSuccess, _ && _)
        assertTrue(succeeded)

        val successes: List[InnerType] = tries.map(_.get)
        assertEquals(GoodGoodGood, successes)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }

    @Test def testGoodUnSequencedJavaCollection() {
        val r = new StringToIntListSequencedMultipleReadable
        val tries: ju.Collection[ContainerType[InnerType]] = r.fromMultipleSources(GoodGoodGoodReadableJavaCollection)

        // Determine if there are any failures.  If so, succeeded is false; otherwise, true.
        val succeeded = tries.aggregate(true)(_ && _.isSuccess, _ && _)
        assertTrue(succeeded)

        val successes: Iterable[InnerType] = tries.map(_.get)
        assertEquals(GoodGoodGood, successes)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }


    @Test def testGoodUnSequencedImplicitIndexedSeq() {
        val r = new StringToIntListSequencedMultipleReadable
        val tries = r.fromMultipleSources(GoodGoodGoodReadableIndexedSeq)

        // Determine if there are any failures.  If so, succeeded is false; otherwise, true.
        val succeeded = tries.aggregate(true)(_ && _.isSuccess, _ && _)
        assertTrue(succeeded)

        val successes: immutable.IndexedSeq[InnerType] = tries.map(_.get)
        assertEquals(GoodGoodGood, successes)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }

    @Test def testGoodUnSequencedExplicitIndexedSeq() {
        val r = new StringToIntListSequencedMultipleReadable
        val functor = implicitly[Functor[immutable.IndexedSeq]]

        val tries = r.fromMultipleSources(GoodGoodGoodReadableIndexedSeq)(functor)

        // Determine if there are any failures.  If so, succeeded is false; otherwise, true.
        val succeeded = tries.aggregate(true)(_ && _.isSuccess, _ && _)
        assertTrue(succeeded)

        val successes: immutable.IndexedSeq[InnerType] = tries.map(_.get)
        assertEquals(GoodGoodGood, successes)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }

    @Test def testGoodSequencedList() {
        val r = new StringToIntListSequencedMultipleReadable

        val actTry = r.fromSequencedMultipleSources(GoodGoodGoodReadableList)
        assertTrue(actTry.isSuccess)

        val act = actTry.get
        assertEquals(GoodGoodGood, act)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }

    @Test def testGoodSequencedIndexedSeq() {
        val r = new StringToIntListSequencedMultipleReadable

        val actTry = r.fromSequencedMultipleSources(GoodGoodGoodReadableIndexedSeq)
        assertTrue(actTry.isSuccess)

        val act = actTry.get
        assertEquals(GoodGoodGood, act)

        // Process all on success
        assertEquals(GoodGoodGood.size, r.numFromStringCalls())
    }

    @Test def testBadSequencedList() {
        val r = new StringToIntListSequencedMultipleReadable

        val actTry: ContainerType[List[InnerType]] = r.fromSequencedMultipleSources(GoodBadGoodReadableList)
        assertTrue(actTry.isFailure)

        // strict
        assertEquals(GoodBadGoodReadableList.size, r.numFromStringCalls())

        assertTrue(actTry.failed.get.isInstanceOf[NumberFormatException])
        assertEquals(GoodBadGoodExceptionMsg, actTry.failed.get.getMessage)
    }

    @Test def testBadSequencedIndexedSeq() {
        val r = new StringToIntListSequencedMultipleReadable

        val actTry: ContainerType[immutable.IndexedSeq[InnerType]] = r.fromSequencedMultipleSources(GoodBadGoodReadableIndexedSeq)
        assertTrue(actTry.isFailure)

        // strict
        assertEquals(GoodBadGoodReadableIndexedSeq.size, r.numFromStringCalls())

        assertTrue(actTry.failed.get.isInstanceOf[NumberFormatException])
        assertEquals(GoodBadGoodExceptionMsg, actTry.failed.get.getMessage)
    }

    /** Test that calling fromSequencedMultipleSources on a container type (Stream) that has a non-strict traverse
      * algorithm actually causes to fromSequencedMultipleSources operate in the a non-strict way.
      */
    @Test def testBadSequencedStream() {
        val r = new StringToIntListSequencedMultipleReadable

        val actTry: ContainerType[Stream[InnerType]] = r.fromSequencedMultipleSources(GoodBadGoodReadableStream)
        assertTrue(actTry.isFailure)

        // NON-STRICT!
        assertEquals(GoodBadGoodFailingIndex + 1, r.numFromStringCalls())

        assertTrue(actTry.failed.get.isInstanceOf[NumberFormatException])
        assertEquals(GoodBadGoodExceptionMsg, actTry.failed.get.getMessage)
    }
}

private object SequenceMultipleReadableTest {
    type InnerType = List[Int]
    type ContainerType[+A] = Try[A]

    class StringToIntListSequencedMultipleReadable
        extends ReadableByString[ContainerType[InnerType]]
        with MultipleAlohaReadable[ContainerType[InnerType]]
        with SequenceMultipleReadable[ReadableSource, ContainerType, InnerType] {

        private[this] val inputs = new AtomicReference[List[String]](Nil)

        /**
          * @return the number of times fromString is called.  This should equal the number of success if no failures
          *         occur or the number of successes before the first failure plus one if failures exist.
          */
        def numFromStringCalls() = inputs.get().size

        /** Get the inputs into the calls to fromString in order.
          * @return
          */
        def inputsToFromString() = inputs.get().reverse

        /** Attempt to atomically insert s into the list of inputs processed by fromString.
          * @param s a string passed to fromString that we insert into the inputs atomic reference.  If the atomic
          *          compare-and-swap fails, throw an IllegalStateException.
          */
        @throws[IllegalStateException]("If parameter s couldn't be inserted into inputs atomic reference on the first CAS attempt.")
        private[this] def addToInputs(s: String) {
            val ins = inputs.get
            if (!inputs.compareAndSet(ins, s"'$s'" :: ins)) {
                throw new IllegalStateException(s"Couldn't insert '$s' into list ${ins.reverse}")
            }
        }

        def fromString(s: String): ContainerType[InnerType] = Try {
            addToInputs(s)

            val st = s.trim
            if (st == "")
                List.empty
            else st.split("""\s*,\s*""", -1).map(_.toInt).toList
        }
    }

    val GoodGoodGood = (1 to 3).tails.take(3).toList
    val GoodGoodGoodReadableList = GoodGoodGood.map(x => stringToStringReadableConverter(x.mkString(",")))
    val GoodGoodGoodReadableIndexedSeq = GoodGoodGoodReadableList.toIndexedSeq
    val GoodGoodGoodReadableJavaCollection: ju.Collection[ReadableSource] = GoodGoodGoodReadableList

    /** An example that fails in the middle so that we can test strict and non-strict traverse operations.
      *
      *                                     fail on index 1
      *                                            |
      *                               index 0      |    index 2
      *                                  |         |      |
      *                                  V         v      v
      */
    val GoodBadGoodStringList = List("1,2,3", "2, Blah", "3")

    val GoodBadGoodExceptionMsg = """For input string: "Blah""""

    /** The index into GoodBadGoodStringList on which fromString should fail.
      */
    val GoodBadGoodFailingIndex = 1
    val GoodBadGoodReadableList = GoodBadGoodStringList.map(stringToStringReadableConverter)
    val GoodBadGoodReadableIndexedSeq = GoodBadGoodReadableList.toIndexedSeq
    val GoodBadGoodReadableStream = GoodBadGoodReadableList.toStream
}
