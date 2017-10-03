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
        assertEquals(IdS :: List(IdD, IdI), e.failureModelIds)
        assertEquals(E1 ++ E2 ++ E3, e.errorMsgs)
        assertEquals(M1 ++ M2 ++ M3, e.missingVarNames)
    }
  }

  @Test def testAggFirstChildSuccess(): Unit = {
    val v2 = D.success(IdD, 3d, E2, M2)
    val v3 = I.failure(IdI, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS :: List(IdI), e.failureModelIds)
        assertEquals(E1 ++ E3, e.errorMsgs)
        assertEquals(M1 ++ M3, e.missingVarNames)
    }
  }

  @Test def testAggSecondChildSuccess(): Unit = {
    val v2 = D.failure(IdD, E2, M2)
    val v3 = I.success(IdI, 4, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS :: List(IdD), e.failureModelIds)
        assertEquals(E1 ++ E2, e.errorMsgs)
        assertEquals(M1 ++ M2, e.missingVarNames)
    }
  }

  @Test def testAggBothChildSuccess(): Unit = {
    val v2 = D.success(IdD, 3d, E2, M2)
    val v3 = I.success(IdI, 4, E3, M3)
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(IdS :: Nil, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testAggRootSuccess(): Unit = {
    val v2 = D.failure(IdD, E2, M2)
    val v3 = I.failure(IdI, E3, M3)
    val v1 = S.success(IdS, "success", E1, M1, Seq(v2, v3))

    v1 match {
      case Right(s) => assertEquals("success", s)
      case Left(_) => fail() // No info
    }
  }

  @Test def testAgg_fsf(): Unit = {
    val v3 = I.failure(IdI, E3, M3)
    val v2 = D.success(IdD, 2d, E2, M2, subvalues = Seq(v3))
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        // Nil because v2 is successful, so it throws away all child info.
        assertEquals(IdS :: Nil, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testAgg_ffs(): Unit = {
    val v3 = I.success(IdI, 2, E3, M3)
    val v2 = D.failure(IdD, E2, M2, subvalues = Seq(v3))
    val v1 = S.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        // v3 is not included because it was successful.
        // None of its children would be included either.
        assertEquals(IdS :: List(IdD), e.failureModelIds)
        assertEquals(E1 ++ E2, e.errorMsgs)
        assertEquals(M1 ++ M2, e.missingVarNames)
    }
  }

  @Test def testNoAggAllFailures(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testNoAggFirstChildSuccess(): Unit = {
    val v2 = DN.success(IdD, 3d, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testNoAggSecondChildSuccess(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.success(IdI, 4, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testNoAggBothChildSuccess(): Unit = {
    val v2 = DN.success(IdD, 3d, E2, M2)
    val v3 = IN.success(IdI, 4, E3, M3)
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2, v3))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testNoAggRootSuccess(): Unit = {
    val v2 = DN.failure(IdD, E2, M2)
    val v3 = IN.failure(IdI, E3, M3)
    val v1 = SN.success(IdS, "success", E1, M1, Seq(v2, v3))

    v1 match {
      case Right(s) => assertEquals("success", s)
      case Left(_) => fail() // No info
    }
  }

  @Test def testNoAgg_fsf(): Unit = {
    val v3 = IN.failure(IdI, E3, M3)
    val v2 = DN.success(IdD, 2d, E2, M2, subvalues = Seq(v3))
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        // Nil because v2 is successful, so it throws away all child info.
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testNoAgg_ffs(): Unit = {
    val v3 = IN.success(IdI, 2, E3, M3)
    val v2 = DN.failure(IdD, E2, M2, subvalues = Seq(v3))
    val v1 = SN.failure(IdS, E1, M1, subvalues = Seq(v2))

    v1 match {
      case Right(_) => fail()
      case Left(e) =>
        // v3 is not included because it was successful.
        // None of its children would be included either.
        assertEquals(NoAggFailMidList, e.failureModelIds)
        assertEquals(E1, e.errorMsgs)
        assertEquals(M1, e.missingVarNames)
    }
  }

  @Test def testDfsPreOrder(): Unit = {
    val a = EitherAuditor[Int]

    // Tree:
    //
    //   1 +- 2 +- 3
    //     |    |
    //     |    +- 4
    //     |
    //     +- 5 +- 6
    //          |
    //          +- 7
    //
    val v3 = a.failure(ModelId(3))
    val v4 = a.failure(ModelId(4))
    val v2 = a.failure(ModelId(2), subvalues = Seq(v3, v4))

    val v6 = a.failure(ModelId(6))
    val v7 = a.failure(ModelId(7))
    val v5 = a.failure(ModelId(5), subvalues = Seq(v6, v7))

    val v1 = a.failure(ModelId(1), subvalues = Seq(v2, v5))

    val mids = v1.left.get.failureModelIds
    assertEquals(1 to 7, mids.map(mid => mid.getId()))
  }

  @Test def testScaladocExample(): Unit = {
    val a = EitherAuditor[Int]

    // Tree:
    //
    //   1:F +-- 2:F +--  3:S
    //       |       |
    //       |       +--  4:F
    //       |
    //       +-- 5:S +--  6:F
    //               |
    //               +--  7:F
    //
    val v3 = a.success(ModelId(3), 3, Seq("e3"), Set("m3"))
    val v4 = a.failure(ModelId(4), Seq("e4"), Set("m4"))
    val v2 = a.failure(ModelId(2), Seq("e2"), Set("m2"), Seq(v3, v4))

    val v6 = a.failure(ModelId(6), Seq("e6"), Set("m6"))
    val v7 = a.failure(ModelId(7), Seq("e7"), Set("m7"))
    val v5 = a.success(ModelId(5), 5, Seq("e5"), Set("m5"), Seq(v6, v7))

    val v1 = a.failure(ModelId(1), Seq("e1"), Set("m1"), Seq(v2, v5))

    // Expected values.  Notice that all information about the
    // failures occurring deeper in the tree than a success are
    // disregarded.
    val dfsPreorder = Seq(1, 2, 4)
    val expectedFailedModelIds = dfsPreorder.map(i => ModelId(i))
    val expectedErrors         = dfsPreorder.map(i => s"e$i")
    val expectedMissing        = dfsPreorder.map(i => s"m$i").toSet


    val e = v1.left.get
    assertEquals(expectedFailedModelIds, e.failureModelIds)
    assertEquals(expectedErrors, e.errorMsgs)
    assertEquals(expectedMissing, e.missingVarNames)
  }
}

object EitherAuditorTest {
  private val S = EitherAuditor[String]
  private val I = S.changeType[Int].get
  private val D = S.changeType[Double].get

  private val SN = EitherAuditor[String](aggregateDiagnostics = false)
  private val IN = SN.changeType[Int].get
  private val DN = SN.changeType[Double].get

  private val E1 = Seq("e11", "e12")
  private val E2 = Seq("e21", "e22")
  private val E3 = Seq("e31", "e32")
  private val M1 = Set("a", "b")
  private val M2 = Set("b", "c")
  private val M3 = Set("c", "d")

  private val IdS = ModelId(1, "String")
  private val IdD = ModelId(2, "Double")
  private val IdI = ModelId(3, "Int")

  private val NoAggFailMidList = ::(IdS, Nil)
}
