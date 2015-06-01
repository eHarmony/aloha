package com.eharmony.matching.aloha.feature

import OptionMath.Syntax._

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._
import com.eharmony.matching.aloha.feature.OptionMath.OptMathOps

/** Test that inline math syntax works for Option values.  These should work for Byte, Short, Int, Long, Float, Double
  *
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class OptionMathTest {

    // ========================================================================
    //                               STATIC INT
    // ========================================================================


    @Test def testStaticIntAbsMissing() {
        val a: Option[Int] = None
        assertEquals("Test Option[Int] abs missing", None, OptMathOps.abs(a))
    }

    @Test def testStaticIntAbsPositive() {
        val a = Option(2)
        assertEquals("Test Option[Int] abs positive", a, OptMathOps.abs(a))
    }

    @Test def testStaticIntAbsNegative() {
        val a = Option(-2)
        assertEquals("Test Option[Int] abs negative", Option(2), OptMathOps.abs(a))
    }

    @Test def testStaticIntMinusMissing() {
        val a: Option[Int] = None
        assertEquals("Test Option[Int] unary minus missing", None, OptMathOps.negate(a))
    }

    @Test def testStaticIntMinusPositive() {
        val a = Option(2)
        assertEquals("Test Option[Int] unary minus positive", Option(-2), OptMathOps.negate(a))
    }

    @Test def testStaticIntMinusNegative() {
        val a = Option(-2)
        assertEquals("Test Option[Int] unary minus positive", Option(2), OptMathOps.negate(a))
    }

    @Test def testStaticIntAdditionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] addition missing left argument", None, OptMathOps.plus(a, b))
    }

    @Test def testStaticIntAdditionMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] addition missing right argument", None, OptMathOps.plus(a, b))
    }

    @Test def testStaticIntAddition() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] addition", Option(3), OptMathOps.plus(a, b))
    }

    @Test def testStaticIntSubtractionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction missing left argument", None, OptMathOps.minus(a, b))
    }

    @Test def testStaticIntSubtractionMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] subtraction missing right argument", None, OptMathOps.minus(a, b))
    }

    @Test def testStaticIntSubtraction() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), OptMathOps.minus(a, b))
    }

    @Test def testStaticIntMultiplicationMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] multiplication missing left argument", None, OptMathOps.times(a, b))
    }

    @Test def testStaticIntMultiplicationMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] multiplication missing right argument", None, OptMathOps.times(a, b))
    }

    @Test def testStaticIntMultiplication() {
        val a = Option(2)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(6), OptMathOps.times(a, b))
    }

    @Test def testStaticIntDivisionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] division missing left argument", None, OptMathOps.quot(a, b))
    }

    @Test def testStaticIntDivisionMissingRight() {
        val a = Option(3)
        val b = None
        assertEquals("Test Option[Int] division missing right argument", None, OptMathOps.quot(a, b))
    }

    @Test def testStaticIntDivision_3_2_1() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), OptMathOps.quot(a, b))
    }

    @Test def testStaticIntDivision_6_3_2() {
        val a = Option(6)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(2), OptMathOps.quot(a, b))
    }

    @Test def testStaticIntModMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] mod missing left argument", None, OptMathOps.rem(a, b))
    }

    @Test def testStaticIntModMissingRight() {
        val a = Option(3)
        val b = None
        assertEquals("Test Option[Int] mod missing right argument", None, OptMathOps.rem(a, b))
    }

    @Test def testStaticIntMod_3_2_1() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), OptMathOps.rem(a, b))
    }

    @Test def testStaticIntMod_6_3_0() {
        val a = Option(6)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(0), OptMathOps.rem(a, b))
    }

    @Test def testStaticIntLtMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] less than missing left", None, OptMathOps.lt(a, b))
    }

    @Test def testStaticIntLtMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] less than missing right", None, OptMathOps.lt(a, b))
    }

    @Test def testStaticIntLt_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 < 1", Option(false), OptMathOps.lt(a, b))
    }

    @Test def testStaticIntLt_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 < 2", Option(true), OptMathOps.lt(a, b))
    }

    @Test def testStaticIntLt_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 < 1", Option(false), OptMathOps.lt(a, b))
    }


    @Test def testStaticIntLteMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] less than or equal to missing left", None, OptMathOps.lteq(a, b))
    }

    @Test def testStaticIntLteMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] less than or equal to missing right", None, OptMathOps.lteq(a, b))
    }

    @Test def testStaticIntLte_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 <= 1", Option(true), OptMathOps.lteq(a, b))
    }

    @Test def testStaticIntLte_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 <= 2", Option(true), OptMathOps.lteq(a, b))
    }

    @Test def testStaticIntLte_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 <= 1", Option(false), OptMathOps.lteq(a, b))
    }


    @Test def testStaticIntGtMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] greater than missing left", None, OptMathOps.gt(a, b))
    }

    @Test def testStaticIntGtMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] greater than missing right", None, OptMathOps.gt(a, b))
    }

    @Test def testStaticIntGt_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 > 1", Option(false), OptMathOps.gt(a, b))
    }

    @Test def testStaticIntGt_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 > 2", Option(false), OptMathOps.gt(a, b))
    }

    @Test def testStaticIntGt_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 > 1", Option(true), OptMathOps.gt(a, b))
    }


    @Test def testStaticIntGteMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] greater than or equal to missing left", None, OptMathOps.gteq(a, b))
    }

    @Test def testStaticIntGteMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] greater than or equal to missing right", None, OptMathOps.gteq(a, b))
    }

    @Test def testStaticIntGte_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 >= 1", Option(true), OptMathOps.gteq(a, b))
    }

    @Test def testStaticIntGte_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 >= 2", Option(false), OptMathOps.gteq(a, b))
    }

    @Test def testStaticIntGte_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 >= 1", Option(true), OptMathOps.gteq(a, b))
    }


    // ========================================================================
    //                              STATIC FLOAT
    // ========================================================================

    @Test def testStaticFloatAbsMissing() {
        val a: Option[Float] = None
        assertEquals("Test Option[Float] abs missing", None, OptMathOps.abs(a))
    }

    @Test def testStaticFloatAbsPositive() {
        val a = Option(2f)
        assertEquals("Test Option[Float] abs positive", a, OptMathOps.abs(a))
    }

    @Test def testStaticFloatAbsNegative() {
        val a = Option(-2f)
        assertEquals("Test Option[Float] abs negative", Option(2f), OptMathOps.abs(a))
    }

    @Test def testStaticFloatMinusMissing() {
        val a: Option[Float] = None
        assertEquals("Test Option[Float] unary minus missing", None, OptMathOps.negate(a))
    }

    @Test def testStaticFloatMinusPositive() {
        val a = Option(2f)
        assertEquals("Test Option[Float] unary minus positive", Option(-2f), OptMathOps.negate(a))
    }

    @Test def testStaticFloatMinusNegative() {
        val a = Option(-2f)
        assertEquals("Test Option[Float] unary minus positive", Option(2f), OptMathOps.negate(a))
    }

    @Test def testStaticFloatAdditionMissingLeft() {
        val a = None
        val b = Option(2f)
        assertEquals("Test Option[Float] addition missing left argument", None, OptMathOps.plus(a, b))
    }

    @Test def testStaticFloatAdditionMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] addition missing right argument", None, OptMathOps.plus(a, b))
    }

    @Test def testStaticFloatAddition() {
        val a = Option(1f)
        val b = Option(2f)
        assertEquals("Test Option[Float] addition", Option(3f), OptMathOps.plus(a, b))
    }

    @Test def testStaticFloatSubtractionMissingLeft() {
        val a = None
        val b = Option(2f)
        assertEquals("Test Option[Float] subtraction missing left argument", None, OptMathOps.minus(a, b))
    }

    @Test def testStaticFloatSubtractionMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] subtraction missing right argument", None, OptMathOps.minus(a, b))
    }

    @Test def testStaticFloatSubtraction() {
        val a = Option(3f)
        val b = Option(2f)
        assertEquals("Test Option[Float] subtraction", Option(1f), OptMathOps.minus(a, b))
    }

    @Test def testStaticFloatMultiplicationMissingLeft() {
        val a = None
        val b = Option(2f)
        assertEquals("Test Option[Float] multiplication missing left argument", None, OptMathOps.times(a, b))
    }

    @Test def testStaticFloatMultiplicationMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] multiplication missing right argument", None, OptMathOps.times(a, b))
    }

    @Test def testStaticFloatMultiplication() {
        val a = Option(2f)
        val b = Option(3f)
        assertEquals("Test Option[Float] subtraction", Option(6f), OptMathOps.times(a, b))
    }

    @Test def testStaticFloatDivisionMissingLeft() {
        val a = None
        val b = Option(2f)
        assertEquals("Test Option[Float] division missing left argument", None, OptMathOps.div(a, b))
    }

    @Test def testStaticFloatDivisionMissingRight() {
        val a = Option(3f)
        val b = None
        assertEquals("Test Option[Float] division missing right argument", None, OptMathOps.div(a, b))
    }

    @Test def testStaticFloatDivision_3f_2f_1p5f() {
        val a = Option(3f)
        val b = Option(2f)
        assertEquals("Test Option[Float] subtraction", Option(1.5f), OptMathOps.div(a, b))
    }

    @Test def testStaticFloatDivision_6f_3f_2f() {
        val a = Option(6f)
        val b = Option(3f)
        assertEquals("Test Option[Float] subtraction", Option(2f), OptMathOps.div(a, b))
    }

    @Test def testStaticFloatLtMissingLeft() {
        val a = None
        val b = Option(1f)
        assertEquals("Test Option[Float] less than missing left", None, OptMathOps.lt(a, b))
    }

    @Test def testStaticFloatLtMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] less than missing right", None, OptMathOps.lt(a, b))
    }

    @Test def testStaticFloatLt_1f_1f() {
        val a = Option(1f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 1f < 1f", Option(false), OptMathOps.lt(a, b))
    }

    @Test def testStaticFloatLt_1f_2f() {
        val a = Option(1f)
        val b = Option(2f)
        assertEquals("Test Option[Float] 1f < 2f", Option(true), OptMathOps.lt(a, b))
    }

    @Test def testStaticFloatLt_2f_1f() {
        val a = Option(2f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 2f < 1f", Option(false), OptMathOps.lt(a, b))
    }


    @Test def testStaticFloatLteMissingLeft() {
        val a = None
        val b = Option(1f)
        assertEquals("Test Option[Float] less than or equal to missing left", None, OptMathOps.lteq(a, b))
    }

    @Test def testStaticFloatLteMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] less than or equal to missing right", None, OptMathOps.lteq(a, b))
    }

    @Test def testStaticFloatLte_1f_1f() {
        val a = Option(1f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 1f <= 1f", Option(true), OptMathOps.lteq(a, b))
    }

    @Test def testStaticFloatLte_1f_2f() {
        val a = Option(1f)
        val b = Option(2f)
        assertEquals("Test Option[Float] 1f <= 2f", Option(true), OptMathOps.lteq(a, b))
    }

    @Test def testStaticFloatLte_2f_1f() {
        val a = Option(2f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 2f <= 1f", Option(false), OptMathOps.lteq(a, b))
    }


    @Test def testStaticFloatGtMissingLeft() {
        val a = None
        val b = Option(1f)
        assertEquals("Test Option[Float] greater than missing left", None, OptMathOps.gt(a, b))
    }

    @Test def testStaticFloatGtMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] greater than missing right", None, OptMathOps.gt(a, b))
    }

    @Test def testStaticFloatGt_1f_1f() {
        val a = Option(1f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 1f > 1f", Option(false), OptMathOps.gt(a, b))
    }

    @Test def testStaticFloatGt_1f_2f() {
        val a = Option(1f)
        val b = Option(2f)
        assertEquals("Test Option[Float] 1f > 2f", Option(false), OptMathOps.gt(a, b))
    }

    @Test def testStaticFloatGt_2f_1f() {
        val a = Option(2f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 2f > 1f", Option(true), OptMathOps.gt(a, b))
    }


    @Test def testStaticFloatGteMissingLeft() {
        val a = None
        val b = Option(1f)
        assertEquals("Test Option[Float] greater than or equal to missing left", None, OptMathOps.gteq(a, b))
    }

    @Test def testStaticFloatGteMissingRight() {
        val a = Option(1f)
        val b = None
        assertEquals("Test Option[Float] greater than or equal to missing right", None, OptMathOps.gteq(a, b))
    }

    @Test def testStaticFloatGte_1f_1f() {
        val a = Option(1f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 1f >= 1f", Option(true), OptMathOps.gteq(a, b))
    }

    @Test def testStaticFloatGte_1f_2f() {
        val a = Option(1f)
        val b = Option(2f)
        assertEquals("Test Option[Float] 1f >= 2f", Option(false), OptMathOps.gteq(a, b))
    }

    @Test def testStaticFloatGte_2f_1f() {
        val a = Option(2f)
        val b = Option(1f)
        assertEquals("Test Option[Float] 2f >= 1f", Option(true), OptMathOps.gteq(a, b))
    }


    // ========================================================================
    //                                  INT
    // ========================================================================

    @Test def testIntAbsMissing() {
        val a: Option[Int] = None
        assertEquals("Test Option[Int] abs missing", None, a.abs())
    }

    @Test def testIntAbsPositive() {
        val a = Option(2)
        assertEquals("Test Option[Int] abs positive", a, a.abs())
    }

    @Test def testIntAbsNegative() {
        val a = Option(-2)
        assertEquals("Test Option[Int] abs negative", Option(2), a.abs())
    }

    @Test def testIntMinusMissing() {
        val a: Option[Int] = None
        assertEquals("Test Option[Int] unary minus missing", None, -a)
    }

    @Test def testIntMinusPositive() {
        val a = Option(2)
        assertEquals("Test Option[Int] unary minus positive", Option(-2), -a)
    }

    @Test def testIntMinusNegative() {
        val a = Option(-2)
        assertEquals("Test Option[Int] unary minus positive", Option(2), -a)
    }

    @Test def testIntAdditionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] addition missing left argument", None, a + b)
    }

    @Test def testIntAdditionMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] addition missing right argument", None, a + b)
    }

    @Test def testIntAddition() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] addition", Option(3), a + b)
    }

    @Test def testIntSubtractionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction missing left argument", None, a - b)
    }

    @Test def testIntSubtractionMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] subtraction missing right argument", None, a - b)
    }

    @Test def testIntSubtraction() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), a - b)
    }

    @Test def testIntMultiplicationMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] multiplication missing left argument", None, a * b)
    }

    @Test def testIntMultiplicationMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] multiplication missing right argument", None, a * b)
    }

    @Test def testIntMultiplication() {
        val a = Option(2)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(6), a * b)
    }

    @Test def testIntDivisionMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] division missing left argument", None, a / b)
    }

    @Test def testIntDivisionMissingRight() {
        val a = Option(3)
        val b = None
        assertEquals("Test Option[Int] division missing right argument", None, a / b)
    }

    @Test def testIntDivision_3_2_1() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), a / b)
    }

    @Test def testIntDivision_6_3_2() {
        val a = Option(6)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(2), a / b)
    }

    @Test def testIntModMissingLeft() {
        val a = None
        val b = Option(2)
        assertEquals("Test Option[Int] mod missing left argument", None, a % b)
    }

    @Test def testIntModMissingRight() {
        val a = Option(3)
        val b = None
        assertEquals("Test Option[Int] mod missing right argument", None, a % b)
    }

    @Test def testIntMod_3_2_1() {
        val a = Option(3)
        val b = Option(2)
        assertEquals("Test Option[Int] subtraction", Option(1), a % b)
    }

    @Test def testIntMod_6_3_0() {
        val a = Option(6)
        val b = Option(3)
        assertEquals("Test Option[Int] subtraction", Option(0), a % b)
    }

    @Test def testIntLtMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] less than missing left", None, a < b)
    }

    @Test def testIntLtMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] less than missing right", None, a < b)
    }

    @Test def testIntLt_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 < 1", Option(false), a < b)
    }

    @Test def testIntLt_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 < 2", Option(true), a < b)
    }

    @Test def testIntLt_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 < 1", Option(false), a < b)
    }


    @Test def testIntLteMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] less than or equal to missing left", None, a <= b)
    }

    @Test def testIntLteMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] less than or equal to missing right", None, a <= b)
    }

    @Test def testIntLte_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 <= 1", Option(true), a <= b)
    }

    @Test def testIntLte_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 <= 2", Option(true), a <= b)
    }

    @Test def testIntLte_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 <= 1", Option(false), a <= b)
    }


    @Test def testIntGtMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] greater than missing left", None, a > b)
    }

    @Test def testIntGtMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] greater than missing right", None, a > b)
    }

    @Test def testIntGt_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 > 1", Option(false), a > b)
    }

    @Test def testIntGt_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 > 2", Option(false), a > b)
    }

    @Test def testIntGt_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 > 1", Option(true), a > b)
    }


    @Test def testIntGteMissingLeft() {
        val a = None
        val b = Option(1)
        assertEquals("Test Option[Int] greater than or equal to missing left", None, a >= b)
    }

    @Test def testIntGteMissingRight() {
        val a = Option(1)
        val b = None
        assertEquals("Test Option[Int] greater than or equal to missing right", None, a >= b)
    }

    @Test def testIntGte_1_1() {
        val a = Option(1)
        val b = Option(1)
        assertEquals("Test Option[Int] 1 >= 1", Option(true), a >= b)
    }

    @Test def testIntGte_1_2() {
        val a = Option(1)
        val b = Option(2)
        assertEquals("Test Option[Int] 1 >= 2", Option(false), a >= b)
    }

    @Test def testIntGte_2_1() {
        val a = Option(2)
        val b = Option(1)
        assertEquals("Test Option[Int] 2 >= 1", Option(true), a >= b)
    }


    // ========================================================================
    //                                 FLOAT
    // ========================================================================

    @Test def testFloatAbsMissing() {
        val a: Option[Float] = None
        assertEquals("Test Option[Float] abs missing", None, a.abs())
    }

    @Test def testFloatAbsPositive() {
        val a = Option(2.0f)
        assertEquals("Test Option[Float] abs positive", a, a.abs())
    }

    @Test def testFloatAbsNegative() {
        val a = Option(-2.0f)
        assertEquals("Test Option[Float] abs negative", Option(2.0f), a.abs())
    }

    @Test def testFloatMinusMissing() {
        val a: Option[Float] = None
        assertEquals("Test Option[Float] unary minus missing", None, -a)
    }

    @Test def testFloatMinusPositive() {
        val a = Option(2.0f)
        assertEquals("Test Option[Float] unary minus positive", Option(-2.0f), -a)
    }

    @Test def testFloatMinusNegative() {
        val a = Option(-2.0f)
        assertEquals("Test Option[Float] unary minus positive", Option(2.0f), -a)
    }

    @Test def testFloatAdditionMissingLeft() {
        val a = None
        val b = Option(2.0f)
        assertEquals("Test Option[Float] addition missing left argument", None, a + b)
    }

    @Test def testFloatAdditionMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] addition missing right argument", None, a + b)
    }

    @Test def testFloatAddition() {
        val a = Option(1.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] addition", Option(3.0f), a + b)
    }

    @Test def testFloatSubtractionMissingLeft() {
        val a = None
        val b = Option(2.0f)
        assertEquals("Test Option[Float] subtraction missing left argument", None, a - b)
    }

    @Test def testFloatSubtractionMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] subtraction missing right argument", None, a - b)
    }

    @Test def testFloatSubtraction() {
        val a = Option(3.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] subtraction", Option(1.0f), a - b)
    }

    @Test def testFloatMultiplicationMissingLeft() {
        val a = None
        val b = Option(2.0f)
        assertEquals("Test Option[Float] multiplication missing left argument", None, a * b)
    }

    @Test def testFloatMultiplicationMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] multiplication missing right argument", None, a * b)
    }

    @Test def testFloatMultiplication() {
        val a = Option(2.0f)
        val b = Option(3.0f)
        assertEquals("Test Option[Float] subtraction", Option(6.0f), a * b)
    }

    @Test def testFloatDivisionMissingLeft() {
        val a = None
        val b = Option(2.0f)
        assertEquals("Test Option[Float] division missing left argument", None, a / b)
    }

    @Test def testFloatDivisionMissingRight() {
        val a = Option(3.0f)
        val b = None
        assertEquals("Test Option[Float] division missing right argument", None, a / b)
    }

    @Test def testFloatDivision_3_2_1p5() {
        val a = Option(3.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] subtraction", Option(1.5f), a / b)
    }

    @Test def testFloatDivision_6_3_2() {
        val a = Option(6.0f)
        val b = Option(3.0f)
        assertEquals("Test Option[Float] subtraction", Option(2.0f), a / b)
    }

    @Test def testFloatLtMissingLeft() {
        val a = None
        val b = Option(1.0f)
        assertEquals("Test Option[Float] less than missing left", None, a < b)
    }

    @Test def testFloatLtMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] less than missing right", None, a < b)
    }

    @Test def testFloatLt_1_1() {
        val a = Option(1.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 1.0f < 1.0f", Option(false), a < b)
    }

    @Test def testFloatLt_1_2() {
        val a = Option(1.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] 1.0f < 2.0f", Option(true), a < b)
    }

    @Test def testFloatLt_2_1() {
        val a = Option(2.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 2.0f < 1.0f", Option(false), a < b)
    }


    @Test def testFloatLteMissingLeft() {
        val a = None
        val b = Option(1.0f)
        assertEquals("Test Option[Float] less than or equal to missing left", None, a <= b)
    }

    @Test def testFloatLteMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] less than or equal to missing right", None, a <= b)
    }

    @Test def testFloatLte_1_1() {
        val a = Option(1.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 1.0f <= 1.0f", Option(true), a <= b)
    }

    @Test def testFloatLte_1_2() {
        val a = Option(1.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] 1.0f <= 2.0f", Option(true), a <= b)
    }

    @Test def testFloatLte_2_1() {
        val a = Option(2.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] subtraction", Option(false), a <= b)
    }


    @Test def testFloatGtMissingLeft() {
        val a = None
        val b = Option(1.0f)
        assertEquals("Test Option[Float] greater than missing left", None, a > b)
    }

    @Test def testFloatGtMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] greater than missing right", None, a > b)
    }

    @Test def testFloatGt_1_1() {
        val a = Option(1.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 1.0f > 1.0f", Option(false), a > b)
    }

    @Test def testFloatGt_1_2() {
        val a = Option(1.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] 1.0f > 2.0f", Option(false), a > b)
    }

    @Test def testFloatGt_2_1() {
        val a = Option(2.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 2.0f > 1.0f", Option(true), a > b)
    }


    @Test def testFloatGteMissingLeft() {
        val a = None
        val b = Option(1.0f)
        assertEquals("Test Option[Float] greater than or equal to missing left", None, a >= b)
    }

    @Test def testFloatGteMissingRight() {
        val a = Option(1.0f)
        val b = None
        assertEquals("Test Option[Float] greater than or equal to missing right", None, a >= b)
    }

    @Test def testFloatGte_1_1() {
        val a = Option(1.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 1.0f >= 1.0f", Option(true), a >= b)
    }

    @Test def testFloatGte_1_2() {
        val a = Option(1.0f)
        val b = Option(2.0f)
        assertEquals("Test Option[Float] 1.0f >= 2.0f", Option(false), a >= b)
    }

    @Test def testFloatGte_2_1() {
        val a = Option(2.0f)
        val b = Option(1.0f)
        assertEquals("Test Option[Float] 2.0f >= 1.0f", Option(true), a >= b)
    }
}
