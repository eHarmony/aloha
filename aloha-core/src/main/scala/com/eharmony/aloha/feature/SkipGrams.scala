package com.eharmony.aloha.feature

import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

import com.eharmony.aloha.util.SubSeqIterator
import com.eharmony.aloha.util.hashing.MurmurHash3

import scala.collection.concurrent.TrieMap
import scala.collection.parallel.mutable.ParArray
import scala.collection.{mutable => scm}
import scala.{collection => sc}

/**
 * @author R M Deak
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
trait SkipGrams {
    import SkipGramsHelp.defaultSplitter

    def skipGrams(str: String,
                  n: Int,
                  k: Int = 0,
                  sep: String = "_",
                  prefix: String = "=",
                  suffix: String = "",
                  splitString: String = """\s+""",
                  maxElements: Option[Int] = None): sc.Map[String, Int] = {

        val splitter = if (splitString == defaultSplitter.pattern) defaultSplitter
                       else Pattern.compile(splitString)
        val tokens = splitter.split(str)
        val len = tokens.length
        val m = scm.Map.empty[String, Int]
        var i = 0
        while (i < len) {
            val endExcl = math.min(len, i + n + k)
            if (endExcl - i >= n) {
                val range = i + 1 until math.min(len, i + n + k)
                val it = SubSeqIterator(range, n - 1)
                while(it.hasNext) {
                    val gram = new StringBuilder().append(prefix).append(tokens(i))
                    val j = it.next().iterator
                    while (j.hasNext) {
                        gram.append(sep).append(tokens(j.next()))
                    }
                    val g = gram.append(suffix).toString()
                    m.update(g, m.getOrElse(g, 0) + 1)
                }
            }
            i += 1
        }
        maxElements.fold[sc.Map[String, Int]](m)(minHash(_, m))
    }

//    def skipGrams(str: String, n: Int, k: Int = 0, prefix: String = "=", sep: String = "_", suffix: String = ""):
//    sc.Map[String, Int] = {
//        val tokens = str.split("""\s+""")
//        val len = tokens.length
//        val ind = 0 to len - n
//        val m = scm.Map.empty[String, Int]
//        ind.foreach { i =>
//            val endExcl = math.min(len, i + n + k)
//            if (endExcl - i >= n) {
//                val range = i + 1 until math.min(len, i + n + k)
//                SubSeqIterator(range, n - 1).foreach { subseq =>
//                    val gram = subseq.foldLeft(prefix + tokens(i))((s, j) => s + sep + tokens(j)) + suffix
//                    val c = m.getOrElseUpdate(gram, 0)
//                    m.update(gram, c + 1)
//                }
//            }
//        }
//        m
//    }

    def nGrams(s: String,
               n: Int,
               sep: String = "_",
               prefix: String = "=",
               suffix: String = "",
               splitString: String = """\s+""",
               maxElements: Option[Int] = None): sc.Map[String, Int] =
        skipGrams(s, n, 0, sep, prefix, suffix, splitString, maxElements)

    def bag(str: String,
            prefix: String = "=",
            suffix: String = "",
            splitString: String = """\s+""",
            maxElements: Option[Int] = None): sc.Map[String, Int] = {
        val splitter = if (splitString == defaultSplitter.pattern) defaultSplitter
                       else Pattern.compile(splitString)
        val tokens = splitter.split(str)
        val m = scala.collection.mutable.Map[String, Int]()
        var i = tokens.length - 1
        while (i >= 0) {
            val token = new StringBuilder().append(prefix).append(tokens(i)).append(suffix).toString()
            m.update(token, m.getOrElse(token, 0) + 1)
            i -= 1
        }
        maxElements.fold[sc.Map[String, Int]](m)(minHash(_, m))
    }

//    def bag(s: String): sc.Map[String, Int] = nGrams(s, 1)

    private[this] def minHash(maxElements: Int, tokens: sc.Map[String, Int]) = {
        val hashedValues = tokens.map{ case(nGram, count) => (MurmurHash3.stringHash(nGram), nGram, count)}.toSeq
        hashedValues.sorted.take(maxElements).map{ case(_, nGram, count) => (nGram, count) }.toMap
    }
}

trait ParallelSkipGrams {
    import SkipGramsHelp._

    private[this] def partitioningIndices(n: Int, k: Int) = {
        if (n < k) Vector((0, n))
        else {
            val starts = 0 until n by n / k
            starts zip starts.tail :+ n
        }
    }

    def skipGrams(str: String,
                  n: Int,
                  k: Int = 0,
                  sep: String = "_",
                  prefix: String = "=",
                  suffix: String = "",
                  splitString: String = """\s+""",
                  maxElements: Option[Int] = None): sc.Map[String, AtomicInteger]= {

        val splitter = if (splitString == defaultSplitter.pattern) defaultSplitter
                       else Pattern.compile(splitString)
        val tokens = splitter.split(str)
        val len = tokens.length
        val m = TrieMap.empty[String, AtomicInteger]
        partitioningIndices(len, processors).par.foreach { case (b, e) =>
            var i = b
            while (i < e) {
                val endExcl = math.min(len, i + n + k)
                if (endExcl - i >= n) {
                    val range = i + 1 until math.min(len, i + n + k)
                    val it = SubSeqIterator(range, n - 1)
                    while(it.hasNext) {
                        val gram = new StringBuilder().append(prefix).append(tokens(i))
                        val j = it.next().iterator
                        while (j.hasNext) {
                            gram.append(sep).append(tokens(j.next()))
                        }
                        gram.append(suffix)
                        val c = m.getOrElseUpdate(gram.toString(), new AtomicInteger(0))
                        c.incrementAndGet()
                    }
                }
                i += 1
            }
        }
        maxElements.fold[sc.Map[String, AtomicInteger]](m)(parMinHash(_, m))
    }


//    def skipGrams(str: String, n: Int, k: Int = 0, prefix: String = "=", sep: String = "_", suffix: String = ""):
//    sc.Map[String, AtomicInteger] = {
//        val tokens = str.split("""\s+""")
//        val len = tokens.length
//        val ind = (0 to len - n).par
//        val m = TrieMap.empty[String, AtomicInteger]
//        ind.foreach { i =>
//            val endExcl = math.min(len, i + n + k)
//            if (endExcl - i >= n) {
//                val range = i + 1 until math.min(len, i + n + k)
//                SubSeqIterator(range, n - 1).foreach { subseq =>
//                    val gram = subseq.foldLeft(prefix + tokens(i))((s, j) => s + sep + tokens(j)) + suffix
//                    val c = m.getOrElseUpdate(gram, new AtomicInteger(0))
//                    c.incrementAndGet()
//                }
//            }
//        }
//        m
//    }

    //    def nGrams(s: String, n: Int): sc.Map[String, AtomicInteger] = skipGrams(s, n, 0)
    //    def bag(s: String): sc.Map[String, AtomicInteger] = nGrams(s, 1)

    def nGrams(s: String,
               n: Int,
               sep: String = "_",
               prefix: String = "=",
               suffix: String = "",
               splitString: String = """\s+""",
               maxElements: Option[Int] = None): sc.Map[String, AtomicInteger] =
        skipGrams(s, n, 0, sep, prefix, suffix, splitString, maxElements)

    def bag(str: String,
            prefix: String = "=",
            suffix: String = "",
            splitString: String = """\s+""",
            maxElements: Option[Int] = None): sc.Map[String, AtomicInteger] = {
        val splitter = if (splitString == defaultSplitter.pattern) defaultSplitter
                       else Pattern.compile(splitString)
        val m = TrieMap.empty[String, AtomicInteger]
        ParArray.handoff(splitter.split(str)).foreach { token =>
            val t = new StringBuilder().append(prefix).append(token).append(suffix).toString()
            val c = m.getOrElseUpdate(t, new AtomicInteger(0))
            c.incrementAndGet()
        }
        maxElements.fold[sc.Map[String, AtomicInteger]](m)(parMinHash(_, m))
    }

    private[this] def parMinHash(maxElements: Int, tokens: TrieMap[String, AtomicInteger]) = {
        val hashedValues = tokens.par.map{ case(nGram, count) => (MurmurHash3.stringHash(nGram), nGram, count)}.toSeq.seq
        hashedValues.sortBy(_._1).take(maxElements).map{ case(_, nGram, count) => (nGram, count) }.toMap
    }
}

object SkipGrams extends SkipGrams {
    object par extends ParallelSkipGrams
}

object SkipGramsHelp {
    private[feature] val defaultSplitter = Pattern.compile("""\s+""")
    private[feature] val processors = Runtime.getRuntime.availableProcessors()
}
