package com.eharmony.aloha.util

import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}

/**
 * A fast, thread-safe, memory efficient k-subsequence iterator.
 *
 * See [http://deaktator.github.io/ Ryan Deak]'s post on
 * [http://deaktator.github.io/2015/08/13/k-length-subsequences/ K-length Subsequences] for details.
 * @author R M Deak
 */
object SubSeqIterator {
    private val Masks = 0 to 62 map { i => 1L << i }

    def apply[A](values: IndexedSeq[A], k: Int): Iterator[Seq[A]] =
        if (0 == k) OneIterator[A]()
        else if (k > values.size) Iterator.empty
        else new SubSeqIterator(values, k)

    case class OneIterator[A]() extends Iterator[Seq[A]] {
        val more = new AtomicBoolean(true)
        def hasNext: Boolean = more.get
        def next(): Seq[A] =
            if (more.getAndSet(false)) Seq[A]()
            else throw new NoSuchElementException
    }

    case class SubSeqIterator[A](values: IndexedSeq[A], k: Int)
        extends Iterator[Seq[A]] {
        require(values.size <= 63)
        require(k <= values.size)

        private[this] val current = new AtomicLong(smallest(k))
        private[this] val last = largest(values.size, k)
        private[this] val numValues = values.size

        def hasNext: Boolean = current.get <= last

        def next(): IndexedSeq[A] = {
            var c = current.get
            var n = 0L
            if (c <= last) {
                n = next(c)
                while (c <= last && !current.compareAndSet(c, n)) {
                    c = current.get
                    n = next(c)
                }
            }
            if (c <= last) subset(c)
            else throw new NoSuchElementException
        }

        protected[this] def subset(n: Long) = {
            var i = 0
            var els = values.companion.empty[A]
            while(i < numValues) {
                if (0 != (Masks(i) & n))
                    els = els :+ values(i)
                i += 1
            }
            els
        }

        protected[this] def largest(n: Int, k: Int): Long = smallest(k) << (n - k)
        protected[this] def smallest(n: Int): Long = (1L << n) - 1
        protected[this] def next(x: Long): Long = {
            if (x == 0) 0
            else {
                val smallest = x & -x
                val ripple = x + smallest
                val newSmallest = ripple & -ripple
                val ones = ((newSmallest / smallest) >> 1) - 1
                ripple | ones
            }
        }
    }
}
