package com.eharmony.aloha.models.tree.decision

import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{Ignore, Test}
import org.junit.Assert._


import com.eharmony.aloha.semantics.func.{GeneratedAccessor, GenFunc}
import com.eharmony.aloha.util.rand.HashedCategoricalDistribution

@RunWith(classOf[BlockJUnit4ClassRunner])
class DecisionTreeBasicTest {
    import Classes._

    private lazy val t1 = getTree1

    @Test def _100_100_is_even_square() {
        val x = t1.getNode(Map("v1" -> 100, "v2" -> 100))
        assertEquals(Right(EvenSquare), value(x))
    }

    @Test def _1_1_is_odd_square() {
        val x = t1.getNode(Map("v1" -> 1, "v2" -> 1))
        assertEquals(Right(OddSquare), value(x))
    }

    @Test def _2_1_is_() {
        val x = t1.getNode(Map("v1" -> 2, "v2" -> 1))
        assertEquals(Right(Other), value(x))
    }

    @Test def _121_121_is_odd_square() {
        val x = t1.getNode(Map("v1" -> 121, "v2" -> 121))
        assertEquals(Right(OddSquare), value(x))
    }

    @Test def _131_131_is_prime() {
        val x = t1.getNode(Map("v1" -> 131, "v2" -> 131))
        assertEquals(Right(Prime), value(x))
    }

    @Test def _131_14_is_other() {
        val x = t1.getNode(Map("v1" -> 131, "v2" -> 14))
        assertEquals(Right(Other), value(x))
    }

    @Test def MISSING_MISSING_causes_error() {
        val x = t1.getNode(Map.empty)
        assertTrue("Should not have been possible to get to leaf node.", x.isLeft)
    }

    @Test def _131_MISSING_causes_error() {
        val x = t1.getNode(Map("v1" -> 131))
        assertTrue("Should not have been possible to get to leaf node.", x.isLeft)
    }

    /** Test that randomness seems to work.
      */
    @Ignore @Test def testRandom() {}

    def getTree2 = {
        val f = GenFunc.f1(GeneratedAccessor("v", (_:Map[String, Int]).get("v")))("identity", identity)
        val root = Node(0.0,
            IndexedSeq(Node(0.625), Node(2.5)),
            RandomNodeSelector(Seq(f), HashedCategoricalDistribution(0.8, 0.2)))

        root
    }

    private[this] def value(v: Either[InteriorNodeResult[Map[String, Long], Classes], Leaf[Classes]]) =
        v.right.map(_.value)

    /**
      *
      *    square ------- odd ---- OddSquare
      *          \            \
      *           \            --- EvenSquare
      *            \
      *             --- prime ---- Prime
      *                       \
      *                        --- Other
      */
    private[this] def getTree1 = {
        val oddSq = Node(OddSquare)
        val evenSq = Node(EvenSquare)
        val primeNode = Node(Prime)
        val boring = Node(Other)

        val fTrue1 = wrap((_: Any) => true, "true for v1", "v1")
        val fTrue2 = wrap((_: Any) => true, "true for v2", "v2")

        val top = Node(Bad,
            IndexedSeq(oddSq, evenSq),
            LinearNodeSelector(List(wrap(odd, "odd(v2)", "v2"), fTrue2)))

        val bottom = Node(Bad,
            IndexedSeq(primeNode, boring),
            LinearNodeSelector(List(wrap(prime, "prime(v2)", "v2"), fTrue2)))

        val root = Node(Bad,
            IndexedSeq(top, bottom),
            LinearNodeSelector(List(wrap(square, "square(v1)", "v1"), fTrue1)))

        root
    }

    private[this] def wrap(f: Long => Boolean, descriptor: String, key: String) = {
        val g = GenFunc.f1(get(key))(descriptor, _ map f)
        g
    }

    private[this] def get(key: String) = GeneratedAccessor(s"$key", (_:Map[String, Long]).get(key))

    private[this] val odd = (x: Long) => 1 == x % 2

    private[this] val square = (x: Long) => {
        val s = math.sqrt(x).ceil.toInt
        s * s == x
    }

    private[this] val prime = (x: Long) => 1 == primeFactorization(x).size

    // http://www.vogella.com/articles/JavaAlgorithmsPrimeFactorization/article.html
    private[this] def primeFactorization(x: Long) = {
        var n = x
        var factors = List.empty[Long]
        var i = 2
        while(i <= n) {
            while(n % i == 0) {
                factors = i :: factors
                n /= i
            }
            i += 1
        }
        if (n > 1) factors = n :: factors

        factors.reverse.toIndexedSeq
    }
}

object Classes extends Enumeration {
    type Classes = Value
    val EvenSquare, OddSquare, Prime, Other, Bad = Value
}

