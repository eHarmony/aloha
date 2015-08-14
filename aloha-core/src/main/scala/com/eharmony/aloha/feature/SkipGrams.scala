package com.eharmony.aloha.feature

import java.util.concurrent.atomic.AtomicInteger

import com.eharmony.aloha.util.SubSeqIterator

import scala.collection.concurrent.TrieMap
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
    def skipGrams(str: String, n: Int, k: Int = 0, prefix: String = "=", sep: String = "_", suffix: String = ""):
    sc.Map[String, Int] = {
        val tokens = str.split("""\s+""")
        val len = tokens.length
        val ind = 0 to len - n
        val m = scm.Map.empty[String, Int]
        ind.foreach { i =>
            val endExcl = math.min(len, i + n + k)
            if (endExcl - i >= n) {
                val range = i + 1 until math.min(len, i + n + k)
                SubSeqIterator(range, n - 1).foreach { subseq =>
                    val gram = subseq.foldLeft(prefix + tokens(i))((s, j) => s + sep + tokens(j)) + suffix
                    val c = m.getOrElseUpdate(gram, 0)
                    m.update(gram, c + 1)
                }
            }
        }
        m
    }

    def nGrams(s: String, n: Int): sc.Map[String, Int] = skipGrams(s, n, 0)
    def bag(s: String): sc.Map[String, Int] = nGrams(s, 1)
}

trait ParallelSkipGrams {
    def skipGrams(str: String, n: Int, k: Int = 0, prefix: String = "=", sep: String = "_", suffix: String = ""):
    sc.Map[String, AtomicInteger] = {
        val tokens = str.split("""\s+""")
        val len = tokens.length
        val ind = (0 to len - n).par
        val m = TrieMap.empty[String, AtomicInteger]
        ind.foreach { i =>
            val endExcl = math.min(len, i + n + k)
            if (endExcl - i >= n) {
                val range = i + 1 until math.min(len, i + n + k)
                SubSeqIterator(range, n - 1).foreach { subseq =>
                    val gram = subseq.foldLeft(prefix + tokens(i))((s, j) => s + sep + tokens(j)) + suffix
                    val c = m.getOrElseUpdate(gram, new AtomicInteger(0))
                    c.incrementAndGet()
                }
            }
        }
        m
    }

    def nGrams(s: String, n: Int): sc.Map[String, AtomicInteger] = skipGrams(s, n, 0)
    def bag(s: String): sc.Map[String, AtomicInteger] = nGrams(s, 1)
}

object SkipGrams extends SkipGrams {
    object par extends ParallelSkipGrams
}
