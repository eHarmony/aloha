package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.id.ModelId
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan.deak on 10/2/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class EitherAuditorTest {
  import EitherAuditorTest._

  @Test def testAggAllFailures(): Unit = {
    val v2 = D.failure(IdD, E2, M2)
    val v3 = I.failure(IdI, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1 ++ E2 ++ E3, e.errorMsgs)
        assertEquals(M1 ++ M2 ++ M3, e.missingVarNames)
//        assertEquals(Seq(v2.left.get, v3.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testAggFirstChildSuccess(): Unit = {
    val v2 = D.success(IdD, 3d, E2, M2)
    val v3 = I.failure(IdI, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1 ++ E3, e.errorMsgs)
        assertEquals(M1 ++ M3, e.missingVarNames)
//        assertEquals(Seq(v3.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testAggSecondChildSuccess(): Unit = {
    val v2 = D.failure(IdD, E2, M2)
    val v3 = I.success(IdI, 4, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1 ++ E2, e.errorMsgs)
        assertEquals(M1 ++ M2, e.missingVarNames)
//        assertEquals(Seq(v2.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testAggBothChildSuccess(): Unit = {
    val v2 = D.success(IdD, 3d, E2, M2)
    val v3 = I.success(IdI, 4, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
//        assertEquals(Nil, e.subvaluesWithFailures)
    }
  }

  @Test def testAggRootSucces(): Unit = {
    val v2 = D.failure(IdD, E2, M2)
    val v3 = I.failure(IdI, E3, M3)
    val v1 = S.success(IdS, "success", E1, M1)

    v1 match {
      case Right(s) => assertEquals("success", s)
      case Left(e) => fail() // No info
    }
  }

  @Test def testAgg_fsf(): Unit = {
    val v3 = I.failure(IdI, E3, M3)
    val v2 = D.success(IdD, 2d, E2, M2, subvalues = Seq(v3))
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(s) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)

        // Nil because v2 is successful, so it throws away all child info.
//        assertEquals(Nil, e.subvaluesWithFailures)
    }
  }

  @Test def testAgg_ffs(): Unit = {
    val v3 = I.success(IdI, 2, E3, M3)
    val v2 = D.failure(IdD, E2, M2, subvalues = Seq(v3))
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(s) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1 ++ E2, e.errorMsgs)
        assertEquals(M1 ++ M2, e.missingVarNames)

        // v3 is not included because it was successful.
        // None of its children would be included either.
//        assertEquals(Seq(v2.left.get), e.subvaluesWithFailures)
    }
  }








  @Test def testNoAggAllFailures(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
//        assertEquals(Seq(v2.left.get, v3.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testNoAggFirstChildSuccess(): Unit = {
    val v2 = DN.success(IdD, 3d, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
//        assertEquals(Seq(v3.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testNoAggSecondChildSuccess(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.success(IdI, 4, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
//        assertEquals(Seq(v2.left.get), e.subvaluesWithFailures)
    }
  }

  @Test def testNoAggBothChildSuccess(): Unit = {
    val v2 = DN.success(IdD, 3d, E2, M2)
    val v3 = IN.success(IdI, 4, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
//        assertEquals(Nil, e.subvaluesWithFailures)
    }
  }

  @Test def testNoAggRootSucces(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.success(IdS, "success", E1, M1)

    v1 match {
      case Right(s) => assertEquals("success", s)
      case Left(e) => fail() // No info
    }
  }

  @Test def testNoAgg_fsf(): Unit = {
    val v3 = IN.failure(IdI, E3, M3)
    val v2 = DN.success(IdD, 2d, E2, M2, subvalues = Seq(v3))
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(s) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)

        // Nil because v2 is successful, so it throws away all child info.
//        assertEquals(Nil, e.subvaluesWithFailures)
    }
  }

  @Test def testNoAgg_ffs(): Unit = {
    val v3 = IN.success(IdI, 2, E3, M3)
    val v2 = DN.failure(IdD, E2, M2, subvalues = Seq(v3))
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(s) => fail()
      case Left(e) =>
        assertEquals(IdS, e.modelId)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)

        // v3 is not included because it was successful.
        // None of its children would be included either.
//        assertEquals(Seq(v2.left.get), e.subvaluesWithFailures)
    }
  }
}

object EitherAuditorTest {
  private val S = EitherAuditor[String]
  private val I = S.changeType[Int].get
  private val D = S.changeType[Double].get

  private val SN = EitherAuditor[String](aggregateErrsAndMissing = false)
  private val IN = SN.changeType[Int].get
  private val DN = SN.changeType[Double].get

  private val E1 = Seq("e11", "e12")
  private val E2 = Seq("e21", "e22")
  private val E3 = Seq("e31", "e32")
  private val M1 = Set("a", "b")
  private val M2 = Set("b", "c")
  private val M3 = Set("c", "d")

  private val IdS = ModelId(1, "String")
  private val IdI = ModelId(2, "Int")
  private val IdD = ModelId(3, "Double")
}
