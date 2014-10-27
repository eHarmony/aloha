package com.eharmony.matching.aloha.feature

import scala.collection.mutable.ListBuffer

/**
 * @author msokolowski
 *
 * Implements k-skip-n-grams e.g
 *
 * from http://homepages.inf.ed.ac.uk/ballison/pdf/lrec_skipgrams.pdf
 *
 * "Insurgents killed in ongoing fighting.”
 * Bi-grams = {insurgents killed, killed in, in ongoing, ongoing fighting}.
 * 2-skip-2-grams = {insurgents killed, insurgents in, insurgents ongoing, killed in, killed ongoing, killed fighting, in ongoing, in fighting, ongoing fighting} Tri-grams = {insurgents killed in, killed in ongoing, in ongoing fighting}.
 * 2-skip-3-grams = {insurgents killed in, insurgents killed ongoing, insurgents killed fighting, insurgents in ongoing, insurgents in fighting, insurgents ongoing fighting, killed in ongoing, killed in fighting, killed ongoing fighting, in ongoing fighting}.
 */
trait nGrams {
  def skipGrams(s: String, n: Int, k: Int): Iterable[(String, Double)] = {
    s.split(" ").sliding(n + k).foldLeft(Iterable.empty[(String, Double)])((acc, l) => acc ++ l.combinations(n).map(e => e.mkString("_") -> 1.0).toMap)
  }
  def nGrams(s: String, n: Int): Iterable[(String, Double)] = {
    skipGrams(s, n, 0)
  }
  def bag(s: String): Iterable[(String, Double)] = {
    nGrams(s, 1)
  }
}

object nGrams extends nGrams