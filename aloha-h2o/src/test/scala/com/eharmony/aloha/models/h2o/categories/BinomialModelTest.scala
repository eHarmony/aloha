package com.eharmony.aloha.models.h2o.categories

import java.lang.reflect.Modifier
import javassist._

import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import water.fvec.Vec
import water.util.VecUtils

import scala.language.reflectiveCalls

/**
  * Created by ryan on 5/16/16.
  */
@Ignore
@RunWith(classOf[BlockJUnit4ClassRunner])
class BinomialModelTest {
  import BinomialModelTest._

  @Test def test1(): Unit = {
//    val key = Key.make[Vec](Array(Key.VEC))
//    val key = new Key[Vec](Array(Key.VEC))
//    val key = Key.make[Vec]("my_categorical")
    val key = Vec.newKey()
//    val rowLayout = 0

//    val categories = (1 to 1000).map(_.toString)
//    val shuffledCategories = new Random(0).shuffle(categories)
//    val values = Array(1d, 2d, 3d)
//    val vec = Vec.makeCon(0d, 1L)
    val vec = new Vec(key, -1, null, Vec.T_STR)

//    val rdd = sc.parallelize(shuffledCategories.map(V1.apply))
//    val h2OFrame: H2OFrame = org.apache.spark.h2o.H2OContext.toH2OFrame(sc, rdd, None)
//    val domain = h2OFrame.domains().head
//    assertEquals(categories, domain)


//    val vec = new Vec(key, 0, null, Vec.T_STR)
//    vec.set(0, categories(0))

//    val cf = new CreateFrame
//    cf.randomize = true
//    cf.execImpl().get()

//    val vec = new Vec(key, 0, categories, Vec.T_CAT)

//    byte[] typeArr = {Vec.T_STR};
//    Vec labels = frame.lastVec().makeCons(1, 0, null, typeArr)[0]
//
//    val vec: Vec = Vec.makeZero(0, false).adaptTo(categories)
//
//    org.apache.spark.h2o._

//    val categorical: Vec = VecUtils.stringToCategorical(vec)
//    categorical.domain()
    val domain: Array[String] = CollectStringVecDomain(vec)


////    val vec: Vec = Vec.makeVec(values, categories, Vec.newKey())
//    val clazz = classOf[VecUtils].getDeclaredClasses.find(_.getSimpleName == "CollectStringVecDomain")
//    clazz.foreach { c =>
//      getClass.getClassLoader.loadClass(c.getName).newInstance()
//    }
//    // val inst = clazz.map(c => Class.forName(c.getName).newInstance())
//
//    val methods = clazz.map(_.getMethods)
//    val b = 1
//    clazz.map(_.)
//    val ctor = clazz.flatMap { c =>
//      val ctors = c.getConstructors
//
//      ctors match {
//        case cs if cs.length == 1 => cs.headOption.map { pc =>
//          pc.setAccessible(true)
//          pc
//        }
//        case _ => None
//      }
//    }

//    val instance = ctor.map(_.newInstance().asInstanceOf[{def domain(vec: Vec): Array[String]}])
//
//    val domain = instance.map(_ domain vec)


    //    val ctor = collectStringVecDomain.map(_.getConstructor())
//    val inst = ctor.map(_.newInstance().asInstanceOf[{def domain(vec: Vec): Array[String]}])
    val a = 1
//    val instance = csvdCtor.newInstance().asInstanceOf[{def domain(vec: Vec): Array[String]}]
//    val domain: Array[String] = instance.domain(vec)
//    assertEquals(Array.empty[String], domain)

    // water.util.VecUtils.toCategoricalVec(Vec)
    //   water.util.VecUtils.stringToCategorical(Vec)
    //     water.util.VecUtils.CollectStringVecDomain()
    //     water.util.VecUtils.CollectStringVecDomain.domain(Vec)

  }
}

object BinomialModelTest {
  def CollectStringVecDomain(vec: Vec) = {

    val vuClass = ClassPool.getDefault.get("water.util.VecUtils")
    val vecClass = ClassPool.getDefault.get("water.fvec.Vec")
//    val csvdClass = ClassPool.getDefault.get("water.util.VecUtils$CollectStringVecDomain")
//    csvdClass.setModifiers(javassist.Modifier.PUBLIC)

    val csvdClass = vuClass.getDeclaredClasses.filter(_.getSimpleName == "VecUtils$CollectStringVecDomain").head
    csvdClass.setModifiers(javassist.Modifier.PUBLIC)
//    csvdClasses.foreach(c => println(c.getSimpleName))

//    val cClass = classOf[String]
//    val ctClass = ClassPool.getDefault.get(cClass.getName)
//    val cVal = ""

    val dOld = csvdClass.getMethods.filter(m => m.getName == "domain" && m.getParameterTypes.toSeq == Seq(vecClass)).head

    dOld.setName("domainOld")

    val domain = CtNewMethod.copy(dOld, "domain", csvdClass, null)


    // csvdClass.removeMethod(domain)
//    val newDomain = new CtMethod(domain.getReturnType, "domain", Array(vecClass), csvdClass)
//      new CtMethod(domain.getMethodInfo, domain.getDeclaringClass)
//    newDomain.set


    val ints = 1 to 100
    val seed = 0L
    val shuffled = new scala.util.Random(seed).shuffle(ints)
    val inserts = shuffled.map { i =>
      s"""_uniques.put("$i", _placeHolder);"""
    }.mkString("\n  ")

    val body = s"""
                  |{
                  |  _uniques = new water.util.IcedHashMap();
                  |  $inserts
                  |  return domain();
                  |}
                """.stripMargin.trim

    domain.setBody(body)
    csvdClass.addMethod(domain)


//    vuClass.toClass.getMethod("toCategoricalVec", classOf[Vec]).invoke(null, vec).asInstanceOf[Array[String]]

    csvdClass.toClass.getMethod("domain", classOf[Vec]).invoke(null, vec).asInstanceOf[Array[String]]



    // Add a public constructor with a dummy type.
//    val ctr = new CtConstructor(Array(ctClass), csvdClass)
//    ctr.setModifiers(javassist.Modifier.PUBLIC)
//    ctr.setBody("{this();}")
//    csvdClass.addConstructor(ctr)



    // Get the Class, constructor, and instance.
//    val newClass = csvdClass.toClass
//    val ctor = newClass.getConstructor(cClass)
//    val instance = ctor.newInstance(cVal).asInstanceOf[{def domain(): Array[String]}]


//    val instance = newClass.newInstance().asInstanceOf[{def domain(): Array[String]}]
//    instance
  }

  case class V1(tpe: String)
//  lazy val sc = new org.apache.spark.SparkContext(new org.apache.spark.SparkConf().setMaster("local[*]").setAppName("testing"))
}