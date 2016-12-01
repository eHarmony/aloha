package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.util.Logging

import scala.util.Random
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.junit.Test

import com.eharmony.aloha.NoEvictionCache
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler

case class Delegate[-A, +B](f: A => B, impl: String, n: Int) extends (A => B) {
    def apply(a: A) = f(a)
    override def toString() = s"Delegate($impl)"
}

class TestCaching extends Logging {
    @Test def test() {
        val n = 50
        val timeout = 5
        val threads = 2
        val (r1, t1) = time(run(TwitterEvalCompiler(), n, timeout, threads))
        debug(s"$t1 seconds.  result: ${r1.foldLeft(0L)(_ + _.n)}. \n${r1.map(_.n).sorted}")
    }

    def run(eval: TwitterEvalCompiler, n: Int, timeout: Int, threads: Int) = {
        implicit val ec = concurrent.ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(threads))
        val cache = new NoEvictionCache
        val r = new Random(0)
        val exp = Seq.fill(n)(r.nextInt(n / 2) + 1).map(i => s"""new com.eharmony.aloha.semantics.compiled.Delegate((_:Any) => $i, "(_:Any) => $i", $i) """)
        val futures = exp.map(s => cache(s)(eval.fromString[Delegate[Any, Int]](s)))
        val oneFuture = Future sequence futures
        val seq = Await.result(oneFuture, timeout.seconds).map(_.get)
        seq
    }


    def time[A](f: => A) = {
        val t1 = System.nanoTime
        val r = f
        val t2 = System.nanoTime
        (r, (1.0e-9*(t2 - t1)).toFloat)
    }
}
