package com.eharmony.aloha.audit.impl.avro

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type.RECORD
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.commons.io.IOUtils
import java.{util => ju}
import scala.collection.JavaConversions.collectionAsScalaIterable

/**
  * Created by deak on 2/26/17.
  */
sealed abstract class AvroGenericRecordAuditor[N] extends MorphableAuditor[GenericRecord, N, GenericRecord]

object AvroGenericRecordAuditor extends Serializable {
  @transient private[this] lazy val AuditorSchema: Schema = {
    val parser = new Schema.Parser()
    val is = getClass.getClassLoader.getResourceAsStream("com/eharmony/aloha/audit/impl/avro/generic_record_auditor.avsc")
    try {
      parser.parse(is)
    }
    finally {
      IOUtils.closeQuietly(is)
    }
  }

  @transient private[this] lazy val ModelIdSchema: Schema = {
    // get at the end blows up (on purpose).
    collectionAsScalaIterable(AuditorSchema.getField("model").schema().getTypes).toSeq.find(s => s.getType == RECORD).get
  }

  // Force the retrieval of the lazy val at creation time
  require(AuditorSchema != null && ModelIdSchema != null)

  def apply[N](implicit ri: RefInfo[N]): Option[AvroGenericRecordAuditor[N]] = {
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
      case r if RefInfoOps.isIterable(RefInfo.Boolean, r) => opt(IdentityArrayAuditor[Boolean]())
      case r if RefInfoOps.isIterable(RefInfo.Byte, r) => opt(IdentitySubTypeArrayAuditor[Byte, Int])
      case r if RefInfoOps.isIterable(RefInfo.Short, r) => opt(IdentitySubTypeArrayAuditor[Short, Int])
      case r if RefInfoOps.isIterable(RefInfo.Int, r) => opt(IdentityArrayAuditor[Int]())
      case r if RefInfoOps.isIterable(RefInfo.Long, r) => opt(IdentityArrayAuditor[Long]())
      case r if RefInfoOps.isIterable(RefInfo.Float, r) => opt(IdentityArrayAuditor[Float]())
      case r if RefInfoOps.isIterable(RefInfo.Double, r) => opt(IdentityArrayAuditor[Double]())
      case r if RefInfoOps.isIterable(RefInfo.String, r) => opt(IdentityArrayAuditor[String]())

      case _ => None
    }
  }

  /**
    * Ensures the return values are a List.  This is used instead of JavaConversions because
    * while java Iterables are OK to provide, we want to maintain the semantics of ordering.
    *
    * @param it an Iterable
    * @tparam A type of element in Iterable and returned Java List.
    * @return a Java List with the same elements as the iterable.
    */
  private[this] def toJList[A](it: Iterable[A]): ju.List[A] = {
    val lst = new ju.ArrayList[A]()
    it.foreach(x => lst.add(x))
    lst
  }

  private[this] def toCastJList[A, B](it: Iterable[A])(implicit f: A => B): ju.List[B] = {
    val lst = new ju.ArrayList[B]()
    it.foreach(x => lst.add(f(x)))
    lst
  }

  /**
    * Cast `auditor` to `ScoreAuditor[N]` and wrap in an Option.
    * '''NOTE''': ''I hate this method'' but I don't want to require an implicit
    * [[AvroGenericRecordAuditor]] as a parameter in the apply method because [[Impl]]'s
    * `changeType` method would then need the same implicit to be included.  A determination
    * should be made as to the feasibility of this strategy.
    *
    * @param auditor an auditor to wrap
    * @tparam N type of auditor to be returned
    * @return
    */
  private[this] def opt[N](auditor: AvroGenericRecordAuditor[_]): Option[AvroGenericRecordAuditor[N]] =
    Option(auditor.asInstanceOf[AvroGenericRecordAuditor[N]])

  private[this] sealed trait Impl[N] extends AvroGenericRecordAuditor[N] {

    override final def changeType[M: RefInfo]: Option[AvroGenericRecordAuditor[M]] = AvroGenericRecordAuditor[M]

    protected[this] def convertToWire(value: N): Any

    override final def failure(key: ModelIdentity,
                               errorMsgs: => Seq[String],
                               missingVarNames: => Set[String],
                               subValues: Seq[GenericRecord]): GenericRecord = {

      val r = new GenericData.Record(AuditorSchema)
      r.put("model", mId(key))

      // TODO: Should these be null if iterables are empty?
      // TODO: Should there be an option to accumlate errors and missing variables from submodels?
      r.put("errorMsgs", toJList(errorMsgs))
      r.put("subvalues", toJList(subValues))

      // TODO: Should we care about ordering here?
      r.put("missingVarNames", toJList(missingVarNames.toVector.sorted))

      r
    }

    override final def success(key: ModelIdentity,
                               valueToAudit: N,
                               errorMsgs: => Seq[String] = Nil,
                               missingVarNames: => Set[String] = Set.empty,
                               subValues: Seq[GenericRecord] = Nil,
                               prob: => Option[Float] = None): GenericRecord = {
      val r = new GenericData.Record(AuditorSchema)
      r.put("model", mId(key))
      r.put("value", convertToWire(valueToAudit))
      prob.foreach(p => r.put("prob", p))

      // TODO: Should these be null if iterables are empty?
      // TODO: Should there be an option to accumlate errors and missing variables from submodels?
      r.put("errorMsgs", toJList(errorMsgs))
      r.put("subvalues", toJList(subValues))

      // TODO: Should we care about ordering here?
      r.put("missingVarNames", toJList(missingVarNames.toVector.sorted))

      r
    }


    private[this] def mId(modelId: ModelIdentity) = {
      val mId = new GenericData.Record(ModelIdSchema)
      mId.put("id", modelId.getId())
      mId.put("name", modelId.getName())
      mId
    }
  }

  private[this] case class IdentityAuditor[A]() extends Impl[A] {
    protected[this] def convertToWire(value: A): A = value
  }

  private[this] case class IdentitySubTypeAuditor[A, B](implicit f: A => B) extends Impl[A] {
    protected[this] def convertToWire(value: A): B = f(value)
  }

  private[this] case class IdentityArrayAuditor[A]() extends Impl[Iterable[A]] {
    override protected[this] def convertToWire(value: Iterable[A]): ju.List[A] = toJList(value)
  }

  private[this] case class IdentitySubTypeArrayAuditor[A, B](implicit f: A => B) extends Impl[Iterable[A]] {
    override protected[this] def convertToWire(value: Iterable[A]): ju.List[B] = toCastJList(value)(f)
  }
}