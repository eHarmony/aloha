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
      Set("redefine", "stage_poly", "keep", "permutations", "autolink"),
      UnrecoverableFlagSet
    )
  }

  @Test def testUnrecoverable(): Unit = {
    val unrec = UnrecoverableFlagSet.iterator.map { f =>
      VwMultilabelModel.updatedVwParams(s"--csoaa_ldf mc --$f", Set.empty)
    }.toList

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

  @Test def testNamespaceErrors(): Unit = {
    val args = "--wap_ldf m --ignore_linear b --ignore a -qbb -qbd " +
      "--cubic bcd --interactions dde --interactions abcde"
    val updated = updatedVwParams(args, Set())

    val exp = Left(
      NamespaceError(
        "--wap_ldf m --ignore_linear b --ignore a -qbb -qbd --cubic bcd " +
          "--interactions dde --interactions abcde",
        Set(),
        Map(
          "ignore"        -> Set('a'),
          "ignore_linear" -> Set('b'),
          "quadratic"     -> Set('b', 'd'),
          "cubic"         -> Set('b', 'c', 'd', 'e'),
          "interactions"  -> Set('a', 'b', 'c', 'd', 'e')
        )
      )
    )

    assertEquals(exp, updated)
  }

  @Test def testNoAvailableLabelNss(): Unit = {
    // All namespaces taken.
    val nss = (Char.MinValue to Char.MaxValue).map(_.toString).toSet
    val validArgs = "--csoaa_ldf mc"

    VwMultilabelModel.updatedVwParams(validArgs, nss) match {
      case Left(LabelNamespaceError(orig, nssOut)) =>
        assertEquals(validArgs, orig)
        assertEquals(nss, nssOut)
      case _ => fail()
    }
  }

  @Test def testBadVwFlag(): Unit = {
    val args = "--wap_ldf m --NO_A_VALID_VW_FLAG"

    val exp = VwError(
      args,
      "--wap_ldf m --NO_A_VALID_VW_FLAG --noconstant --csoaa_rank --ignore y",
      "unrecognised option '--NO_A_VALID_VW_FLAG'"
    )

    VwMultilabelModel.updatedVwParams(args, Set.empty) match {
      case Left(e) => assertEquals(exp, e)
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
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore cdy " +
               "--ignore_linear ab " +
               "-qYa -qYb " +
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
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testMultipleInteractions(): Unit = {
    val nss  = ('a' to 'e').map(_.toString).toSet

    val args = s"--csoaa_ldf mc --interactions ab --interactions abc " +
                "--interactions abcd --interactions abcde"

    val exp = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
              "--ignore_linear abcde " +
              "-qYa -qYb -qYc -qYd -qYe " +
              "--cubic Yab " +
              "--interactions Yabc " +
              "--interactions Yabcd " +
              "--interactions Yabcde"

    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def interactionsWithSelf(): Unit = {
    val nss = Set("a")
    val args = "--wap_ldf m -qaa --cubic aaa --interactions aaaa"
    val exp = "--wap_ldf m --noconstant --csoaa_rank --ignore y --ignore_linear a " +
              "-qYa " +
              "--cubic Yaa " +
              "--interactions Yaaa " +
              "--interactions Yaaaa"

    VwMultilabelModel.updatedVwParams(args, nss) match {
      case Right(s) => assertEquals(exp, s)
      case x => assertEquals("", x)
    }
  }
}
