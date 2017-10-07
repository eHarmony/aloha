package com.eharmony.aloha.models.vw.jni.multilabel

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan.deak on 10/6/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelParamAugmentationTest extends VwMultilabelParamAugmentation {

  @Test def testNotCsoaaWap(): Unit = {
    val args = ""
    VwMultilabelModel.updatedVwParams(args, Set.empty) match {
      case Left(NotCsoaaOrWap(ps)) => assertEquals(args, ps)
      case _ => fail()
    }
  }

  @Test def testExpectedUnrecoverableFlags(): Unit = {
    assertEquals(
      "Unrecoverable flags has changed.",
      Set("redefine", "stage_poly", "keep", "permutations"),
      UnrecoverableFlagSet
    )
  }

  @Test def testUnrecoverable(): Unit = {
    val unrec = UnrecoverableFlagSet.iterator.map { f =>
      VwMultilabelModel.updatedVwParams(s"--csoaa_ldf mc --$f", Set.empty)
    }

    unrec foreach {
      case Left(UnrecoverableParams(p, us)) =>
        assertEquals(
          p,
          us.map(u => s"--$u")
            .mkString("--csoaa_ldf mc ", " ", "")
        )
      case _ => fail()
    }
  }

  @Test def testIgnoredNotInNsSet(): Unit = {
    val args = "--csoaa_ldf mc --ignore a"
    val origNss = Set.empty[String]
    VwMultilabelModel.updatedVwParams(args, origNss) match {
      case Left(NamespaceError(o, nss, bad)) =>
        assertEquals(args, o)
        assertEquals(origNss, nss)
        assertEquals(Map("ignore" -> Set('a')), bad)
      case _ => fail()
    }
  }

  @Test def testIgnoredNotInNsSet2(): Unit = {
    val args = "--csoaa_ldf mc --ignore ab"
    val origNss = Set("a")
    VwMultilabelModel.updatedVwParams(args, origNss) match {
      case Left(NamespaceError(o, nss, bad)) =>
        assertEquals(args, o)
        assertEquals(origNss, nss)
        assertEquals(Map("ignore" -> Set('b')), bad)
      case _ => fail()
    }
  }

  @Test def testQuadraticCreation(): Unit = {
    val args = "--csoaa_ldf mc"
    val nss = Set("abc", "bcd")

    // Notice: ignore_linear and quadratics are in sorted order.
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
               "--ignore_linear ab -qYa -qYb"
    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testIgnoredNoQuadraticCreation(): Unit = {
    val args = "--csoaa_ldf mc --ignore_linear a"
    val nss  = Set("abc", "bcd")

    // Notice: ignore_linear and quadratics are in sorted order.
    val exp = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
              "--ignore_linear ab -qYb"

    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testCubicCreation(): Unit = {
    val args = "--csoaa_ldf mc -qab --quadratic cb"
    val nss  = Set("abc", "bcd", "cde", "def")
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
               "--ignore_linear abcd " +
               "-qYa -qYb -qYc -qYd " +
               "--cubic Yab --cubic Ybc"

    // Notice: ignore_linear and quadratics are in sorted order.
    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testCubicCreationIgnoredLinear(): Unit = {
    val args = "--csoaa_ldf mc -qab --quadratic cb --ignore_linear d"
    val nss  = Set("abc", "bcd", "cde", "def")
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
               "--ignore_linear abcd " +
               "-qYa -qYb -qYc " +
               "--cubic Yab --cubic Ybc"

    // Notice: ignore_linear and quadratics are in sorted order.
    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testCubicCreationIgnored(): Unit = {
    val args = "--csoaa_ldf mc -qab --quadratic cb --ignore c"
    val nss  = Set("abc", "bcd", "cde", "def")
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore cy " +
               "--ignore_linear abd " +
               "-qYa -qYb -qYd " +
               "--cubic Yab"

    // Notice: ignore_linear and quadratics are in sorted order.
    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testCubicWithInteractionsCreationIgnored(): Unit = {
    val args = "--csoaa_ldf mc --interactions ab --interactions cb --ignore c --ignore d"
    val nss  = Set("abc", "bcd", "cde", "def")
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore cy " +
               "--ignore_linear abd " +
               "-qYa -qYb -qYd " +
               "--cubic Yab"

    // Notice: ignore_linear and quadratics are in sorted order.
    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testHigherOrderInteractions(): Unit = {
    val args = "--csoaa_ldf mc --interactions abcd --ignore_linear abcd"
    val nss  = Set("abc", "bcd", "cde", "def")
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
               "--ignore_linear abcd " +
               "--interactions Yabcd"

    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s.replaceAll(" +", " "))
      case _ => fail()
    }
  }

  // TODO: More VW argument augmentation tests!!!

}
