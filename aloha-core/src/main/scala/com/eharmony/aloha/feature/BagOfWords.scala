package com.eharmony.aloha.feature

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author msokolowski
 *
 * Implements k-skip-n-grams e.g
 *
 * from http://homepages.inf.ed.ac.uk/ballison/pdf/lrec_skipgrams.pdf (Introduction)
 *
 * "Insurgents killed in ongoing fighting.”
 * Bi-grams = {insurgents killed, killed in, in ongoing, ongoing fighting}.
 * 2-skip-2-grams = {insurgents killed, insurgents in, insurgents ongoing, killed in, killed ongoing, killed fighting, in ongoing, in fighting, ongoing fighting} Tri-grams = {insurgents killed in, killed in ongoing, in ongoing fighting}.
 * 2-skip-3-grams = {insurgents killed in, insurgents killed ongoing, insurgents killed fighting, insurgents in ongoing, insurgents in fighting, insurgents ongoing fighting, killed in ongoing, killed in fighting, killed ongoing fighting, in ongoing fighting}.
 */
trait BagOfWords {
  def skipGrams(s: String, n: Int, k: Int): Iterable[(String, Double)] = {
    val orig = s.split("""\s+""").sliding(n + k).foldLeft(Iterable.empty[(String, Double)])((acc, l) => acc ++ l.combinations(n).map(e => "=" + e.mkString("_") -> 1.0).toMap)
//    val sliding = s.split("""\s+""").
//                 sliding(n + k).toVector
//    val orig = sliding.foldLeft(Iterable.empty[(String, Double)]){(acc, l) =>
//      val h = l.head
//      val out = acc ++ CombinationsIterator(l.tail, n-1).map(e => "=" + (h +: e).mkString("_") -> 1.0).toMap
//      println(out)
//      out
//    }
    val grouped = orig.groupBy(_._1).mapValues(_.size.toDouble)
    grouped
  }
  def nGrams(s: String, n: Int): Iterable[(String, Double)] = {
    skipGrams(s, n, 0)
  }
  def bag(s: String): Iterable[(String, Double)] = {
    nGrams(s, 1)
  }
}

object BagOfWords extends BagOfWords


/**
 * This is threadsafe
 *
 * Based on answer on stack overflow answer to
 * [http://stackoverflow.com/a/506862 Creating multiple numbers with certain number of bits set]
 * by Nils Pipenbrinck.
 */
object CombinationsIterator {
  private val Masks = 0 to 62 map { i => 1L << i }

  def apply[A](values: IndexedSeq[A], k: Int): Iterator[Seq[A]] = {
    if (0 == k) new OneIterator[A]
    else new CombinationsIterator(values, k)
  }

  private[this] class OneIterator[A] extends Iterator[Seq[A]] {
    val more = new AtomicBoolean(true)
    def hasNext = more.get
    def next() = {
      val m = more.getAndSet(false)
      if (m) Seq[A]()
      else throw new NoSuchElementException
    }
  }

  private[this] class CombinationsIterator[A](values: IndexedSeq[A], k: Int) extends Iterator[Seq[A]] {
    require(values.size <= 63)
    require(k <= values.size)

    private[this] val current = new AtomicLong(smallest(k))
    private[this] val last = largest(values.size, k)
    private[this] val highestBitOfLast = {
      val b = java.lang.Long.highestOneBit(last)
      Masks.indexWhere(_ == b)
    }
    private[this] val numValues = values.size

    def hasNext = current.get <= last

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
      while(i <= highestBitOfLast) {
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
