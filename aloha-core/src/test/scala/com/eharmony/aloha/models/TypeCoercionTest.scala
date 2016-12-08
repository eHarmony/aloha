package com.eharmony.aloha.models

import com.eharmony.aloha.util.Logging

import scala.language.existentials

import java.{lang => jl}

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import com.eharmony.aloha.models.TypeCoercionTest.{matrixAndTypes, Precision}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}


/**
 * Test that the proper coercionsexist and that they work.  There are a ton of coercions.
 * At the time of this writing: 557 (3 * 219) coercions.  Here we tests all of the coercions
 * from types not lifted to option.  We also test the that option-based coercions exist.  We
 * don't check the correctness of the option coercions because they are just a mapping of the
 * non-lifted coercions.
 *
 * Notice a lot of the string tests are unstable and throw.  This may mean that we should remove
 * from-string based coercions.
 *
 * @author R M Deak
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class TypeCoercionTest extends Logging {
    val (matrix, types) = matrixAndTypes()

    @Test def test_Bo_Bo_0(): Unit = assertEquals(true, TypeCoercion[Boolean, Boolean].get.apply(true))
    @Test def test_Bo_Bo_1(): Unit = assertEquals(false, TypeCoercion[Boolean, Boolean].get.apply(false))
    @Test def test_Bo_JBo_0(): Unit = assertEquals(true, TypeCoercion[Boolean, jl.Boolean].get.apply(true))
    @Test def test_Bo_JBo_1(): Unit = assertEquals(false, TypeCoercion[Boolean, jl.Boolean].get.apply(false))
    @Test def test_Bo_St_0(): Unit = assertEquals("true", TypeCoercion[Boolean, String].get.apply(true))
    @Test def test_Bo_St_1(): Unit = assertEquals("false", TypeCoercion[Boolean, String].get.apply(false))


    @Test def test_By_By_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, Byte].get.apply(2.toByte))
    @Test def test_By_By_1(): Unit = assertEquals((-3).toByte, TypeCoercion[Byte, Byte].get.apply((-3).toByte))
    @Test def test_By_C_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, Char].get.apply(2.toByte))
    @Test def test_By_C_1(): Unit = assertEquals((-3).toChar, TypeCoercion[Byte, Char].get.apply((-3).toByte))
    @Test def test_By_D_0(): Unit = assertEquals(2d, TypeCoercion[Byte, Double].get.apply(2.toByte), 0)
    @Test def test_By_D_1(): Unit = assertEquals(-3d, TypeCoercion[Byte, Double].get.apply((-3).toByte), 0)
    @Test def test_By_F_0(): Unit = assertEquals(2f, TypeCoercion[Byte, Float].get.apply(2.toByte), 0)
    @Test def test_By_F_1(): Unit = assertEquals(-3f, TypeCoercion[Byte, Float].get.apply((-3).toByte), 0)
    @Test def test_By_I_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, Int].get.apply(2.toByte))
    @Test def test_By_I_1(): Unit = assertEquals((-3).toByte, TypeCoercion[Byte, Int].get.apply((-3).toByte))
    @Test def test_By_JBy_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, jl.Byte].get.apply(2.toByte))
    @Test def test_By_JBy_1(): Unit = assertEquals((-3).toByte, TypeCoercion[Byte, jl.Byte].get.apply((-3).toByte))
    @Test def test_By_JC_0(): Unit = assertEquals(jChar(2), TypeCoercion[Byte, jl.Character].get.apply(2.toByte))
    @Test def test_By_JC_1(): Unit = assertEquals(jChar(65533.toChar), TypeCoercion[Byte, jl.Character].get.apply((-3).toByte)) //underflow
    @Test def test_By_JD_0(): Unit = assertEquals(2d, TypeCoercion[Byte, jl.Double].get.apply(2.toByte), 0)
    @Test def test_By_JD_1(): Unit = assertEquals(-3d, TypeCoercion[Byte, jl.Double].get.apply((-3).toByte), 0)
    @Test def test_By_JF_0(): Unit = assertEquals(2f, TypeCoercion[Byte, jl.Float].get.apply(2.toByte).floatValue(), 0)
    @Test def test_By_JF_1(): Unit = assertEquals(-3f, TypeCoercion[Byte, jl.Float].get.apply((-3).toByte).floatValue(), 0)
    @Test def test_By_JI_0(): Unit = assertEquals(jInt(2), TypeCoercion[Byte, jl.Integer].get.apply(2.toByte))
    @Test def test_By_JI_1(): Unit = assertEquals(jInt(-3), TypeCoercion[Byte, jl.Integer].get.apply((-3).toByte))
    @Test def test_By_JL_0(): Unit = assertEquals(jLong(2), TypeCoercion[Byte, jl.Long].get.apply(2.toByte))
    @Test def test_By_JL_1(): Unit = assertEquals(jLong(-3), TypeCoercion[Byte, jl.Long].get.apply((-3).toByte))
    @Test def test_By_JSh_0(): Unit = assertEquals(jShort(2.toShort), TypeCoercion[Byte, jl.Short].get.apply(2.toByte))
    @Test def test_By_JSh_1(): Unit = assertEquals(jShort(-3.toShort), TypeCoercion[Byte, jl.Short].get.apply((-3).toByte))
    @Test def test_By_L_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, Long].get.apply(2.toByte))
    @Test def test_By_L_1(): Unit = assertEquals((-3).toByte, TypeCoercion[Byte, Long].get.apply((-3).toByte))
    @Test def test_By_Sh_0(): Unit = assertEquals(2.toByte, TypeCoercion[Byte, Short].get.apply(2.toByte))
    @Test def test_By_Sh_1(): Unit = assertEquals((-3).toByte, TypeCoercion[Byte, Short].get.apply((-3).toByte))
    @Test def test_By_St_0(): Unit = assertEquals("2", TypeCoercion[Byte, String].get.apply(2.toByte))
    @Test def test_By_St_1(): Unit = assertEquals("-3", TypeCoercion[Byte, String].get.apply((-3).toByte))


    @Test def test_C_By_0(): Unit = assertEquals('2'.toByte, TypeCoercion[Char, Byte].get.apply('2'))
    @Test def test_C_By_1(): Unit = assertEquals('3'.toByte, TypeCoercion[Char, Byte].get.apply('3'))
    @Test def test_C_C_0(): Unit = assertEquals('2', TypeCoercion[Char, Char].get.apply('2'))
    @Test def test_C_C_1(): Unit = assertEquals('3', TypeCoercion[Char, Char].get.apply('3'))
    @Test def test_C_D_0(): Unit = assertEquals('2'.toDouble, TypeCoercion[Char, Double].get.apply('2'), 0)
    @Test def test_C_D_1(): Unit = assertEquals('3'.toDouble, TypeCoercion[Char, Double].get.apply('3'), 0)
    @Test def test_C_F_0(): Unit = assertEquals('2'.toFloat, TypeCoercion[Char, Float].get.apply('2'), 0)
    @Test def test_C_F_1(): Unit = assertEquals('3'.toFloat, TypeCoercion[Char, Float].get.apply('3'), 0)
    @Test def test_C_I_0(): Unit = assertEquals('2'.toInt, TypeCoercion[Char, Int].get.apply('2'))
    @Test def test_C_I_1(): Unit = assertEquals('3'.toInt, TypeCoercion[Char, Int].get.apply('3'))
    @Test def test_C_JBy_0(): Unit = assertEquals(jByte('2'.toByte), TypeCoercion[Char, jl.Byte].get.apply('2'))
    @Test def test_C_JBy_1(): Unit = assertEquals(jByte('3'.toByte), TypeCoercion[Char, jl.Byte].get.apply('3'))
    @Test def test_C_JC_0(): Unit = assertEquals(jChar('2'), TypeCoercion[Char, jl.Character].get.apply('2'))
    @Test def test_C_JC_1(): Unit = assertEquals(jChar('3'), TypeCoercion[Char, jl.Character].get.apply('3'))
    @Test def test_C_JD_0(): Unit = assertEquals(jDouble('2'.toDouble).doubleValue, TypeCoercion[Char, jl.Double].get.apply('2').doubleValue, 0)
    @Test def test_C_JD_1(): Unit = assertEquals(jDouble('3'.toDouble).doubleValue, TypeCoercion[Char, jl.Double].get.apply('3').doubleValue, 0)
    @Test def test_C_JF_0(): Unit = assertEquals(jFloat('2'.toFloat).floatValue, TypeCoercion[Char, jl.Float].get.apply('2').floatValue, 0)
    @Test def test_C_JF_1(): Unit = assertEquals(jFloat('3'.toFloat).floatValue, TypeCoercion[Char, jl.Float].get.apply('3').floatValue, 0)
    @Test def test_C_JI_0(): Unit = assertEquals(jInt('2'.toInt), TypeCoercion[Char, jl.Integer].get.apply('2'))
    @Test def test_C_JI_1(): Unit = assertEquals(jInt('3'.toInt), TypeCoercion[Char, jl.Integer].get.apply('3'))
    @Test def test_C_JL_0(): Unit = assertEquals(jLong('2'.toLong), TypeCoercion[Char, jl.Long].get.apply('2'))
    @Test def test_C_JL_1(): Unit = assertEquals(jLong('3'.toLong), TypeCoercion[Char, jl.Long].get.apply('3'))
    @Test def test_C_JSh_0(): Unit = assertEquals(jShort('2'.toShort), TypeCoercion[Char, jl.Short].get.apply('2'))
    @Test def test_C_JSh_1(): Unit = assertEquals(jShort('3'.toShort), TypeCoercion[Char, jl.Short].get.apply('3'))
    @Test def test_C_L_0(): Unit = assertEquals('2'.toLong, TypeCoercion[Char, Long].get.apply('2'))
    @Test def test_C_L_1(): Unit = assertEquals('3'.toLong, TypeCoercion[Char, Long].get.apply('3'))
    @Test def test_C_Sh_0(): Unit = assertEquals('2'.toShort, TypeCoercion[Char, Short].get.apply('2'))
    @Test def test_C_Sh_1(): Unit = assertEquals('3'.toShort, TypeCoercion[Char, Short].get.apply('3'))
    @Test def test_C_St_0(): Unit = assertEquals("2", TypeCoercion[Char, String].get.apply('2'))
    @Test def test_C_St_1(): Unit = assertEquals("3", TypeCoercion[Char, String].get.apply('3'))


    @Test def test_D_By_0(): Unit = assertEquals(7.toByte, TypeCoercion[Double, Byte].get.apply(7.5))
    @Test def test_D_By_1(): Unit = assertEquals(-8.toByte, TypeCoercion[Double, Byte].get.apply(-8.5))
    @Test def test_D_C_0(): Unit = assertEquals(7.toChar, TypeCoercion[Double, Char].get.apply(7.5))
    @Test def test_D_C_1(): Unit = assertEquals(-8.toChar, TypeCoercion[Double, Char].get.apply(-8.5))
    @Test def test_D_D_0(): Unit = assertEquals(7.5, TypeCoercion[Double, Double].get.apply(7.5), 0)
    @Test def test_D_D_1(): Unit = assertEquals(-8.5, TypeCoercion[Double, Double].get.apply(-8.5), 0)
    @Test def test_D_F_0(): Unit = assertEquals(7.5f, TypeCoercion[Double, Float].get.apply(7.5), 0)
    @Test def test_D_F_1(): Unit = assertEquals(-8.5f, TypeCoercion[Double, Float].get.apply(-8.5), 0)
    @Test def test_D_I_0(): Unit = assertEquals(7, TypeCoercion[Double, Int].get.apply(7.5))
    @Test def test_D_I_1(): Unit = assertEquals(-8, TypeCoercion[Double, Int].get.apply(-8.5))
    @Test def test_D_JBy_0(): Unit = assertEquals(jByte(7.toByte), TypeCoercion[Double, jl.Byte].get.apply(7.5))
    @Test def test_D_JBy_1(): Unit = assertEquals(jByte(-8.toByte), TypeCoercion[Double, jl.Byte].get.apply(-8.5))
    @Test def test_D_JC_0(): Unit = assertEquals(jChar(7.toChar), TypeCoercion[Double, jl.Character].get.apply(7.5))
    @Test def test_D_JC_1(): Unit = assertEquals(jChar(-8.toChar), TypeCoercion[Double, jl.Character].get.apply(-8.5))
    @Test def test_D_JD_0(): Unit = assertEquals(jDouble(7.5).doubleValue, TypeCoercion[Double, jl.Double].get.apply(7.5).doubleValue, 0)
    @Test def test_D_JD_1(): Unit = assertEquals(jDouble(-8.5).doubleValue, TypeCoercion[Double, jl.Double].get.apply(-8.5).doubleValue, 0)
    @Test def test_D_JF_0(): Unit = assertEquals(jFloat(7.5f).floatValue, TypeCoercion[Double, jl.Float].get.apply(7.5).floatValue, 0)
    @Test def test_D_JF_1(): Unit = assertEquals(jFloat(-8.5f).floatValue, TypeCoercion[Double, jl.Float].get.apply(-8.5).floatValue, 0)
    @Test def test_D_JI_0(): Unit = assertEquals(jInt(7), TypeCoercion[Double, jl.Integer].get.apply(7.5))
    @Test def test_D_JI_1(): Unit = assertEquals(jInt(-8), TypeCoercion[Double, jl.Integer].get.apply(-8.5))
    @Test def test_D_JL_0(): Unit = assertEquals(jLong(7L), TypeCoercion[Double, jl.Long].get.apply(7.5))
    @Test def test_D_JL_1(): Unit = assertEquals(jLong(-8L), TypeCoercion[Double, jl.Long].get.apply(-8.5))
    @Test def test_D_JSh_0(): Unit = assertEquals(jShort(7.toShort), TypeCoercion[Double, jl.Short].get.apply(7.5))
    @Test def test_D_JSh_1(): Unit = assertEquals(jShort(-8.toShort), TypeCoercion[Double, jl.Short].get.apply(-8.5))
    @Test def test_D_L_0(): Unit = assertEquals(7L, TypeCoercion[Double, Long].get.apply(7.5))
    @Test def test_D_L_1(): Unit = assertEquals(-8L, TypeCoercion[Double, Long].get.apply(-8.5))
    @Test def test_D_Sh_0(): Unit = assertEquals(7.toShort, TypeCoercion[Double, Short].get.apply(7.5))
    @Test def test_D_Sh_1(): Unit = assertEquals(-8.toShort, TypeCoercion[Double, Short].get.apply(-8.5))
    @Test def test_D_St_0(): Unit = assertEquals("7.5", TypeCoercion[Double, String].get.apply(7.5))
    @Test def test_D_St_1(): Unit = assertEquals("-8.5", TypeCoercion[Double, String].get.apply(-8.5))


    @Test def test_F_By_0(): Unit = assertEquals(6.toByte, TypeCoercion[Float, Byte].get.apply(6.499f))
    @Test def test_F_By_1(): Unit = assertEquals(-7.toByte, TypeCoercion[Float, Byte].get.apply(-7.499f))
    @Test def test_F_C_0(): Unit = assertEquals(6.toChar, TypeCoercion[Float, Char].get.apply(6.499f))
    @Test def test_F_C_1(): Unit = assertEquals(-7.toChar, TypeCoercion[Float, Char].get.apply(-7.499f))
    @Test def test_F_D_0(): Unit = assertEquals(6.499, TypeCoercion[Float, Double].get.apply(6.499f), Precision)
    @Test def test_F_D_1(): Unit = assertEquals(-7.499, TypeCoercion[Float, Double].get.apply(-7.499f), Precision)
    @Test def test_F_F_0(): Unit = assertEquals(6.499f, TypeCoercion[Float, Float].get.apply(6.499f), 0)
    @Test def test_F_F_1(): Unit = assertEquals(-7.499f, TypeCoercion[Float, Float].get.apply(-7.499f), 0)
    @Test def test_F_I_0(): Unit = assertEquals(6, TypeCoercion[Float, Int].get.apply(6.499f))
    @Test def test_F_I_1(): Unit = assertEquals(-7, TypeCoercion[Float, Int].get.apply(-7.499f))
    @Test def test_F_JBy_0(): Unit = assertEquals(jByte(6.toByte), TypeCoercion[Float, jl.Byte].get.apply(6.499f))
    @Test def test_F_JBy_1(): Unit = assertEquals(jByte(-7.toByte), TypeCoercion[Float, jl.Byte].get.apply(-7.499f))
    @Test def test_F_JC_0(): Unit = assertEquals(jChar(6.toChar), TypeCoercion[Float, jl.Character].get.apply(6.499f))
    @Test def test_F_JC_1(): Unit = assertEquals(jChar(65529.toChar), TypeCoercion[Float, jl.Character].get.apply(-7.499f)) // underflow
    @Test def test_F_JD_0(): Unit = assertEquals(jDouble(6.499).doubleValue, TypeCoercion[Float, jl.Double].get.apply(6.499f).doubleValue, Precision)
    @Test def test_F_JD_1(): Unit = assertEquals(jDouble(-7.499).doubleValue, TypeCoercion[Float, jl.Double].get.apply(-7.499f).doubleValue, Precision)
    @Test def test_F_JF_0(): Unit = assertEquals(jFloat(6.499f).floatValue, TypeCoercion[Float, jl.Float].get.apply(6.499f).floatValue, 0)
    @Test def test_F_JF_1(): Unit = assertEquals(jFloat(-7.499f).floatValue, TypeCoercion[Float, jl.Float].get.apply(-7.499f).floatValue, 0)
    @Test def test_F_JI_0(): Unit = assertEquals(jInt(6), TypeCoercion[Float, jl.Integer].get.apply(6.499f))
    @Test def test_F_JI_1(): Unit = assertEquals(jInt(-7), TypeCoercion[Float, jl.Integer].get.apply(-7.499f))
    @Test def test_F_JL_0(): Unit = assertEquals(jLong(6L), TypeCoercion[Float, jl.Long].get.apply(6.499f))
    @Test def test_F_JL_1(): Unit = assertEquals(jLong(-7L), TypeCoercion[Float, jl.Long].get.apply(-7.499f))
    @Test def test_F_JSh_0(): Unit = assertEquals(jShort(6.toShort), TypeCoercion[Float, jl.Short].get.apply(6.499f))
    @Test def test_F_JSh_1(): Unit = assertEquals(jShort(-7.toShort), TypeCoercion[Float, jl.Short].get.apply(-7.499f))
    @Test def test_F_L_0(): Unit = assertEquals(6.toLong, TypeCoercion[Float, Long].get.apply(6.499f))
    @Test def test_F_L_1(): Unit = assertEquals(-7.toLong, TypeCoercion[Float, Long].get.apply(-7.499f))
    @Test def test_F_Sh_0(): Unit = assertEquals(6.toShort, TypeCoercion[Float, Short].get.apply(6.499f))
    @Test def test_F_Sh_1(): Unit = assertEquals(-7.toShort, TypeCoercion[Float, Short].get.apply(-7.499f))
    @Test def test_F_St_0(): Unit = assertEquals("6.499", TypeCoercion[Float, String].get.apply(6.499f))
    @Test def test_F_St_1(): Unit = assertEquals("-7.499", TypeCoercion[Float, String].get.apply(-7.499f))


    @Test def test_I_By_0(): Unit = assertEquals(4.toByte, TypeCoercion[Int, Byte].get.apply(4))
    @Test def test_I_By_1(): Unit = assertEquals(-5.toByte, TypeCoercion[Int, Byte].get.apply(-5))
    @Test def test_I_C_0(): Unit = assertEquals(4.toChar, TypeCoercion[Int, Char].get.apply(4))
    @Test def test_I_C_1(): Unit = assertEquals(-5.toChar, TypeCoercion[Int, Char].get.apply(-5))
    @Test def test_I_D_0(): Unit = assertEquals(4d, TypeCoercion[Int, Double].get.apply(4), 0)
    @Test def test_I_D_1(): Unit = assertEquals(-5d, TypeCoercion[Int, Double].get.apply(-5), 0)
    @Test def test_I_F_0(): Unit = assertEquals(4f, TypeCoercion[Int, Float].get.apply(4), 0)
    @Test def test_I_F_1(): Unit = assertEquals(-5f, TypeCoercion[Int, Float].get.apply(-5), 0)
    @Test def test_I_I_0(): Unit = assertEquals(4, TypeCoercion[Int, Int].get.apply(4))
    @Test def test_I_I_1(): Unit = assertEquals(-5, TypeCoercion[Int, Int].get.apply(-5))
    @Test def test_I_JBy_0(): Unit = assertEquals(jByte(4.toByte), TypeCoercion[Int, jl.Byte].get.apply(4))
    @Test def test_I_JBy_1(): Unit = assertEquals(jByte(-5.toByte), TypeCoercion[Int, jl.Byte].get.apply(-5))
    @Test def test_I_JC_0(): Unit = assertEquals(jChar(4.toChar), TypeCoercion[Int, jl.Character].get.apply(4))
    @Test def test_I_JC_1(): Unit = assertEquals(jChar(65531.toChar), TypeCoercion[Int, jl.Character].get.apply(-5))
    @Test def test_I_JD_0(): Unit = assertEquals(jDouble(4d).doubleValue, TypeCoercion[Int, jl.Double].get.apply(4).doubleValue, 0)
    @Test def test_I_JD_1(): Unit = assertEquals(jDouble(-5d).doubleValue, TypeCoercion[Int, jl.Double].get.apply(-5).doubleValue, 0)
    @Test def test_I_JF_0(): Unit = assertEquals(jFloat(4f).floatValue, TypeCoercion[Int, jl.Float].get.apply(4).floatValue, 0)
    @Test def test_I_JF_1(): Unit = assertEquals(jFloat(-5f).floatValue, TypeCoercion[Int, jl.Float].get.apply(-5).floatValue, 0)
    @Test def test_I_JI_0(): Unit = assertEquals(jInt(4), TypeCoercion[Int, jl.Integer].get.apply(4))
    @Test def test_I_JI_1(): Unit = assertEquals(jInt(-5), TypeCoercion[Int, jl.Integer].get.apply(-5))
    @Test def test_I_JL_0(): Unit = assertEquals(jLong(4L), TypeCoercion[Int, jl.Long].get.apply(4))
    @Test def test_I_JL_1(): Unit = assertEquals(jLong(-5L), TypeCoercion[Int, jl.Long].get.apply(-5))
    @Test def test_I_JSh_0(): Unit = assertEquals(jShort(4.toShort), TypeCoercion[Int, jl.Short].get.apply(4))
    @Test def test_I_JSh_1(): Unit = assertEquals(jShort(-5.toShort), TypeCoercion[Int, jl.Short].get.apply(-5))
    @Test def test_I_L_0(): Unit = assertEquals(4L, TypeCoercion[Int, Long].get.apply(4))
    @Test def test_I_L_1(): Unit = assertEquals(-5L, TypeCoercion[Int, Long].get.apply(-5))
    @Test def test_I_Sh_0(): Unit = assertEquals(4.toShort, TypeCoercion[Int, Short].get.apply(4))
    @Test def test_I_Sh_1(): Unit = assertEquals(-5.toShort, TypeCoercion[Int, Short].get.apply(-5))
    @Test def test_I_St_0(): Unit = assertEquals("4", TypeCoercion[Int, String].get.apply(4))
    @Test def test_I_St_1(): Unit = assertEquals("-5", TypeCoercion[Int, String].get.apply(-5))


    @Test def test_JBo_Bo_0(): Unit = assertEquals(false, TypeCoercion[jl.Boolean, Boolean].get.apply(jl.Boolean.FALSE))
    @Test def test_JBo_Bo_1(): Unit = assertEquals(true, TypeCoercion[jl.Boolean, Boolean].get.apply(jl.Boolean.TRUE))
    @Test def test_JBo_JBo_0(): Unit = assertEquals(jl.Boolean.FALSE, TypeCoercion[jl.Boolean, jl.Boolean].get.apply(jl.Boolean.FALSE))
    @Test def test_JBo_JBo_1(): Unit = assertEquals(jl.Boolean.TRUE, TypeCoercion[jl.Boolean, jl.Boolean].get.apply(jl.Boolean.TRUE))
    @Test def test_JBo_St_0(): Unit = assertEquals(jl.Boolean.FALSE.toString, TypeCoercion[jl.Boolean, String].get.apply(jl.Boolean.FALSE))
    @Test def test_JBo_St_1(): Unit = assertEquals(jl.Boolean.TRUE.toString, TypeCoercion[jl.Boolean, String].get.apply(jl.Boolean.TRUE))


    @Test def test_JBy_By_0(): Unit = assertEquals(8.toByte, TypeCoercion[jl.Byte, Byte].get.apply(jByte(8)))
    @Test def test_JBy_By_1(): Unit = assertEquals(-9.toByte, TypeCoercion[jl.Byte, Byte].get.apply(jByte(-9)))
    @Test def test_JBy_C_0(): Unit = assertEquals(8.toChar, TypeCoercion[jl.Byte, Char].get.apply(jByte(8)))
    @Test def test_JBy_C_1(): Unit = assertEquals(-9.toChar, TypeCoercion[jl.Byte, Char].get.apply(jByte(-9)))
    @Test def test_JBy_D_0(): Unit = assertEquals(8d, TypeCoercion[jl.Byte, Double].get.apply(jByte(8)), 0)
    @Test def test_JBy_D_1(): Unit = assertEquals(-9d, TypeCoercion[jl.Byte, Double].get.apply(jByte(-9)), 0)
    @Test def test_JBy_F_0(): Unit = assertEquals(8f, TypeCoercion[jl.Byte, Float].get.apply(jByte(8)), 0)
    @Test def test_JBy_F_1(): Unit = assertEquals(-9f, TypeCoercion[jl.Byte, Float].get.apply(jByte(-9)), 0)
    @Test def test_JBy_I_0(): Unit = assertEquals(8, TypeCoercion[jl.Byte, Int].get.apply(jByte(8)))
    @Test def test_JBy_I_1(): Unit = assertEquals(-9, TypeCoercion[jl.Byte, Int].get.apply(jByte(-9)))
    @Test def test_JBy_JBy_0(): Unit = assertEquals(jByte(8), TypeCoercion[jl.Byte, jl.Byte].get.apply(jByte(8)))
    @Test def test_JBy_JBy_1(): Unit = assertEquals(jByte(-9), TypeCoercion[jl.Byte, jl.Byte].get.apply(jByte(-9)))
    @Test def test_JBy_JC_0(): Unit = assertEquals(jChar(8), TypeCoercion[jl.Byte, jl.Character].get.apply(jByte(8)))
    @Test def test_JBy_JC_1(): Unit = assertEquals(jChar(-9), TypeCoercion[jl.Byte, jl.Character].get.apply(jByte(-9)))
    @Test def test_JBy_JD_0(): Unit = assertEquals(jDouble(8).doubleValue, TypeCoercion[jl.Byte, jl.Double].get.apply(jByte(8)).doubleValue, 0)
    @Test def test_JBy_JD_1(): Unit = assertEquals(jDouble(-9).doubleValue, TypeCoercion[jl.Byte, jl.Double].get.apply(jByte(-9)).doubleValue, 0)
    @Test def test_JBy_JF_0(): Unit = assertEquals(jFloat(8).floatValue, TypeCoercion[jl.Byte, jl.Float].get.apply(jByte(8)).floatValue, 0)
    @Test def test_JBy_JF_1(): Unit = assertEquals(jByte(-9).floatValue, TypeCoercion[jl.Byte, jl.Float].get.apply(jByte(-9)).floatValue, 0)
    @Test def test_JBy_JI_0(): Unit = assertEquals(jInt(8), TypeCoercion[jl.Byte, jl.Integer].get.apply(jByte(8)))
    @Test def test_JBy_JI_1(): Unit = assertEquals(jInt(-9), TypeCoercion[jl.Byte, jl.Integer].get.apply(jByte(-9)))
    @Test def test_JBy_JL_0(): Unit = assertEquals(jLong(8), TypeCoercion[jl.Byte, jl.Long].get.apply(jByte(8)))
    @Test def test_JBy_JL_1(): Unit = assertEquals(jLong(-9), TypeCoercion[jl.Byte, jl.Long].get.apply(jByte(-9)))
    @Test def test_JBy_JSh_0(): Unit = assertEquals(jShort(8), TypeCoercion[jl.Byte, jl.Short].get.apply(jByte(8)))
    @Test def test_JBy_JSh_1(): Unit = assertEquals(jShort(-9), TypeCoercion[jl.Byte, jl.Short].get.apply(jByte(-9)))
    @Test def test_JBy_L_0(): Unit = assertEquals(8L, TypeCoercion[jl.Byte, Long].get.apply(jByte(8)))
    @Test def test_JBy_L_1(): Unit = assertEquals(-9L, TypeCoercion[jl.Byte, Long].get.apply(jByte(-9)))
    @Test def test_JBy_Sh_0(): Unit = assertEquals(8.toShort, TypeCoercion[jl.Byte, Short].get.apply(jByte(8)))
    @Test def test_JBy_Sh_1(): Unit = assertEquals(-9.toShort, TypeCoercion[jl.Byte, Short].get.apply(jByte(-9)))
    @Test def test_JBy_St_0(): Unit = assertEquals("8", TypeCoercion[jl.Byte, String].get.apply(jByte(8)))
    @Test def test_JBy_St_1(): Unit = assertEquals("-9", TypeCoercion[jl.Byte, String].get.apply(jByte(-9)))


    @Test def test_JC_C_0(): Unit = assertEquals('4', TypeCoercion[jl.Character, Char].get.apply(jChar('4')))
    @Test def test_JC_C_1(): Unit = assertEquals('5', TypeCoercion[jl.Character, Char].get.apply(jChar('5')))
    @Test def test_JC_JC_0(): Unit = assertEquals(jChar('4'), TypeCoercion[jl.Character, jl.Character].get.apply(jChar('4')))
    @Test def test_JC_JC_1(): Unit = assertEquals(jChar('5'), TypeCoercion[jl.Character, jl.Character].get.apply(jChar('5')))
    @Test def test_JC_St_0(): Unit = assertEquals("4", TypeCoercion[jl.Character, String].get.apply(jChar('4')))
    @Test def test_JC_St_1(): Unit = assertEquals("5", TypeCoercion[jl.Character, String].get.apply(jChar('5')))


    @Test def test_JD_By_0(): Unit = assertEquals(jByte(13), TypeCoercion[jl.Double, Byte].get.apply(jDouble(13.5)))
    @Test def test_JD_By_1(): Unit = assertEquals(jByte(-14), TypeCoercion[jl.Double, Byte].get.apply(jDouble(-14.5)))
    @Test def test_JD_C_0(): Unit = assertEquals(13.toChar, TypeCoercion[jl.Double, Char].get.apply(jDouble(13.5)))
    @Test def test_JD_C_1(): Unit = assertEquals((-14).toChar, TypeCoercion[jl.Double, Char].get.apply(jDouble(-14.5)))
    @Test def test_JD_D_0(): Unit = assertEquals(13.5, TypeCoercion[jl.Double, Double].get.apply(jDouble(13.5)), 0)
    @Test def test_JD_D_1(): Unit = assertEquals(-14.5, TypeCoercion[jl.Double, Double].get.apply(jDouble(-14.5)), 0)
    @Test def test_JD_F_0(): Unit = assertEquals(13.5f, TypeCoercion[jl.Double, Float].get.apply(jDouble(13.5)), 0)
    @Test def test_JD_F_1(): Unit = assertEquals(-14.5f, TypeCoercion[jl.Double, Float].get.apply(jDouble(-14.5)), 0)
    @Test def test_JD_I_0(): Unit = assertEquals(13, TypeCoercion[jl.Double, Int].get.apply(jDouble(13.5)))
    @Test def test_JD_I_1(): Unit = assertEquals(-14, TypeCoercion[jl.Double, Int].get.apply(jDouble(-14.5)))
    @Test def test_JD_JBy_0(): Unit = assertEquals(jByte(13), TypeCoercion[jl.Double, jl.Byte].get.apply(jDouble(13.5)))
    @Test def test_JD_JBy_1(): Unit = assertEquals(jByte(-14), TypeCoercion[jl.Double, jl.Byte].get.apply(jDouble(-14.5)))
    @Test def test_JD_JC_0(): Unit = assertEquals(jChar(13), TypeCoercion[jl.Double, jl.Character].get.apply(jDouble(13.5)))
    @Test def test_JD_JC_1(): Unit = assertEquals(jChar(-14), TypeCoercion[jl.Double, jl.Character].get.apply(jDouble(-14.5)))
    @Test def test_JD_JD_0(): Unit = assertEquals(jDouble(13.5), TypeCoercion[jl.Double, jl.Double].get.apply(jDouble(13.5)))
    @Test def test_JD_JD_1(): Unit = assertEquals(jDouble(-14.5), TypeCoercion[jl.Double, jl.Double].get.apply(jDouble(-14.5)))
    @Test def test_JD_JF_0(): Unit = assertEquals(jFloat(13.5f), TypeCoercion[jl.Double, jl.Float].get.apply(jDouble(13.5)))
    @Test def test_JD_JF_1(): Unit = assertEquals(jFloat(-14.5f), TypeCoercion[jl.Double, jl.Float].get.apply(jDouble(-14.5)))
    @Test def test_JD_JI_0(): Unit = assertEquals(jInt(13), TypeCoercion[jl.Double, jl.Integer].get.apply(jDouble(13.5)))
    @Test def test_JD_JI_1(): Unit = assertEquals(jInt(-14), TypeCoercion[jl.Double, jl.Integer].get.apply(jDouble(-14.5)))
    @Test def test_JD_JL_0(): Unit = assertEquals(jLong(13), TypeCoercion[jl.Double, jl.Long].get.apply(jDouble(13.5)))
    @Test def test_JD_JL_1(): Unit = assertEquals(jLong(-14), TypeCoercion[jl.Double, jl.Long].get.apply(jDouble(-14.5)))
    @Test def test_JD_JSh_0(): Unit = assertEquals(jShort(13), TypeCoercion[jl.Double, jl.Short].get.apply(jDouble(13.5)))
    @Test def test_JD_JSh_1(): Unit = assertEquals(jShort(-14), TypeCoercion[jl.Double, jl.Short].get.apply(jDouble(-14.5)))
    @Test def test_JD_L_0(): Unit = assertEquals(13L, TypeCoercion[jl.Double, Long].get.apply(jDouble(13.5)))
    @Test def test_JD_L_1(): Unit = assertEquals(-14L, TypeCoercion[jl.Double, Long].get.apply(jDouble(-14.5)))
    @Test def test_JD_Sh_0(): Unit = assertEquals(jShort(13), TypeCoercion[jl.Double, Short].get.apply(jDouble(13.5)))
    @Test def test_JD_Sh_1(): Unit = assertEquals(jShort(-14), TypeCoercion[jl.Double, Short].get.apply(jDouble(-14.5)))
    @Test def test_JD_St_0(): Unit = assertEquals("13.5", TypeCoercion[jl.Double, String].get.apply(jDouble(13.5)))
    @Test def test_JD_St_1(): Unit = assertEquals("-14.5", TypeCoercion[jl.Double, String].get.apply(jDouble(-14.5)))


    @Test def test_JF_By_0(): Unit = assertEquals(12.toByte, TypeCoercion[jl.Float, Byte].get.apply(jFloat(12.499f)))
    @Test def test_JF_By_1(): Unit = assertEquals((-13).toByte, TypeCoercion[jl.Float, Byte].get.apply(jFloat(-13.499f)))
    @Test def test_JF_C_0(): Unit = assertEquals(12.toChar, TypeCoercion[jl.Float, Char].get.apply(jFloat(12.499f)))
    @Test def test_JF_C_1(): Unit = assertEquals((-13).toChar, TypeCoercion[jl.Float, Char].get.apply(jFloat(-13.499f)))
    @Test def test_JF_D_0(): Unit = assertEquals(12.499d, TypeCoercion[jl.Float, Double].get.apply(jFloat(12.499f)), Precision)
    @Test def test_JF_D_1(): Unit = assertEquals(-13.499d, TypeCoercion[jl.Float, Double].get.apply(jFloat(-13.499f)), Precision)
    @Test def test_JF_F_0(): Unit = assertEquals(12.499f, TypeCoercion[jl.Float, Float].get.apply(jFloat(12.499f)), 0)
    @Test def test_JF_F_1(): Unit = assertEquals(-13.499f, TypeCoercion[jl.Float, Float].get.apply(jFloat(-13.499f)), 0)
    @Test def test_JF_I_0(): Unit = assertEquals(12, TypeCoercion[jl.Float, Int].get.apply(jFloat(12.499f)))
    @Test def test_JF_I_1(): Unit = assertEquals(-13, TypeCoercion[jl.Float, Int].get.apply(jFloat(-13.499f)))
    @Test def test_JF_JBy_0(): Unit = assertEquals(jByte(12), TypeCoercion[jl.Float, jl.Byte].get.apply(jFloat(12.499f)))
    @Test def test_JF_JBy_1(): Unit = assertEquals(jByte(-13), TypeCoercion[jl.Float, jl.Byte].get.apply(jFloat(-13.499f)))
    @Test def test_JF_JC_0(): Unit = assertEquals(jChar(12), TypeCoercion[jl.Float, jl.Character].get.apply(jFloat(12.499f)))
    @Test def test_JF_JC_1(): Unit = assertEquals(jChar(-13), TypeCoercion[jl.Float, jl.Character].get.apply(jFloat(-13.499f)))
    @Test def test_JF_JD_0(): Unit = assertEquals(jDouble(12.499).doubleValue, TypeCoercion[jl.Float, jl.Double].get.apply(jFloat(12.499f)).doubleValue, Precision)
    @Test def test_JF_JD_1(): Unit = assertEquals(jDouble(-13.499).doubleValue, TypeCoercion[jl.Float, jl.Double].get.apply(jFloat(-13.499f)).doubleValue, Precision)
    @Test def test_JF_JF_0(): Unit = assertEquals(jFloat(12.499f).floatValue, TypeCoercion[jl.Float, jl.Float].get.apply(jFloat(12.499f)).floatValue, 0)
    @Test def test_JF_JF_1(): Unit = assertEquals(jFloat(-13.499f).floatValue, TypeCoercion[jl.Float, jl.Float].get.apply(jFloat(-13.499f)).floatValue, 0)
    @Test def test_JF_JI_0(): Unit = assertEquals(jInt(12), TypeCoercion[jl.Float, jl.Integer].get.apply(jFloat(12.499f)))
    @Test def test_JF_JI_1(): Unit = assertEquals(jInt(-13), TypeCoercion[jl.Float, jl.Integer].get.apply(jFloat(-13.499f)))
    @Test def test_JF_JL_0(): Unit = assertEquals(jLong(12L), TypeCoercion[jl.Float, jl.Long].get.apply(jFloat(12.499f)))
    @Test def test_JF_JL_1(): Unit = assertEquals(jLong(-13L), TypeCoercion[jl.Float, jl.Long].get.apply(jFloat(-13.499f)))
    @Test def test_JF_JSh_0(): Unit = assertEquals(jShort(12), TypeCoercion[jl.Float, jl.Short].get.apply(jFloat(12.499f)))
    @Test def test_JF_JSh_1(): Unit = assertEquals(jShort(-13), TypeCoercion[jl.Float, jl.Short].get.apply(jFloat(-13.499f)))
    @Test def test_JF_L_0(): Unit = assertEquals(12L, TypeCoercion[jl.Float, Long].get.apply(jFloat(12.499f)))
    @Test def test_JF_L_1(): Unit = assertEquals(-13L, TypeCoercion[jl.Float, Long].get.apply(jFloat(-13.499f)))
    @Test def test_JF_Sh_0(): Unit = assertEquals(12.toShort, TypeCoercion[jl.Float, Short].get.apply(jFloat(12.499f)))
    @Test def test_JF_Sh_1(): Unit = assertEquals((-13).toShort, TypeCoercion[jl.Float, Short].get.apply(jFloat(-13.499f)))
    @Test def test_JF_St_0(): Unit = assertEquals("12.499", TypeCoercion[jl.Float, String].get.apply(jFloat(12.499f)))
    @Test def test_JF_St_1(): Unit = assertEquals("-13.499", TypeCoercion[jl.Float, String].get.apply(jFloat(-13.499f)))


    @Test def test_JI_By_0(): Unit = assertEquals(10.toByte, TypeCoercion[jl.Integer, Byte].get.apply(jInt(10)))
    @Test def test_JI_By_1(): Unit = assertEquals((-11).toByte, TypeCoercion[jl.Integer, Byte].get.apply(jInt(-11)))
    @Test def test_JI_C_0(): Unit = assertEquals(10.toChar, TypeCoercion[jl.Integer, Char].get.apply(jInt(10)))
    @Test def test_JI_C_1(): Unit = assertEquals((-11).toChar, TypeCoercion[jl.Integer, Char].get.apply(jInt(-11)))
    @Test def test_JI_D_0(): Unit = assertEquals(10d, TypeCoercion[jl.Integer, Double].get.apply(jInt(10)), 0)
    @Test def test_JI_D_1(): Unit = assertEquals(-11d, TypeCoercion[jl.Integer, Double].get.apply(jInt(-11)), 0)
    @Test def test_JI_F_0(): Unit = assertEquals(10f, TypeCoercion[jl.Integer, Float].get.apply(jInt(10)), 0)
    @Test def test_JI_F_1(): Unit = assertEquals(-11f, TypeCoercion[jl.Integer, Float].get.apply(jInt(-11)), 0)
    @Test def test_JI_I_0(): Unit = assertEquals(10, TypeCoercion[jl.Integer, Int].get.apply(jInt(10)))
    @Test def test_JI_I_1(): Unit = assertEquals(-11, TypeCoercion[jl.Integer, Int].get.apply(jInt(-11)))
    @Test def test_JI_JBy_0(): Unit = assertEquals(jByte(10), TypeCoercion[jl.Integer, jl.Byte].get.apply(jInt(10)))
    @Test def test_JI_JBy_1(): Unit = assertEquals(jByte(-11), TypeCoercion[jl.Integer, jl.Byte].get.apply(jInt(-11)))
    @Test def test_JI_JC_0(): Unit = assertEquals(jChar(10), TypeCoercion[jl.Integer, jl.Character].get.apply(jInt(10)))
    @Test def test_JI_JC_1(): Unit = assertEquals(jChar(-11), TypeCoercion[jl.Integer, jl.Character].get.apply(jInt(-11)))
    @Test def test_JI_JD_0(): Unit = assertEquals(jDouble(10).doubleValue, TypeCoercion[jl.Integer, jl.Double].get.apply(jInt(10)).doubleValue, 0)
    @Test def test_JI_JD_1(): Unit = assertEquals(jDouble(-11).doubleValue, TypeCoercion[jl.Integer, jl.Double].get.apply(jInt(-11)).doubleValue, 0)
    @Test def test_JI_JF_0(): Unit = assertEquals(jFloat(10).floatValue, TypeCoercion[jl.Integer, jl.Float].get.apply(jInt(10)).floatValue, 0)
    @Test def test_JI_JF_1(): Unit = assertEquals(jFloat(-11).floatValue, TypeCoercion[jl.Integer, jl.Float].get.apply(jInt(-11)).floatValue, 0)
    @Test def test_JI_JI_0(): Unit = assertEquals(jInt(10), TypeCoercion[jl.Integer, jl.Integer].get.apply(jInt(10)))
    @Test def test_JI_JI_1(): Unit = assertEquals(jInt(-11), TypeCoercion[jl.Integer, jl.Integer].get.apply(jInt(-11)))
    @Test def test_JI_JL_0(): Unit = assertEquals(jLong(10), TypeCoercion[jl.Integer, jl.Long].get.apply(jInt(10)))
    @Test def test_JI_JL_1(): Unit = assertEquals(jLong(-11), TypeCoercion[jl.Integer, jl.Long].get.apply(jInt(-11)))
    @Test def test_JI_JSh_0(): Unit = assertEquals(jShort(10), TypeCoercion[jl.Integer, jl.Short].get.apply(jInt(10)))
    @Test def test_JI_JSh_1(): Unit = assertEquals(jShort(-11), TypeCoercion[jl.Integer, jl.Short].get.apply(jInt(-11)))
    @Test def test_JI_L_0(): Unit = assertEquals(10L, TypeCoercion[jl.Integer, Long].get.apply(jInt(10)))
    @Test def test_JI_L_1(): Unit = assertEquals(-11L, TypeCoercion[jl.Integer, Long].get.apply(jInt(-11)))
    @Test def test_JI_Sh_0(): Unit = assertEquals(10.toShort, TypeCoercion[jl.Integer, Short].get.apply(jInt(10)))
    @Test def test_JI_Sh_1(): Unit = assertEquals(-11.toShort, TypeCoercion[jl.Integer, Short].get.apply(jInt(-11)))
    @Test def test_JI_St_0(): Unit = assertEquals("10", TypeCoercion[jl.Integer, String].get.apply(jInt(10)))
    @Test def test_JI_St_1(): Unit = assertEquals("-11", TypeCoercion[jl.Integer, String].get.apply(jInt(-11)))


    @Test def test_JL_By_0(): Unit = assertEquals(11.toByte, TypeCoercion[jl.Long, Byte].get.apply(jLong(11)))
    @Test def test_JL_By_1(): Unit = assertEquals((-12).toByte, TypeCoercion[jl.Long, Byte].get.apply(jLong(-12)))
    @Test def test_JL_C_0(): Unit = assertEquals(11.toChar, TypeCoercion[jl.Long, Char].get.apply(jLong(11)))
    @Test def test_JL_C_1(): Unit = assertEquals((-12).toChar, TypeCoercion[jl.Long, Char].get.apply(jLong(-12)))
    @Test def test_JL_D_0(): Unit = assertEquals(11d, TypeCoercion[jl.Long, Double].get.apply(jLong(11)), 0)
    @Test def test_JL_D_1(): Unit = assertEquals(-12d, TypeCoercion[jl.Long, Double].get.apply(jLong(-12)), 0)
    @Test def test_JL_F_0(): Unit = assertEquals(11f, TypeCoercion[jl.Long, Float].get.apply(jLong(11)), 0)
    @Test def test_JL_F_1(): Unit = assertEquals(-12f, TypeCoercion[jl.Long, Float].get.apply(jLong(-12)), 0)
    @Test def test_JL_I_0(): Unit = assertEquals(11, TypeCoercion[jl.Long, Int].get.apply(jLong(11)))
    @Test def test_JL_I_1(): Unit = assertEquals(-12, TypeCoercion[jl.Long, Int].get.apply(jLong(-12)))
    @Test def test_JL_JBy_0(): Unit = assertEquals(jByte(11), TypeCoercion[jl.Long, jl.Byte].get.apply(jLong(11)))
    @Test def test_JL_JBy_1(): Unit = assertEquals(jByte(-12), TypeCoercion[jl.Long, jl.Byte].get.apply(jLong(-12)))
    @Test def test_JL_JC_0(): Unit = assertEquals(jChar(11), TypeCoercion[jl.Long, jl.Character].get.apply(jLong(11)))
    @Test def test_JL_JC_1(): Unit = assertEquals(jChar(-12), TypeCoercion[jl.Long, jl.Character].get.apply(jLong(-12)))
    @Test def test_JL_JD_0(): Unit = assertEquals(jDouble(11), TypeCoercion[jl.Long, jl.Double].get.apply(jLong(11)))
    @Test def test_JL_JD_1(): Unit = assertEquals(jDouble(-12), TypeCoercion[jl.Long, jl.Double].get.apply(jLong(-12)))
    @Test def test_JL_JF_0(): Unit = assertEquals(jFloat(11), TypeCoercion[jl.Long, jl.Float].get.apply(jLong(11)))
    @Test def test_JL_JF_1(): Unit = assertEquals(jFloat(-12), TypeCoercion[jl.Long, jl.Float].get.apply(jLong(-12)))
    @Test def test_JL_JI_0(): Unit = assertEquals(jInt(11), TypeCoercion[jl.Long, jl.Integer].get.apply(jLong(11)))
    @Test def test_JL_JI_1(): Unit = assertEquals(jInt(-12), TypeCoercion[jl.Long, jl.Integer].get.apply(jLong(-12)))
    @Test def test_JL_JL_0(): Unit = assertEquals(jLong(11), TypeCoercion[jl.Long, jl.Long].get.apply(jLong(11)))
    @Test def test_JL_JL_1(): Unit = assertEquals(jLong(-12), TypeCoercion[jl.Long, jl.Long].get.apply(jLong(-12)))
    @Test def test_JL_JSh_0(): Unit = assertEquals(jShort(11), TypeCoercion[jl.Long, jl.Short].get.apply(jLong(11)))
    @Test def test_JL_JSh_1(): Unit = assertEquals(jShort(-12), TypeCoercion[jl.Long, jl.Short].get.apply(jLong(-12)))
    @Test def test_JL_L_0(): Unit = assertEquals(11L, TypeCoercion[jl.Long, Long].get.apply(jLong(11)))
    @Test def test_JL_L_1(): Unit = assertEquals(-12L, TypeCoercion[jl.Long, Long].get.apply(jLong(-12)))
    @Test def test_JL_Sh_0(): Unit = assertEquals(11.toShort, TypeCoercion[jl.Long, Short].get.apply(jLong(11)))
    @Test def test_JL_Sh_1(): Unit = assertEquals(-12.toShort, TypeCoercion[jl.Long, Short].get.apply(jLong(-12)))
    @Test def test_JL_St_0(): Unit = assertEquals("11", TypeCoercion[jl.Long, String].get.apply(jLong(11)))
    @Test def test_JL_St_1(): Unit = assertEquals("-12", TypeCoercion[jl.Long, String].get.apply(jLong(-12)))


    @Test def test_JSh_By_0(): Unit = assertEquals(9.toByte, TypeCoercion[jl.Short, Byte].get.apply(jShort(9)))
    @Test def test_JSh_By_1(): Unit = assertEquals(-10.toByte, TypeCoercion[jl.Short, Byte].get.apply(jShort(-10)))
    @Test def test_JSh_C_0(): Unit = assertEquals(9.toChar, TypeCoercion[jl.Short, Char].get.apply(jShort(9)))
    @Test def test_JSh_C_1(): Unit = assertEquals(-10.toChar, TypeCoercion[jl.Short, Char].get.apply(jShort(-10)))
    @Test def test_JSh_D_0(): Unit = assertEquals(9d, TypeCoercion[jl.Short, Double].get.apply(jShort(9)), 0)
    @Test def test_JSh_D_1(): Unit = assertEquals(-10d, TypeCoercion[jl.Short, Double].get.apply(jShort(-10)), 0)
    @Test def test_JSh_F_0(): Unit = assertEquals(9f, TypeCoercion[jl.Short, Float].get.apply(jShort(9)), 0)
    @Test def test_JSh_F_1(): Unit = assertEquals(-10f, TypeCoercion[jl.Short, Float].get.apply(jShort(-10)), 0)
    @Test def test_JSh_I_0(): Unit = assertEquals(9, TypeCoercion[jl.Short, Int].get.apply(jShort(9)))
    @Test def test_JSh_I_1(): Unit = assertEquals(-10, TypeCoercion[jl.Short, Int].get.apply(jShort(-10)))
    @Test def test_JSh_JBy_0(): Unit = assertEquals(jByte(9), TypeCoercion[jl.Short, jl.Byte].get.apply(jShort(9)))
    @Test def test_JSh_JBy_1(): Unit = assertEquals(jByte(-10), TypeCoercion[jl.Short, jl.Byte].get.apply(jShort(-10)))
    @Test def test_JSh_JC_0(): Unit = assertEquals(jChar(9), TypeCoercion[jl.Short, jl.Character].get.apply(jShort(9)))
    @Test def test_JSh_JC_1(): Unit = assertEquals(jChar(-10), TypeCoercion[jl.Short, jl.Character].get.apply(jShort(-10)))
    @Test def test_JSh_JD_0(): Unit = assertEquals(jDouble(9).doubleValue, TypeCoercion[jl.Short, jl.Double].get.apply(jShort(9)).doubleValue, 0)
    @Test def test_JSh_JD_1(): Unit = assertEquals(jDouble(-10).doubleValue, TypeCoercion[jl.Short, jl.Double].get.apply(jShort(-10)).doubleValue, 0)
    @Test def test_JSh_JF_0(): Unit = assertEquals(jFloat(9).floatValue, TypeCoercion[jl.Short, jl.Float].get.apply(jShort(9)).floatValue, 0)
    @Test def test_JSh_JF_1(): Unit = assertEquals(jFloat(-10).floatValue, TypeCoercion[jl.Short, jl.Float].get.apply(jShort(-10)).floatValue, 0)
    @Test def test_JSh_JI_0(): Unit = assertEquals(jInt(9), TypeCoercion[jl.Short, jl.Integer].get.apply(jShort(9)))
    @Test def test_JSh_JI_1(): Unit = assertEquals(jInt(-10), TypeCoercion[jl.Short, jl.Integer].get.apply(jShort(-10)))
    @Test def test_JSh_JL_0(): Unit = assertEquals(jLong(9), TypeCoercion[jl.Short, jl.Long].get.apply(jShort(9)))
    @Test def test_JSh_JL_1(): Unit = assertEquals(jLong(-10), TypeCoercion[jl.Short, jl.Long].get.apply(jShort(-10)))
    @Test def test_JSh_JSh_0(): Unit = assertEquals(jShort(9), TypeCoercion[jl.Short, jl.Short].get.apply(jShort(9)))
    @Test def test_JSh_JSh_1(): Unit = assertEquals(jShort(-10), TypeCoercion[jl.Short, jl.Short].get.apply(jShort(-10)))
    @Test def test_JSh_L_0(): Unit = assertEquals(9L, TypeCoercion[jl.Short, Long].get.apply(jShort(9)))
    @Test def test_JSh_L_1(): Unit = assertEquals(-10L, TypeCoercion[jl.Short, Long].get.apply(jShort(-10)))
    @Test def test_JSh_Sh_0(): Unit = assertEquals(9.toShort, TypeCoercion[jl.Short, Short].get.apply(jShort(9)))
    @Test def test_JSh_Sh_1(): Unit = assertEquals(-10.toShort, TypeCoercion[jl.Short, Short].get.apply(jShort(-10)))
    @Test def test_JSh_St_0(): Unit = assertEquals("9", TypeCoercion[jl.Short, String].get.apply(jShort(9)))
    @Test def test_JSh_St_1(): Unit = assertEquals("-10", TypeCoercion[jl.Short, String].get.apply(jShort(-10)))


    @Test def test_L_By_0(): Unit = assertEquals(5.toByte, TypeCoercion[Long, Byte].get.apply(5L))
    @Test def test_L_By_1(): Unit = assertEquals(-6.toByte, TypeCoercion[Long, Byte].get.apply(-6L))
    @Test def test_L_C_0(): Unit = assertEquals(5.toChar, TypeCoercion[Long, Char].get.apply(5L))
    @Test def test_L_C_1(): Unit = assertEquals((-6).toChar, TypeCoercion[Long, Char].get.apply(-6L))
    @Test def test_L_D_0(): Unit = assertEquals(5d, TypeCoercion[Long, Double].get.apply(5L), 0)
    @Test def test_L_D_1(): Unit = assertEquals(-6d, TypeCoercion[Long, Double].get.apply(-6L), 0)
    @Test def test_L_F_0(): Unit = assertEquals(5f, TypeCoercion[Long, Float].get.apply(5L), 0)
    @Test def test_L_F_1(): Unit = assertEquals(-6f, TypeCoercion[Long, Float].get.apply(-6L), 0)
    @Test def test_L_I_0(): Unit = assertEquals(5, TypeCoercion[Long, Int].get.apply(5L))
    @Test def test_L_I_1(): Unit = assertEquals(-6, TypeCoercion[Long, Int].get.apply(-6L))
    @Test def test_L_JBy_0(): Unit = assertEquals(jByte(5), TypeCoercion[Long, jl.Byte].get.apply(5L))
    @Test def test_L_JBy_1(): Unit = assertEquals(jByte(-6), TypeCoercion[Long, jl.Byte].get.apply(-6L))
    @Test def test_L_JC_0(): Unit = assertEquals(jChar(5), TypeCoercion[Long, jl.Character].get.apply(5L))
    @Test def test_L_JC_1(): Unit = assertEquals(jChar(-6), TypeCoercion[Long, jl.Character].get.apply(-6L))
    @Test def test_L_JD_0(): Unit = assertEquals(jDouble(5).doubleValue, TypeCoercion[Long, jl.Double].get.apply(5L).doubleValue, 0)
    @Test def test_L_JD_1(): Unit = assertEquals(jDouble(-6).doubleValue, TypeCoercion[Long, jl.Double].get.apply(-6L).doubleValue, 0)
    @Test def test_L_JF_0(): Unit = assertEquals(jFloat(5).floatValue, TypeCoercion[Long, jl.Float].get.apply(5L).floatValue, 0)
    @Test def test_L_JF_1(): Unit = assertEquals(jFloat(-6).floatValue, TypeCoercion[Long, jl.Float].get.apply(-6L).floatValue, 0)
    @Test def test_L_JI_0(): Unit = assertEquals(jInt(5), TypeCoercion[Long, jl.Integer].get.apply(5L))
    @Test def test_L_JI_1(): Unit = assertEquals(jInt(-6), TypeCoercion[Long, jl.Integer].get.apply(-6L))
    @Test def test_L_JL_0(): Unit = assertEquals(jLong(5), TypeCoercion[Long, jl.Long].get.apply(5L))
    @Test def test_L_JL_1(): Unit = assertEquals(jLong(-6), TypeCoercion[Long, jl.Long].get.apply(-6L))
    @Test def test_L_JSh_0(): Unit = assertEquals(jShort(5), TypeCoercion[Long, jl.Short].get.apply(5L))
    @Test def test_L_JSh_1(): Unit = assertEquals(jShort(-6), TypeCoercion[Long, jl.Short].get.apply(-6L))
    @Test def test_L_L_0(): Unit = assertEquals(5L, TypeCoercion[Long, Long].get.apply(5L))
    @Test def test_L_L_1(): Unit = assertEquals(-6L, TypeCoercion[Long, Long].get.apply(-6L))
    @Test def test_L_Sh_0(): Unit = assertEquals(5.toShort, TypeCoercion[Long, Short].get.apply(5L))
    @Test def test_L_Sh_1(): Unit = assertEquals(-6.toShort, TypeCoercion[Long, Short].get.apply(-6L))
    @Test def test_L_St_0(): Unit = assertEquals("5", TypeCoercion[Long, String].get.apply(5L))
    @Test def test_L_St_1(): Unit = assertEquals("-6", TypeCoercion[Long, String].get.apply(-6L))


    @Test def test_Sh_By_0(): Unit = assertEquals(3.toByte, TypeCoercion[Short, Byte].get.apply(3.toShort))
    @Test def test_Sh_By_1(): Unit = assertEquals((-4).toByte, TypeCoercion[Short, Byte].get.apply((-4).toShort))
    @Test def test_Sh_C_0(): Unit = assertEquals(3.toChar, TypeCoercion[Short, Char].get.apply(3.toShort))
    @Test def test_Sh_C_1(): Unit = assertEquals((-4).toChar, TypeCoercion[Short, Char].get.apply((-4).toShort))
    @Test def test_Sh_D_0(): Unit = assertEquals(3d, TypeCoercion[Short, Double].get.apply(3.toShort), 0)
    @Test def test_Sh_D_1(): Unit = assertEquals(-4d, TypeCoercion[Short, Double].get.apply((-4).toShort), 0)
    @Test def test_Sh_F_0(): Unit = assertEquals(3f, TypeCoercion[Short, Float].get.apply(3.toShort), 0)
    @Test def test_Sh_F_1(): Unit = assertEquals(-4f, TypeCoercion[Short, Float].get.apply((-4).toShort), 0)
    @Test def test_Sh_I_0(): Unit = assertEquals(3, TypeCoercion[Short, Int].get.apply(3.toShort))
    @Test def test_Sh_I_1(): Unit = assertEquals(-4, TypeCoercion[Short, Int].get.apply((-4).toShort))
    @Test def test_Sh_JBy_0(): Unit = assertEquals(jByte(3), TypeCoercion[Short, jl.Byte].get.apply(3.toShort))
    @Test def test_Sh_JBy_1(): Unit = assertEquals(jByte(-4), TypeCoercion[Short, jl.Byte].get.apply((-4).toShort))
    @Test def test_Sh_JC_0(): Unit = assertEquals(jChar(3), TypeCoercion[Short, jl.Character].get.apply(3.toShort))
    @Test def test_Sh_JC_1(): Unit = assertEquals(jChar(-4), TypeCoercion[Short, jl.Character].get.apply((-4).toShort))
    @Test def test_Sh_JD_0(): Unit = assertEquals(jDouble(3).doubleValue, TypeCoercion[Short, jl.Double].get.apply(3.toShort).doubleValue, 0)
    @Test def test_Sh_JD_1(): Unit = assertEquals(jDouble(-4).doubleValue, TypeCoercion[Short, jl.Double].get.apply((-4).toShort).doubleValue, 0)
    @Test def test_Sh_JF_0(): Unit = assertEquals(jFloat(3).floatValue, TypeCoercion[Short, jl.Float].get.apply(3.toShort).floatValue, 0)
    @Test def test_Sh_JF_1(): Unit = assertEquals(jFloat(-4).floatValue, TypeCoercion[Short, jl.Float].get.apply((-4).toShort).floatValue, 0)
    @Test def test_Sh_JI_0(): Unit = assertEquals(jInt(3), TypeCoercion[Short, jl.Integer].get.apply(3.toShort))
    @Test def test_Sh_JI_1(): Unit = assertEquals(jInt(-4), TypeCoercion[Short, jl.Integer].get.apply((-4).toShort))
    @Test def test_Sh_JL_0(): Unit = assertEquals(jLong(3), TypeCoercion[Short, jl.Long].get.apply(3.toShort))
    @Test def test_Sh_JL_1(): Unit = assertEquals(jLong(-4), TypeCoercion[Short, jl.Long].get.apply((-4).toShort))
    @Test def test_Sh_JSh_0(): Unit = assertEquals(jShort(3), TypeCoercion[Short, jl.Short].get.apply(3.toShort))
    @Test def test_Sh_JSh_1(): Unit = assertEquals(jShort(-4), TypeCoercion[Short, jl.Short].get.apply((-4).toShort))
    @Test def test_Sh_L_0(): Unit = assertEquals(3L, TypeCoercion[Short, Long].get.apply(3.toShort))
    @Test def test_Sh_L_1(): Unit = assertEquals(-4L, TypeCoercion[Short, Long].get.apply((-4).toShort))
    @Test def test_Sh_Sh_0(): Unit = assertEquals(3.toShort, TypeCoercion[Short, Short].get.apply(3.toShort))
    @Test def test_Sh_Sh_1(): Unit = assertEquals((-4).toShort, TypeCoercion[Short, Short].get.apply((-4).toShort))
    @Test def test_Sh_St_0(): Unit = assertEquals("3", TypeCoercion[Short, String].get.apply(3.toShort))
    @Test def test_Sh_St_1(): Unit = assertEquals("-4", TypeCoercion[Short, String].get.apply((-4).toShort))


    @Test def test_St_St_0(): Unit = assertEquals("31", TypeCoercion[String, String].get.apply("31"))
    @Test def test_St_St_1(): Unit = assertEquals("-1", TypeCoercion[String, String].get.apply("-1"))
    @Test def test_St_St_2(): Unit = assertEquals("true", TypeCoercion[String, String].get.apply("true"))
    @Test def test_St_St_3(): Unit = assertEquals("false", TypeCoercion[String, String].get.apply("false"))


    @Test def testNoCoersion_Bo_C(): Unit = assertTrue(TypeCoercion[Boolean, Char].isEmpty)
    @Test def testNoCoersion_Bo_By(): Unit = assertTrue(TypeCoercion[Boolean, Byte].isEmpty)
    @Test def testNoCoersion_Bo_Sh(): Unit = assertTrue(TypeCoercion[Boolean, Short].isEmpty)
    @Test def testNoCoersion_Bo_I(): Unit = assertTrue(TypeCoercion[Boolean, Int].isEmpty)
    @Test def testNoCoersion_Bo_L(): Unit = assertTrue(TypeCoercion[Boolean, Long].isEmpty)
    @Test def testNoCoersion_Bo_F(): Unit = assertTrue(TypeCoercion[Boolean, Float].isEmpty)
    @Test def testNoCoersion_Bo_D(): Unit = assertTrue(TypeCoercion[Boolean, Double].isEmpty)
    @Test def testNoCoersion_Bo_JBy(): Unit = assertTrue(TypeCoercion[Boolean, jl.Byte].isEmpty)
    @Test def testNoCoersion_Bo_JSh(): Unit = assertTrue(TypeCoercion[Boolean, jl.Short].isEmpty)
    @Test def testNoCoersion_Bo_JI(): Unit = assertTrue(TypeCoercion[Boolean, jl.Integer].isEmpty)
    @Test def testNoCoersion_Bo_JL(): Unit = assertTrue(TypeCoercion[Boolean, jl.Long].isEmpty)
    @Test def testNoCoersion_Bo_JF(): Unit = assertTrue(TypeCoercion[Boolean, jl.Float].isEmpty)
    @Test def testNoCoersion_Bo_JD(): Unit = assertTrue(TypeCoercion[Boolean, jl.Double].isEmpty)
    @Test def testNoCoersion_Bo_JC(): Unit = assertTrue(TypeCoercion[Boolean, jl.Character].isEmpty)
    @Test def testNoCoersion_C_Bo(): Unit = assertTrue(TypeCoercion[Char, Boolean].isEmpty)
    @Test def testNoCoersion_C_JBo(): Unit = assertTrue(TypeCoercion[Char, jl.Boolean].isEmpty)
    @Test def testNoCoersion_By_Bo(): Unit = assertTrue(TypeCoercion[Byte, Boolean].isEmpty)
    @Test def testNoCoersion_By_JBo(): Unit = assertTrue(TypeCoercion[Byte, jl.Boolean].isEmpty)
    @Test def testNoCoersion_Sh_Bo(): Unit = assertTrue(TypeCoercion[Short, Boolean].isEmpty)
    @Test def testNoCoersion_Sh_JBo(): Unit = assertTrue(TypeCoercion[Short, jl.Boolean].isEmpty)
    @Test def testNoCoersion_I_Bo(): Unit = assertTrue(TypeCoercion[Int, Boolean].isEmpty)
    @Test def testNoCoersion_I_JBo(): Unit = assertTrue(TypeCoercion[Int, jl.Boolean].isEmpty)
    @Test def testNoCoersion_L_Bo(): Unit = assertTrue(TypeCoercion[Long, Boolean].isEmpty)
    @Test def testNoCoersion_L_JBo(): Unit = assertTrue(TypeCoercion[Long, jl.Boolean].isEmpty)
    @Test def testNoCoersion_F_Bo(): Unit = assertTrue(TypeCoercion[Float, Boolean].isEmpty)
    @Test def testNoCoersion_F_JBo(): Unit = assertTrue(TypeCoercion[Float, jl.Boolean].isEmpty)
    @Test def testNoCoersion_D_Bo(): Unit = assertTrue(TypeCoercion[Double, Boolean].isEmpty)
    @Test def testNoCoersion_D_JBo(): Unit = assertTrue(TypeCoercion[Double, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JBy_Bo(): Unit = assertTrue(TypeCoercion[jl.Byte, Boolean].isEmpty)
    @Test def testNoCoersion_JBy_JBo(): Unit = assertTrue(TypeCoercion[jl.Byte, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JSh_Bo(): Unit = assertTrue(TypeCoercion[jl.Short, Boolean].isEmpty)
    @Test def testNoCoersion_JSh_JBo(): Unit = assertTrue(TypeCoercion[jl.Short, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JI_Bo(): Unit = assertTrue(TypeCoercion[jl.Integer, Boolean].isEmpty)
    @Test def testNoCoersion_JI_JBo(): Unit = assertTrue(TypeCoercion[jl.Integer, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JL_Bo(): Unit = assertTrue(TypeCoercion[jl.Long, Boolean].isEmpty)
    @Test def testNoCoersion_JL_JBo(): Unit = assertTrue(TypeCoercion[jl.Long, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JF_Bo(): Unit = assertTrue(TypeCoercion[jl.Float, Boolean].isEmpty)
    @Test def testNoCoersion_JF_JBo(): Unit = assertTrue(TypeCoercion[jl.Float, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JD_Bo(): Unit = assertTrue(TypeCoercion[jl.Double, Boolean].isEmpty)
    @Test def testNoCoersion_JD_JBo(): Unit = assertTrue(TypeCoercion[jl.Double, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JC_Bo(): Unit = assertTrue(TypeCoercion[jl.Character, Boolean].isEmpty)
    @Test def testNoCoersion_JC_JBo(): Unit = assertTrue(TypeCoercion[jl.Character, jl.Boolean].isEmpty)
    @Test def testNoCoersion_JBo_C(): Unit = assertTrue(TypeCoercion[jl.Boolean, Char].isEmpty)
    @Test def testNoCoersion_JBo_By(): Unit = assertTrue(TypeCoercion[jl.Boolean, Byte].isEmpty)
    @Test def testNoCoersion_JBo_Sh(): Unit = assertTrue(TypeCoercion[jl.Boolean, Short].isEmpty)
    @Test def testNoCoersion_JBo_I(): Unit = assertTrue(TypeCoercion[jl.Boolean, Int].isEmpty)
    @Test def testNoCoersion_JBo_L(): Unit = assertTrue(TypeCoercion[jl.Boolean, Long].isEmpty)
    @Test def testNoCoersion_JBo_F(): Unit = assertTrue(TypeCoercion[jl.Boolean, Float].isEmpty)
    @Test def testNoCoersion_JBo_D(): Unit = assertTrue(TypeCoercion[jl.Boolean, Double].isEmpty)
    @Test def testNoCoersion_JBo_JBy(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Byte].isEmpty)
    @Test def testNoCoersion_JBo_JSh(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Short].isEmpty)
    @Test def testNoCoersion_JBo_JI(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Integer].isEmpty)
    @Test def testNoCoersion_JBo_JL(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Long].isEmpty)
    @Test def testNoCoersion_JBo_JF(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Float].isEmpty)
    @Test def testNoCoersion_JBo_JD(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Double].isEmpty)
    @Test def testNoCoersion_JBo_JC(): Unit = assertTrue(TypeCoercion[jl.Boolean, jl.Character].isEmpty)
    @Test def testNoCoersion_St_C(): Unit = assertTrue(TypeCoercion[String, Char].isEmpty)
    @Test def testNoCoersion_St_By(): Unit = assertTrue(TypeCoercion[String, Byte].isEmpty)
    @Test def testNoCoersion_St_Sh(): Unit = assertTrue(TypeCoercion[String, Short].isEmpty)
    @Test def testNoCoersion_St_I(): Unit = assertTrue(TypeCoercion[String, Int].isEmpty)
    @Test def testNoCoersion_St_L(): Unit = assertTrue(TypeCoercion[String, Long].isEmpty)
    @Test def testNoCoersion_St_F(): Unit = assertTrue(TypeCoercion[String, Float].isEmpty)
    @Test def testNoCoersion_St_D(): Unit = assertTrue(TypeCoercion[String, Double].isEmpty)
    @Test def testNoCoersion_St_JBy(): Unit = assertTrue(TypeCoercion[String, jl.Byte].isEmpty)
    @Test def testNoCoersion_St_JSh(): Unit = assertTrue(TypeCoercion[String, jl.Short].isEmpty)
    @Test def testNoCoersion_St_JI(): Unit = assertTrue(TypeCoercion[String, jl.Integer].isEmpty)
    @Test def testNoCoersion_St_JL(): Unit = assertTrue(TypeCoercion[String, jl.Long].isEmpty)
    @Test def testNoCoersion_St_JF(): Unit = assertTrue(TypeCoercion[String, jl.Float].isEmpty)
    @Test def testNoCoersion_St_JD(): Unit = assertTrue(TypeCoercion[String, jl.Double].isEmpty)
    @Test def testNoCoersion_St_JC(): Unit = assertTrue(TypeCoercion[String, jl.Character].isEmpty)
    @Test def testNoCoersion_St_JBo(): Unit = assertTrue(TypeCoercion[String, jl.Boolean].isEmpty)


    /**
     * Take the matrix from the comment and ensure that if a coercion is supposed to exist, then it does.  This
     * doesn't test that the conversions are actually correct.
     */
    @Test def testRawCoercionsExist(): Unit = {
        val funcs = for {
            from <- matrix.indices
            to <- matrix(from).indices
            shouldExist = matrix(from)(to)
            f = types(from)
            t = types(to)
            doesExist = TypeCoercion(f, t).isDefined
            ok = shouldExist == doesExist  // p -> q == ~p v q
        } yield (shouldExist, ok, s"${RefInfoOps.toString(f)} => ${RefInfoOps.toString(t)}")

        debug(s"Found ${funcs.count(_._1)} coercion functions.")
        val missing = funcs.collect { case (sE, ok, f) if !ok => f }
        assertTrue(s"Found ${missing.size} functions missing: ${missing.mkString("\n\t", "\n\t", "")}", missing.isEmpty)
    }

    /**
     * Take the matrix from the comment and ensure that if a coercion is supposed to exist, then it does.  This
     * doesn't test that the conversions are actually correct.  This tests that coercions of the form:
     *
     * A => Option[B] exists where A and B are not options.
     */
    @Test def testToOptCoercionsExist(): Unit = {
        val funcs = for {
            from <- matrix.indices
            to <- matrix(from).indices
            shouldExist = matrix(from)(to)
            f = types(from)
            t = RefInfoOps.option(types(to))
            doesExist = TypeCoercion(f, t).isDefined
            ok = shouldExist == doesExist  // p -> q == ~p v q
        } yield (shouldExist, ok, s"${RefInfoOps.toString(f)} => ${RefInfoOps.toString(t)}")

        debug(s"Found ${funcs.count(_._1)} toOption coercion functions.")
        val missing = funcs.collect { case (sE, ok, f) if !ok => f }
        assertTrue(s"Found ${missing.size} toOption functions missing: ${missing.mkString("\n\t", "\n\t", "")}", missing.isEmpty)
    }

    /**
     * Take the matrix from the comment and ensure that if a coercion is supposed to exist, then it does.  This
     * doesn't test that the conversions are actually correct.  This tests that coercions of the form:
     *
     * Option[A] => Option[B] exists where A and B are not options.
     */
    @Test def testOptOptCoercionsExist(): Unit = {
        val funcs = for {
            from <- matrix.indices
            to <- matrix(from).indices
            shouldExist = matrix(from)(to)
            f = RefInfoOps.option(types(from))
            t = RefInfoOps.option(types(to))
            doesExist = TypeCoercion(f, t).isDefined
            ok = shouldExist == doesExist  // p -> q == ~p v q
        } yield (shouldExist, ok, s"${RefInfoOps.toString(f)} => ${RefInfoOps.toString(t)}")

        debug(s"Found ${funcs.count(_._1)} Option to Option coercion functions.")
        val missing = funcs.collect { case (sE, ok, f) if !ok => f }
        assertTrue(s"Found ${missing.size} Option to Option functions missing: ${missing.mkString("\n\t", "\n\t", "")}", missing.isEmpty)
    }

    private[this] def jChar(v: Int) = jl.Character.valueOf(v.toChar)
    private[this] def jByte(v: Int) = jl.Byte.valueOf(v.toByte)
    private[this] def jShort(v: Int) = jl.Short.valueOf(v.toShort)
    private[this] def jInt(v: Int) = jl.Integer.valueOf(v)
    private[this] def jLong(v: Long) = jl.Long.valueOf(v)
    private[this] def jFloat(v: Float) = jl.Float.valueOf(v)
    private[this] def jDouble(v: Double) = jl.Double.valueOf(v)

    // NOTE: The generated tests are based on the following programmatically outputted code, that was then hand modified.

    //    @Test def xyz() {
    //        val valMap = vals.toMap
    //        for {
    //            (from, fi) <- types.zipWithIndex
    //            (to, ti) <- types.zipWithIndex
    //            if matrix(fi)(ti)
    //            (in, n) <- valMap(from).zipWithIndex
    //            fromAbbrev = typeToAbbrev(from)
    //            toAbbrev = typeToAbbrev(to)
    //            fS = RefInfoOps.toString(from)
    //            tS = RefInfoOps.toString(to)
    //            // tc = TypeCoercion(from, to).asInstanceOf[Option[Any => Any]].get
    //            // y = tc(in.asInstanceOf[Any])
    //        } {
    //            println(s"""@Test def test_${fromAbbrev}_${toAbbrev}_$n(): Unit = assertEquals($in, TypeCoercion[$fS, $tS].get.apply($in))""")
    //        }
    //    }


    //    @Test def test(): Unit = {
    //        val data =
    //            """
    //              |Bo C By Sh I L F D JBy JSh JI JL JF JD JC
    //              |C Bo JBo
    //              |By Bo JBo
    //              |Sh Bo JBo
    //              |I Bo JBo
    //              |L Bo JBo
    //              |F Bo JBo
    //              |D Bo JBo
    //              |JBy Bo JBo
    //              |JSh Bo JBo
    //              |JI Bo JBo
    //              |JL Bo JBo
    //              |JF Bo JBo
    //              |JD Bo JBo
    //              |JC Bo JBo
    //              |JBo C By Sh I L F D JBy JSh JI JL JF JD JC
    //              |St C By Sh I L F D JBy JSh JI JL JF JD JC JBo
    //            """.stripMargin.trim
    //
    //        val typeMap = TypeCoercionTest.types.toMap
    //
    //        data.split("\n").map(_.split(" ").toList).foreach { case from :: toLst =>
    //            val f = typeMap(from)
    //            toLst foreach { ts =>
    //                val t = typeMap(ts)
    //                val a = RefInfoOps.toString(f).replaceAll("java.lang.String", "String")
    //                val b = RefInfoOps.toString(t).replaceAll("java.lang.String", "String")
    //                val fn = s"@Test def testNoCoersion_${from}_$ts(): Unit = assertTrue(TypeCoercion[$a, $b].isEmpty)"
    //                println(fn)
    //            }
    //        }
    //    }
}

