package com.eharmony.matching.aloha.semantics

import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Test
import org.junit.Assert._

import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.semantics.func.{GenFunc, GeneratedAccessor, GenAggFunc}


@RunWith(classOf[BlockJUnit4ClassRunner])
class ErrorEnrichingSemanticsTest {
    import ErrorEnrichingSemanticsTest._

    /** Ensure that if we choose not to enrich the errors, we get the error provided by the function produced by
      * the semantics (that doesn't necessarily provide meaningful information b/c it's out of context).
      */
    @Test def testDontRethrowOnErrorAndMissing() {
        val sNo = PossiblySafeMapStringDoubleGetSemantics(provideSemanticsUdfException = false)
        val f = sNo.createFunction[Option[Double]](FeatureName2).right.get
        try {
            f(EmptyMap)
            fail("Should have thrown NoSuchElementException")
        }
        catch {
            case e: NoSuchElementException =>
                assertEquals("Wrong message.", s"key not found: $FeatureName", e.getMessage)
        }
    }

    /** Ensure that if we choose not to enrich the errors, we get the error provided by the function produced by
      * the semantics (that doesn't necessarily provide meaningful information b/c it's out of context).
      */
    @Test def testDontRethrowOnError() {
        val sNo = PossiblySafeMapStringDoubleApplySemantics(provideSemanticsUdfException = false)
        val f = sNo.createFunction[Double](FeatureName).right.get

        try {
            f(EmptyMap)
            fail("Should have thrown NoSuchElementException")
        }
        catch {
            case e: NoSuchElementException =>
                assertEquals("Wrong message.", s"key not found: $FeatureName", e.getMessage)
        }
    }

    /** Show that when we enrich errors and get an error when there is a missing data situation, the missing data along
      * with the errors are reported.
      */
    @Test def testRethrowOnErrorAndMissing() {
        val sYes = PossiblySafeMapStringDoubleGetSemantics(provideSemanticsUdfException = true)
        val f = sYes.createFunction[Option[Double]](FeatureName2).right.get

        try {
            f(EmptyMap)
            fail("Should have thrown SemanticsUdfException")
        } catch {
            case SemanticsUdfException(spec, ao, missing, err, cause, in) =>
                assertEquals("Incorrect input found", EmptyMap, in)
                assertEquals("Incorrect specification found", "${" + FeatureName2 + "}", spec)
                assertEquals("accessor output should have one entry", 2, ao.size)

                ao foreach {
                    case (k, v) if k == FeatureName =>  assertFalse("The feature should have resulted in failure", v.isSuccess)
                    case (k, v) if k == FeatureName2 => assertTrue(v.isSuccess)
                    case _ => fail(s"Map should only contain features: $FeatureName, $FeatureName2")
                }

                assertEquals("Incorrect missing features", List(FeatureName2), missing)
                assertEquals("Incorrect accessors erring", List(FeatureName), err)
                assertTrue("Wrong type of exception thrown.", cause.isInstanceOf[java.util.NoSuchElementException])
                assertEquals("Wrong message.", s"key not found: $FeatureName", cause.getMessage)
        }
    }

    /** Show that when we enrich errors and get an error, the error information is appropriately enriched.
      */
    @Test def testRethrowOnError() {
        val sYes = PossiblySafeMapStringDoubleApplySemantics(provideSemanticsUdfException = true)
        val f = sYes.createFunction[Double](FeatureName).right.get

        try {
            f(EmptyMap)
            fail("Should have thrown SemanticsUdfException")
        } catch {
            case SemanticsUdfException(spec, ao, missing, err, cause, in) =>
                assertEquals("Incorrect input found", EmptyMap, in)
                assertEquals("Incorrect specification found", "${" + FeatureName + "}", spec)
                assertEquals("accessor output should have one entry", 1, ao.size)

                ao foreach { case (k, v) =>
                    assertEquals("incorrect feature name found in map", FeatureName, k)
                    assertTrue("The feature should have resulted in failure", v.isFailure)
                }

                assertTrue("Missing should be empty", missing.isEmpty)
                assertEquals("Incorrect accessors erring", List(FeatureName), err)
                assertTrue("Wrong type of exception thrown.", cause.isInstanceOf[java.util.NoSuchElementException])
                assertEquals("Wrong message.", s"key not found: $FeatureName", cause.getMessage)
        }
    }

}

private object ErrorEnrichingSemanticsTest {
    val FeatureName = "missing_feature"
    val FeatureName2 = "other_missing_feature"
    val EmptyMap = Map.empty[String, Double]

    case class PossiblySafeMapStringDoubleApplySemantics(override val provideSemanticsUdfException: Boolean)
        extends UnsafeMapStringDoubleApplySemantics
        with ErrorEnrichingSemantics[Map[String, Double]]

    trait UnsafeMapStringDoubleApplySemantics extends Semantics[Map[String, Double]] {
        def close(){}
        def refInfoA = RefInfo[Map[String, Double]]
        def accessorFunctionNames: Seq[String] = Nil
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
            val cs = codeSpec.trim
            val ga = GeneratedAccessor(cs, (_: Map[String, Double])(cs), Option(s"""(_: Map[String, Double])("$cs")"""))
            val f = GenFunc.f1(ga)("${"+cs+"}", identity)
            val g = Right(f.asInstanceOf[GenAggFunc[Map[String, Double], B]])
            g
        }
    }

    case class PossiblySafeMapStringDoubleGetSemantics(override val provideSemanticsUdfException: Boolean)
        extends UnsafeMapStringDoubleGetSemantics
        with ErrorEnrichingSemantics[Map[String, Double]]

    trait UnsafeMapStringDoubleGetSemantics extends Semantics[Map[String, Double]] {
        def close(){}
        def refInfoA = RefInfo[Map[String, Double]]
        def accessorFunctionNames: Seq[String] = Nil

        /** When codeSpec is the same as FeatureName, create a 1-argument function that returns that value in the map.
          * When codeSpec differs from FeatureName, create a 1-argument function that returns that value in the map associated with codeSpec.
          * This is useful for testing RethrowingSemantics.
          * @param codeSpec specification for a function to be produced by this semantics.
          * @param default a default value in the case that the function would produce an optional type.
          * @tparam B The return type of the function.
          * @return
          */
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
            val cs = codeSpec.trim
            val ga = GeneratedAccessor(FeatureName, (_: Map[String, Double])(FeatureName), Option(s"""(_: Map[String, Double])("$FeatureName")"""))

            val g =
                if (FeatureName == cs.toLowerCase) {
                    val f1 = GenFunc.f1(ga)("${"+FeatureName+"}", identity)
                    Right(f1.asInstanceOf[GenAggFunc[Map[String, Double], B]])
                }
                else {
                    val ga2 = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option(s"""(_: Map[String, Double]).get("$cs")"""))
                    val f2 = GenFunc.f2(ga, ga2)("${"+cs+"}", (a, b) => b)
                    Right(f2.asInstanceOf[GenAggFunc[Map[String, Double], B]])
                }
            g
        }
    }
}
