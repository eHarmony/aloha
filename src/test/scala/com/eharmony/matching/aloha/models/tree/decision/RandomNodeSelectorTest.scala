package com.eharmony.matching.aloha.models.tree.decision

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import scala.util.hashing.MurmurHash3
import scala.util.{Random, MurmurHash}

@RunWith(classOf[BlockJUnit4ClassRunner])
class RandomNodeSelectorTest {
    @Test def test1() {
        val n = 1000
        val k = 10

        val max = Int.MaxValue.toFloat
        val toF = (n: Int) => math.abs(n) / max

        val f = (n: Int) => Array(Option(n))
        val mh3 = f andThen { a => MurmurHash3.orderedHash(a, MurmurHash3.arraySeed) } andThen { toF }
        val mh = f andThen { MurmurHash arrayHash _ } andThen { toF }

        val mh3s = it(n).map(mh3).toArray.sorted
        val mhs = it(n).map(mh).toArray.sorted
        println(mh3s(n/2))
        println(variance(mh3s))

//        val mh3Dist = dist(it(n), mh3, n, k)
//        val mhDist = dist(it(n), mh, n, k)
//        val uniformK = Array.fill(k)(1.0/k)
//
//        val mhNonRandomUniform = JensenShannonDivergence.withinTolerance(mhDist, uniformK, n)
//        val mh3NonRandomUniform = JensenShannonDivergence.withinTolerance(mh3Dist, uniformK, n)
//
//        val mhNonRandomJsd = JensenShannonDivergence.get(mhDist, uniformK)
//        val mh3NonRandomJsd = JensenShannonDivergence.get(mh3Dist, uniformK)
//
//        val mhRandDist = dist(randIt(n), mh, n, k)
//        val mh3RandDist = dist(randIt(n), mh3, n, k)
//
//        val mhRandomUniform = JensenShannonDivergence.withinTolerance(mhRandDist, uniformK, n)
//        val mh3RandomUniform = JensenShannonDivergence.withinTolerance(mh3RandDist, uniformK, n)
//
//        val mhRandomJsd = JensenShannonDivergence.get(mhRandDist, uniformK)
//        val mh3RandomJsd = JensenShannonDivergence.get(mh3RandDist, uniformK)

        val a = 1
    }

    def variance[Double](data: Traversable[Float]) = {
        val mean = data.sum / data.size
        data.reduceLeft((s, x) => s + (x - mean)*(x - mean)) / (data.size - 1)
    }

    def dist[A](it: Iterator[A], f: A => Float, n: Int, k: Int) =
        it.foldLeft(new Array[Double](k)){ case(a, x) => {a((f(x) * k).toInt) += 1; a} } map { _ / n }

    def it(n: Int) = Iterator.range(0, n)

    def randIt(n: Int) = {
        val r = new Random(0)
        Iterator.fill(n)(r.nextInt())
    }
}