private object TypeCoercionTest {

    val Precision = 1e-6

    /**
     * This matrix is copied from the scaladoc for TypeCoercion.
     */
    val matrixStr =
        """
          | *             Bo  C   By  Sh  I   L   F   D   JBy JSh JI  JL  JF  JD  JC  JBo St
          | *           +--------------------------------------------------------------------
          | *       Bo  | I                                                           bB  tS
          | *       C   |     I   A   A   A   A   A   A   A   A   A   A   A   A   A       tS
          | *       By  |     A   I   A   A   A   A   A   A   A   A   A   A   A   A       tS
          | *       Sh  |     A   A   I   A   A   A   A   A   A   A   A   A   A   A       tS
          | *       I   |     A   A   A   I   A   A   A   A   A   A   A   A   A   A       tS
          | *       L   |     A   A   A   A   I   A   A   A   A   A   A   A   A   A       tS
          | *  F    F   |     A   A   A   A   A   I   A   A   A   A   A   A   A   A       tS
          | *  R    D   |     A   A   A   A   A   A   I   A   A   A   A   A   A   A       tS
          | *  O    JBy |     N   N   N   N   N   N   N   I   N   N   N   N   N   N       tS
          | *  M    JSh |     N   N   N   N   N   N   N   N   I   N   N   N   N   N       tS
          | *       JI  |     N   N   N   N   N   N   N   N   N   I   N   N   N   N       tS
          | *       JL  |     N   N   N   N   N   N   N   N   N   N   I   N   N   N       tS
          | *       JF  |     N   N   N   N   N   N   N   N   N   N   N   I   N   N       tS
          | *       JD  |     N   N   N   N   N   N   N   N   N   N   N   N   I   N       tS
          | *       JC  |     uC                                                  I       tS
          | *       JBo | uB                                                          I   tS
          | *       St  |                                                                 I
        """.stripMargin.trim


//    val matrixStr =
//        """
//          | *             Bo  C   By  Sh  I   L   F   D   JBy JSh JI  JL  JF  JD  JC  JBo St
//          | *           +--------------------------------------------------------------------
//          | *       Bo  | I                                                           bB  tS
//          | *       C   |     I   A   A   A   A   A   A   A   A   A   A   A   A   A       tS
//          | *       By  |     A   I   A   A   A   A   A   A   A   A   A   A   A   A       tS
//          | *       Sh  |     A   A   I   A   A   A   A   A   A   A   A   A   A   A       tS
//          | *       I   |     A   A   A   I   A   A   A   A   A   A   A   A   A   A       tS
//          | *       L   |     A   A   A   A   I   A   A   A   A   A   A   A   A   A       tS
//          | *  F    F   |     A   A   A   A   A   I   A   A   A   A   A   A   A   A       tS
//          | *  R    D   |     A   A   A   A   A   A   I   A   A   A   A   A   A   A       tS
//          | *  O    JBy |     N   N   N   N   N   N   N   I   N   N   N   N   N   N       tS
//          | *  M    JSh |     N   N   N   N   N   N   N   N   I   N   N   N   N   N       tS
//          | *       JI  |     N   N   N   N   N   N   N   N   N   I   N   N   N   N       tS
//          | *       JL  |     N   N   N   N   N   N   N   N   N   N   I   N   N   N       tS
//          | *       JF  |     N   N   N   N   N   N   N   N   N   N   N   I   N   N       tS
//          | *       JD  |     N   N   N   N   N   N   N   N   N   N   N   N   I   N       tS
//          | *       JC  |     uC                                                  I       tS
//          | *       JBo | uB                                                          I   tS
//          | *       St  | fS      fS  fS  fS  fS  fS  fS  fS  fS  fS  fS  fS  fS      fS  I
//        """.stripMargin.trim


