package com.eharmony.matching.notaloha

import scala.language.{ higherKinds, implicitConversions }

import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import org.junit.Test
import Model.WrappedModel
import com.eharmony.aloha.reflect.{ RefInfoOps, RefInfo }

// Case classes don't need a new to be instantiated which is their main advantage here
case class ModelId(id: Long, name: String)

// We get a simple model here through a singleton
object ModelId {
  def get = ModelId(id(), "")
  def id() = System.nanoTime.toString.reverse.dropWhile(_ == '0').reverse.toLong // MJS: nanoTime guarantees unicity
}

// Definiton of a model
trait Model[-A, +B] {
  def apply(a: A): B
  val modelId: ModelId
}

// Singleton to create a wrapper for a model object
object Model {
  implicit def fToModel[A: RefInfo, B: RefInfo](f: A => B): WrappedModel[A, B] = WrappedModel(f)
  case class WrappedModel[-A: RefInfo, +B: RefInfo](f: A => B) extends Model[A, B] {
    val modelId = ModelId.get
    def apply(a: A) = f(a)
  }
}

// Used to defined composite models i.e models mapping other models based on their id
trait CompositeModelLike[-A, +B] extends Model[A, Option[B]] {
  val submodels: Seq[Model[_, _]]
  lazy val subModelIntIds = submodels.map(_.modelId.id)
  val values: Seq[B]
}

// More complex definition of a composite model
case class CompositeModel[-A, B, +C](
  m1: Model[A, B],
  m2: Model[B, Int],
  func: PartialFunction[Int, C],
  modelId: ModelId = ModelId.get) extends CompositeModelLike[A, C] {
  val submodels = Seq(m1, m2)
  val values = Iterator.range(1, 10000).map(func).toSeq
  def apply(a: A) = m2(m1(a)) match {
    case i if func isDefinedAt i => Option(values(i))
    case _ => None
  }
}

trait ModelCreator[+M[-_, +_] <: Model[_, _]] {
  def create[A <: AnyVal: RefInfo, B <: AnyVal: RefInfo]: M[A, B]
}

// Corrected here by removing the covariant property of M as it would be redundant to <: i.e M[-_, +_] is a "producer" of Model[_, _] eg can insert itself in a list of Model[_, _]
case class X[M[-_, +_] <: Model[_, _]](mc: ModelCreator[M]) {
  def create[A <: AnyVal: RefInfo, B <: AnyVal: RefInfo]: M[A, B] = mc.create[A, B]
}

case object CompositeModelCreator extends ModelCreator[({ type M[-A, +B] = CompositeModel[A, _, B] })#M] {
  val intTypes = Set(RefInfo.Byte, RefInfo.Short, RefInfo.Int, RefInfo.Long).map(RefInfoOps.toString(_))

  def pf[D](size: Int, to: Int => D) = new PartialFunction[Int, D] {
    def isDefinedAt(i: Int) = 0 <= i && i < size
    def apply(i: Int) = to(i)
  }

  def create[A <: AnyVal: RefInfo, B <: AnyVal: RefInfo]: CompositeModel[A, _, B] = {
    val a = RefInfoOps.toString[A]
    val b = RefInfoOps.toString[B]
    val m =
      if (intTypes contains a) {
        val f1 = (_: A).toString.toLong
        val f2 = (_: Long).toInt
        b match {
          case "Double" => CompositeModel(f1, f2, pf(1000, _.toDouble))
          case "Float" => CompositeModel(f1, f2, pf(1000, _.toFloat))
          case "Byte" => CompositeModel(f1, f2, pf(1000, _.toByte))
          case "Short" => CompositeModel(f1, f2, pf(1000, _.toShort))
          case "Int" => CompositeModel(f1, f2, pf(1000, _.toInt))
          case "Long" => CompositeModel(f1, f2, pf(1000, _.toLong))
          case "Boolean" => CompositeModel(f1, f2, pf(1000, _ % 2 == 0))
          case _ => throw new Exception("unknown B type: " + b)
        }
      } else throw new Exception("unknown A type: " + a)
    m.asInstanceOf[CompositeModel[A, _, B]]
  }
}

case object WrappedModelCreator extends ModelCreator[Model.WrappedModel] {
  def create[A <: AnyVal: RefInfo, B <: AnyVal: RefInfo]: Model.WrappedModel[A, B] = {
    val m = (RefInfoOps.toString[A], RefInfoOps.toString[B]) match {
      case ("Double", "Double") => Model.WrappedModel(identity[Double])
      case ("Int", "Int") => Model.WrappedModel(identity[Int])
      case ("Double", "Int") => Model.WrappedModel((_: Double).toInt)
      case _ => throw new IllegalArgumentException
    }

    m.asInstanceOf[Model.WrappedModel[A, B]]
  }
}

@RunWith(classOf[JUnit4ClassRunner])
class TestX {
  @Test def test1() {
    val c = X(CompositeModelCreator)
    val w: X[WrappedModel] = X(WrappedModelCreator)

    // Notice no errors
    val cid = c.create[Int, Double]
    println(cid.submodels)
    println(cid.subModelIntIds)
    println(cid.values)
    println(cid.modelId)
    1 to 10 map (i => println(cid(i)))
  }
}
