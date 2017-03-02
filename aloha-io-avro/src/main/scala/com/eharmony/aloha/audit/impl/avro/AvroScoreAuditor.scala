package com.eharmony.aloha.audit.impl.avro

import java.{lang => jl, util => ju}

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}

import scala.collection.JavaConversions.seqAsJavaList

/**
  * An auditor for encoding information in a tree structure as an Avro `org.apache.avro.generic.GenericRecord`.
  *
  * See the resources in [[com.eharmony.aloha.audit.impl.avro]] for the Avro schema.
  *
  * Created by ryan on 2/26/17.
  *
  * @tparam N the natural output type of a model whose data is to be audited.
  */
sealed abstract class AvroScoreAuditor[N]
  extends MorphableAuditor[Score, N, Score]
     with Serializable

object AvroScoreAuditor extends Serializable {
  def apply[N](implicit ri: RefInfo[N]): Option[AvroScoreAuditor[N]] = {
    ri match {
      case RefInfo.Boolean => opt(IdentityAuditor[Boolean]())
      case RefInfo.Byte => opt(IdentitySubTypeAuditor[Byte, Int])
      case RefInfo.Short => opt(IdentitySubTypeAuditor[Short, Int])
      case RefInfo.Int => opt(IdentityAuditor[Int]())
      case RefInfo.Long  => opt(IdentityAuditor[Long]())
      case RefInfo.Float => opt(IdentityAuditor[Float]())
      case RefInfo.Double => opt(IdentityAuditor[Double]())

      // TODO: Should a String really be inserted to the record or a CharSequence like a avro.util.Utf8?
      case RefInfo.String => opt(IdentityAuditor[String]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Boolean, r) =>
        opt(IdentityArrayAuditor[Boolean]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Byte, r) =>
        opt(IdentitySubTypeArrayAuditor[Byte, Int])
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Short, r) =>
        opt(IdentitySubTypeArrayAuditor[Short, Int])
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Int, r) =>
        opt(IdentityArrayAuditor[Int]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Long, r) =>
        opt(IdentityArrayAuditor[Long]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Float, r) =>
        opt(IdentityArrayAuditor[Float]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Double, r) =>
        opt(IdentityArrayAuditor[Double]())
      case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.String, r) =>
        opt(IdentityArrayAuditor[String]())

      case _ => None
    }
  }

  /**
    * Cast `auditor` to `AvroGenericRecordAuditor[N]` and wrap in an Option.
    * '''NOTE''': ''I hate this method'' but I don't want to require an implicit
    * [[AvroScoreAuditor]] as a parameter in the apply method because [[Impl]]'s
    * `changeType` method would then need the same implicit to be included.  A determination
    * should be made as to the feasibility of this strategy.
    *
    * @param auditor an auditor to wrap
    * @tparam N type of auditor to be returned
    * @return
    */
  private[this] def opt[N](auditor: AvroScoreAuditor[_]): Option[AvroScoreAuditor[N]] =
    Option(auditor.asInstanceOf[AvroScoreAuditor[N]])

  private[this] sealed trait Impl[N] extends AvroScoreAuditor[N] {

    override final def changeType[M: RefInfo]: Option[AvroScoreAuditor[M]] = AvroScoreAuditor[M]

    protected[this] def convertToWire(value: N): Any

    override final def failure(key: ModelIdentity,
                               errorMsgs: => Seq[String],
                               missingVarNames: => Set[String],
                               subValues: Seq[Score]): Score = {
      new Score(
        new ModelId(key.getId(), key.getName()),
        null,
        seqAsJavaList(subValues),
        seqAsJavaList(errorMsgs),
        seqAsJavaList(missingVarNames.toVector),
        null
      )
    }

    override final def success(key: ModelIdentity,
                               valueToAudit: N,
                               errorMsgs: => Seq[String] = Nil,
                               missingVarNames: => Set[String] = Set.empty,
                               subValues: Seq[Score] = Nil,
                               prob: => Option[Float] = None): Score = {
      new Score(
        new ModelId(key.getId(), key.getName()),
        convertToWire(valueToAudit),
        seqAsJavaList(subValues),
        seqAsJavaList(errorMsgs),
        seqAsJavaList(missingVarNames.toVector),
        prob.map(jl.Float.valueOf).orNull
      )
    }
  }

  private[this] case class IdentityAuditor[A]() extends Impl[A] {
    protected[this] def convertToWire(value: A): A = value
  }

  private[this] case class IdentitySubTypeAuditor[A, B](implicit f: A => B) extends Impl[A] {
    protected[this] def convertToWire(value: A): B = f(value)
  }

  /**
    * Converter from Scala `Iterable`s to a `java.util.List`s.
    *
    * @tparam A Element type in the Iterable passed to `convertToWire`.
    */
  private[this] case class IdentityArrayAuditor[A]() extends Impl[Iterable[A]] {
    /**
      * Convert a Scala Iterable to a `java.util.List`.
      *
      * '''IMPORTANT''': the output type is java.util.List.  This is important because we want
      * the iteration order to be imposed.  Since Seq[_] extends Iterable[_], sequence as well
      * ordered.  If the Iterable `value` is not a sequence, we convert it to a sequence to
      * establish an guaranteed iteration order.  If `value` is already a sequence, its iteration
      * ordering won't change.
      * @param value a Scala Iterable to convert to a `java.util.List`.
      * @return
      */
    override protected[this] def convertToWire(value: Iterable[A]): ju.List[A] = seqAsJavaList(value.toSeq)
  }

  /**
    * Converter from Scala `Iterable`s to a `java.util.List`s.
    *
    * The elements of the Iterable `value` in `convertToWire` are first mapped via `f`.  Then the
    * resulting Iterable is turned into a sequence and converted to a `java.util.List`.  The consequence
    * of mapping then turning to a sequence is that if `f` maps multiple elements from the domain to
    * one element in the codomain, then the outputted Java List may be shorter than the original Iterable.
    * Care must be taken in the choice of `f`.
    *
    * @param f a function to map elements of type `A` to type `B`.  This is useful for boxing, etc.
    * @tparam A Element type in the Iterable passed to `convertToWire`.
    * @tparam B Element type in the resulting `java.util.List` returned by `convertToWire`.
    */
  private[this] case class IdentitySubTypeArrayAuditor[A, B](implicit f: A => B) extends Impl[Iterable[A]] {

    /**
      * '''IMPORTANT''': the output type is java.util.List.  This is important because we want
      * the iteration order to be imposed.  Since Seq[_] extends Iterable[_], sequence as well
      * ordered.  If the Iterable `value` is not a sequence, we convert it to a sequence to
      * establish an guaranteed iteration order.  If `value` is already a sequence, its iteration
      * ordering won't change.
      * @param value a Scala Iterable to convert to a `java.util.List`.
      * @return
      */
    override protected[this] def convertToWire(value: Iterable[A]): ju.List[B] = seqAsJavaList(value.map(f).toSeq)
  }
}