    val types = Vector(
        "Bo"  -> RefInfo.Boolean,
        "C"   -> RefInfo.Char,
        "By"  -> RefInfo.Byte,
        "Sh"  -> RefInfo.Short,
        "I"   -> RefInfo.Int,
        "L"   -> RefInfo.Long,
        "F"   -> RefInfo.Float,
        "D"   -> RefInfo.Double,
        "JBy" -> RefInfo.JavaByte,
        "JSh" -> RefInfo.JavaShort,
        "JI"  -> RefInfo.JavaInteger,
        "JL"  -> RefInfo.JavaLong,
        "JF"  -> RefInfo.JavaFloat,
        "JD"  -> RefInfo.JavaDouble,
        "JC"  -> RefInfo.JavaCharacter,
        "JBo" -> RefInfo.JavaBoolean,
        "St"  -> RefInfo.String
    )

    // Was used to generate tests.
    val typeToAbbrev = Map(
        RefInfo.Boolean       -> "Bo",
        RefInfo.Char          -> "C",
        RefInfo.Byte          -> "By",
        RefInfo.Short         -> "Sh",
        RefInfo.Int           -> "I",
        RefInfo.Long          -> "L",
        RefInfo.Float         -> "F",
        RefInfo.Double        -> "D",
        RefInfo.JavaByte      -> "JBy",
        RefInfo.JavaShort     -> "JSh",
        RefInfo.JavaInteger   -> "JI",
        RefInfo.JavaLong      -> "JL",
        RefInfo.JavaFloat     -> "JF",
        RefInfo.JavaDouble    -> "JD",
        RefInfo.JavaCharacter -> "JC",
        RefInfo.JavaBoolean   -> "JBo",
        RefInfo.String        ->  "St"
    )

