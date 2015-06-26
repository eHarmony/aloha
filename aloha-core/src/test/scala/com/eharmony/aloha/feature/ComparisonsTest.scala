package com.eharmony.aloha.feature

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

@RunWith(classOf[BlockJUnit4ClassRunner])
class ComparisonsTest {
    import ComparisonsTest._

    @Test def test_gtLt_1_1(): Unit = assertFalse(gtLt2(1, 1))
    @Test def test_gtLt_1_2(): Unit = assertFalse(gtLt2(1, 2))
    @Test def test_gtLt_1_3(): Unit = assertTrue(gtLt2(1, 3))
    @Test def test_gtLt_2_1(): Unit = assertFalse(gtLt2(2, 1))
    @Test def test_gtLt_2_2(): Unit = assertFalse(gtLt2(2, 2))
    @Test def test_gtLt_2_3(): Unit = assertFalse(gtLt2(2, 3))
    @Test def test_gtLt_3_1(): Unit = assertFalse(gtLt2(3, 1))
    @Test def test_gtLt_3_2(): Unit = assertFalse(gtLt2(3, 2))
    @Test def test_gtLt_3_3(): Unit = assertFalse(gtLt2(3, 3))

    @Test def test_gtLte_1_1(): Unit = assertFalse(gtLte2(1, 1))
    @Test def test_gtLte_1_2(): Unit = assertTrue(gtLte2(1, 2))
    @Test def test_gtLte_1_3(): Unit = assertTrue(gtLte2(1, 3))
    @Test def test_gtLte_2_1(): Unit = assertFalse(gtLte2(2, 1))
    @Test def test_gtLte_2_2(): Unit = assertFalse(gtLte2(2, 2))
    @Test def test_gtLte_2_3(): Unit = assertFalse(gtLte2(2, 3))
    @Test def test_gtLte_3_1(): Unit = assertFalse(gtLte2(3, 1))
    @Test def test_gtLte_3_2(): Unit = assertFalse(gtLte2(3, 2))
    @Test def test_gtLte_3_3(): Unit = assertFalse(gtLte2(3, 3))

    @Test def test_gteLt_1_1(): Unit = assertFalse(gteLt2(1, 1))
    @Test def test_gteLt_1_2(): Unit = assertFalse(gteLt2(1, 2))
    @Test def test_gteLt_1_3(): Unit = assertTrue(gteLt2(1, 3))
    @Test def test_gteLt_2_1(): Unit = assertFalse(gteLt2(2, 1))
    @Test def test_gteLt_2_2(): Unit = assertFalse(gteLt2(2, 2))
    @Test def test_gteLt_2_3(): Unit = assertTrue(gteLt2(2, 3))
    @Test def test_gteLt_3_1(): Unit = assertFalse(gteLt2(3, 1))
    @Test def test_gteLt_3_2(): Unit = assertFalse(gteLt2(3, 2))
    @Test def test_gteLt_3_3(): Unit = assertFalse(gteLt2(3, 3))

    @Test def test_gteLte_1_1(): Unit = assertFalse(gteLte2(1, 1))
    @Test def test_gteLte_1_2(): Unit = assertTrue(gteLte2(1, 2))
    @Test def test_gteLte_1_3(): Unit = assertTrue(gteLte2(1, 3))
    @Test def test_gteLte_2_1(): Unit = assertFalse(gteLte2(2, 1))
    @Test def test_gteLte_2_2(): Unit = assertTrue(gteLte2(2, 2))
    @Test def test_gteLte_2_3(): Unit = assertTrue(gteLte2(2, 3))
    @Test def test_gteLte_3_1(): Unit = assertFalse(gteLte2(3, 1))
    @Test def test_gteLte_3_2(): Unit = assertFalse(gteLte2(3, 2))
    @Test def test_gteLte_3_3(): Unit = assertFalse(gteLte2(3, 3))
}


object ComparisonsTest {
    import Comparisons._
    val gtLt2: (Int, Int) => Boolean = gtLt(2, _, _)
    val gtLte2: (Int, Int) => Boolean = gtLe(2, _, _)
    val gteLt2: (Int, Int) => Boolean = geLt(2, _, _)
    val gteLte2: (Int, Int) => Boolean = geLe(2, _, _)
}
