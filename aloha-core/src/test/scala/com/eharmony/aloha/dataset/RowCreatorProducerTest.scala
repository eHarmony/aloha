package com.eharmony.aloha.dataset

import java.lang.reflect.Modifier

import com.eharmony.aloha
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import org.junit.Assert._
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.JavaConversions.asScalaSet
import org.reflections.Reflections

@RunWith(classOf[BlockJUnit4ClassRunner])
class RowCreatorProducerTest {
    import RowCreatorProducerTest._

    private[this] def scanPkg = aloha.pkgName + ".dataset"

    @Test def testAllRowCreatorProducersHaveOnlyZeroArgConstructors() {
        val reflections = new Reflections(scanPkg)
        val specProdClasses = reflections.getSubTypesOf(classOf[RowCreatorProducer[_, _, _]]).toSet
        specProdClasses.foreach { clazz =>
            val cons = clazz.getConstructors
            assertTrue(s"There should only be one constructor for ${clazz.getCanonicalName}.  Found ${cons.length} constructors.", cons.length <= 1)
            cons.headOption.foreach { c =>
                if (!(WhitelistedRowCreatorProducers contains clazz)) {
                    val nParams = c.getParameterTypes.length
                    assertEquals(s"The constructor for ${clazz.getCanonicalName} should take 0 arguments.  It takes $nParams.", 0, nParams)
                }
            }
        }
    }

    /**
     * This is ignored because we moved Producers inside the companion objects and even when the companion object
     * and the Producer itself is made final, it is not made final at the JVM level.  This seems like a pretty big
     * scala bug.
     */
    // TODO: Report the above bug!
    @Ignore @Test def testAllRowCreatorProducersAreFinalClasses() {
        val reflections = new Reflections(scanPkg)
        val specProdClasses = reflections.getSubTypesOf(classOf[RowCreatorProducer[_, _, _]]).toSet
        specProdClasses.foreach { clazz =>
            assertTrue(s"${clazz.getCanonicalName} needs to be declared final.", Modifier.isFinal(clazz.getModifiers))
        }
    }
}

object RowCreatorProducerTest {
    private val WhitelistedRowCreatorProducers = Set[Class[_]](
        classOf[VwMultilabelRowCreator.Producer[_, _]]
    )
}
