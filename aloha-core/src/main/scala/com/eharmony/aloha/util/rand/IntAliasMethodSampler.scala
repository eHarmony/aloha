package com.eharmony.aloha.util.rand

case class IntAliasMethodSampler(prob: Seq[Double]) extends AliasMethodSampler {
    protected[this] val (alias, probabilities) = structures(prob)
    val numClasses = alias.size
}