    val vals = Vector(
        RefInfo.Boolean       -> Seq("true", "false"),
        RefInfo.Char          -> Seq("'2'", "'3'"),
        RefInfo.Byte          -> Seq("2.toByte", "(-3).toByte"),
        RefInfo.Short         -> Seq("3.toShort", "(-4).toShort"),
        RefInfo.Int           -> Seq("4.toShort", "-5"),
        RefInfo.Long          -> Seq("5L", "-6L"),
        RefInfo.Float         -> Seq("6.499f", "-7.499f"),
        RefInfo.Double        -> Seq("7.5", "-8.5"),
        RefInfo.JavaByte      -> Seq("java.lang.Byte.valueOf(8.toByte)", "java.lang.Byte.valueOf((-9).toByte)"),
        RefInfo.JavaShort     -> Seq("java.lang.Short.valueOf(9.toShort)", "java.lang.Short.valueOf((-10).toShort)"),
        RefInfo.JavaInteger   -> Seq("java.lang.Integer.valueOf(10)", "java.lang.Integer.valueOf(-11)"),
        RefInfo.JavaLong      -> Seq("java.lang.Long.valueOf(11)", "java.lang.Long.valueOf(-12)"),
        RefInfo.JavaFloat     -> Seq("java.lang.Float.valueOf(12.499f)", "java.lang.Float.valueOf(-13.499f)"),
        RefInfo.JavaDouble    -> Seq("java.lang.Double.valueOf(13.5)", "java.lang.Double.valueOf(-14.5)"),
        RefInfo.JavaCharacter -> Seq("java.lang.Character.valueOf('4')", "java.lang.Character.valueOf('5')"),
        RefInfo.JavaBoolean   -> Seq("java.lang.Boolean.FALSE", "java.lang.Boolean.TRUE"),
        RefInfo.String	      -> Seq("\"31\"", "\"-1\"", "\"true\"", "\"false\"")
    )

    def matrixAndTypes() = {
        val typeMap = types.toMap
        val lines = matrixStr.split("\n")
        val headerTypes = getHeaders(lines(0)).map(typeMap.apply)

        // v
        // 2 until ... b/c we skip the header row and the row that's just a horizontal bar.
        // ^
        val m = (2 until lines.size) map { i => getLine(lines(i)) }
        (m, headerTypes)
    }

    def getHeaders(h: String) = h.drop(2).trim.split(" +").toVector
    def getLine(l: String) = l.substring(l.indexOf("|") + 2).grouped(4).map(_.trim.nonEmpty).toVector
}
