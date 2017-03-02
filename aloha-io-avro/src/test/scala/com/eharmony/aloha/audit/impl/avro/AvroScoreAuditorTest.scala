package com.eharmony.aloha.audit.impl.avro

import java.io.File
import java.{lang => jl, util => ju}

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.JavaConversions.{asScalaBuffer, asScalaIterator}
import scala.util.Random
import scala.collection.{immutable => sci}

/**
  * Test that auditors are instantiated correctly and correctly audit values.
  *
  * '''Note''': This test and things like it really would be done better with a property-based
  * testing framework like ScalaCheck.
  *
  * Created by ryan on 2/27/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AvroScoreAuditorTest extends ModelSerializationTestHelper {
  import AvroScoreAuditorTest._

  private[this] implicit val rand = new Random(0)

  private[this] lazy val schema = Score.getClassSchema

  @Test def testAuditing(): Unit = {
    test(RawBools, RawBytes)
    test(RawShorts, RawInts)
    test(RawLongs, RawFloats)
    test(RawDoubles, RawStrings)

    test(RawJavaBools, RawJavaBytes)
    test(RawJavaShorts, RawJavaInts)
    test(RawJavaLongs, RawJavaFloats)
    test(RawJavaDoubles, RawStrings)

    test(Iterables, IndexedSeqs)
    test(Seqs, Vectors)
    test(Sets, Streams)
    test(Lists, RawFloats)
  }

  @Test def testInstantiation(): Unit = {
    instantiate[Boolean]()
    instantiate[Byte]()
    instantiate[Short]()
    instantiate[Int]()
    instantiate[Long]()
    instantiate[Float]()
    instantiate[Double]()
    instantiate[String]()

    instantiate[jl.Boolean]()
    instantiate[jl.Byte]()
    instantiate[jl.Short]()
    instantiate[jl.Integer]()
    instantiate[jl.Long]()
    instantiate[jl.Float]()
    instantiate[jl.Double]()
  }

  @Test def testMissingValues(): Unit = {
    val missing = Set("one", "two")
    val aud = AvroScoreAuditor[Boolean].get
    val s = aud.success(M1, true, Nil, missing, Nil, None)
    val sMissing = asScalaBuffer(s.get(MissingVarNamesField).asInstanceOf[ju.List[CharSequence]]).map(_.toString).toSet
    assertEquals(missing, sMissing)

    val f = aud.failure(M1, Nil, missing, Nil)
    val fMissing = asScalaBuffer(f.get(MissingVarNamesField).asInstanceOf[ju.List[CharSequence]]).map(_.toString).toSet
    assertEquals(missing, fMissing)
  }

  @Test def testErrors(): Unit = {
    val errors = Seq("one", "two")
    val aud = AvroScoreAuditor[Boolean].get
    val s = aud.success(M1, true, errors, Set.empty, Nil, None)
    val sMissing = asScalaBuffer(s.get(ErrorMsgsField).asInstanceOf[ju.List[CharSequence]]).map(_.toString)
    assertEquals(errors, sMissing)

    val f = aud.failure(M1, errors, Set.empty, Nil)
    val fMissing = asScalaBuffer(f.get(ErrorMsgsField).asInstanceOf[ju.List[CharSequence]]).map(_.toString)
    assertEquals(errors, fMissing)
  }

  @Test def testSerializability(): Unit = {
    val a0 = serializeDeserializeRoundTrip(AvroScoreAuditor[Short].get)
    a0.success(M1, 1.toShort)
    a0.failure(M1)

    val a1 = serializeDeserializeRoundTrip(AvroScoreAuditor[Boolean].get)
    a1.success(M1, false)
    a1.failure(M1)

    val a2 = serializeDeserializeRoundTrip(AvroScoreAuditor[sci.Iterable[Boolean]].get)
    a2.success(M2, sci.Iterable(true, false))
    a2.failure(M2)

    val a3 = serializeDeserializeRoundTrip(AvroScoreAuditor[sci.IndexedSeq[Boolean]].get)
    a3.success(M2, sci.IndexedSeq(true, false))
    a3.failure(M2)

    val a4 = serializeDeserializeRoundTrip(AvroScoreAuditor[List[Boolean]].get)
    a4.success(M2, List(true, false))
    a4.failure(M2)

    val a5 = serializeDeserializeRoundTrip(AvroScoreAuditor[sci.Seq[Boolean]].get)
    a5.success(M2, sci.Seq(true, false))
    a5.failure(M2)

    val a6 = serializeDeserializeRoundTrip(AvroScoreAuditor[Vector[Boolean]].get)
    a6.success(M2, Vector(true, false))
    a6.failure(M2)

    val a7 = serializeDeserializeRoundTrip(AvroScoreAuditor[Set[Boolean]].get)
    a7.success(M2, Set(true, false))
    a7.failure(M2)

    val a8 = serializeDeserializeRoundTrip(AvroScoreAuditor[Stream[Boolean]].get)
    a8.success(M2, Stream(true, false))
    a8.failure(M2)

    val a9 = serializeDeserializeRoundTrip(AvroScoreAuditor[sci.Iterable[Short]].get)
    a9.success(M2, sci.Iterable(1.toShort, 2.toShort))
    a9.failure(M2)
  }

  /**
    * Convenience method for `checkCastIter` where `f` is the identity function.
    * @param exp the expected result
    * @param any a value extracted from a `GenericRecord` via `get`.  This value is non-null
    *            at this point.
    * @tparam A type of the expected value.
    * @tparam Elem element type of the collection extracted from the GenericRecord.
    */
  private[this] def checkIter[A: RefInfo, Elem](exp: A, any: AnyRef): Unit =
    checkCastIter[A, Elem, Elem](exp, any)(identity[Elem])

  /**
    * Check that the value `any` is an Iterable containing the desired element type with the
    * correct elements in the correct order.
    *
    * This function takes into account the proper ordering of the elements in any, by assuming the
    * elements are ordered as in the case of a sequence.  If `A` represents a set, the value extracted
    * from the GenericRecord will be turned into a set.
    *
    * Prior to comparison, each element is transformed via the function, `f`.
    * @param exp the expected result
    * @param any a value extracted from a `GenericRecord` via `get`.  This value is non-null
    *            at this point.  It's not necessary that the value extracted from the record
    *            is non-null.  The null case is caught elsewhere in the tests.
    * @param f a function to apply to each element of the collect represented by `any`.
    * @tparam A type of the expected value.
    * @tparam Elem element type of the collection extracted from the GenericRecord.
    * @tparam C expected element type of the collection represented by `A`.
    */
  private[this] def checkCastIter[A: RefInfo, Elem, C](exp: A, any: AnyRef)(f: Elem => C): Unit = {
    // This isn't strictly necessary and should never fail because of the checks in `checkValue`,
    // but it doesn't hurt.  If this fails, there is a bug in this test, not necessarily the code
    // under test.
    assertNotNull("any should not be null.", any)

    val isSet = RefInfo[A].runtimeClass == classOf[Set[_]]
    val seq = asScalaBuffer(any.asInstanceOf[ju.List[Elem]]).map(f)
    val iter =
      if (!isSet)
        seq
      else seq.toSet
    assertEquals(exp, iter)
  }

  /**
    * Check that the value field in a given record in the audit trail is as expected.
    * @param shouldBePresent Whether the value is expected to be present in the extractedVal
    * @param exp the expected value
    * @param key an identifier used for error reporting
    * @param extractedVal a value that was extracted from a GenericRecord.get and wrapped in an Option.
    * @tparam A Type of the value.
    */
  private[this] def checkValue[A: RefInfo](shouldBePresent: Boolean, exp: A,
                                           key: String, extractedVal: Option[AnyRef]): Unit = {

    if (shouldBePresent) {
      assertTrue(s"$key should be present", extractedVal.isDefined)

      // IMPORTANT: This value is an AnyRef because the return value of GenericRecord.get is Object.
      //            This comes in below when we cast to the scala AnyVal types.  AnyVal DOES NOT
      //            extend AnyRef.  This is intentionally left here to show that we can do this and
      //            unboxing works automatically.  This validates that we can do the casting to
      //            AnyVal types in the auditor code.
      val v: AnyRef = extractedVal.get
      RefInfo[A] match {
        case RefInfo.Boolean | RefInfo.JavaBoolean => assertEquals(exp, v.asInstanceOf[Boolean])
        case RefInfo.Byte | RefInfo.JavaByte => assertEquals(exp, v.asInstanceOf[Int].toByte)
        case RefInfo.Short | RefInfo.JavaShort => assertEquals(exp, v.asInstanceOf[Int].toShort)
        case RefInfo.Int | RefInfo.JavaInteger => assertEquals(exp, v.asInstanceOf[Int])
        case RefInfo.Long | RefInfo.JavaLong => assertEquals(exp, v.asInstanceOf[Long])
        case RefInfo.Float | RefInfo.JavaFloat => assertEquals(exp.asInstanceOf[Float], v.asInstanceOf[Float], 0f)
        case RefInfo.Double | RefInfo.JavaDouble => assertEquals(exp.asInstanceOf[Double], v.asInstanceOf[Double], 0d)
        case RefInfo.String => assertEquals(exp, v.asInstanceOf[CharSequence].toString)

        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Boolean, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaBoolean, r) =>
          checkIter[A, Boolean](exp, v)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Byte, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaByte, r) =>
          checkCastIter[A, Int, Byte](exp, v)(_.toByte)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Short, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaShort, r) =>
          checkCastIter[A, Int, Short](exp, v)(_.toShort)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Int, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaInteger, r) =>
          checkIter[A, Int](exp, v)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Long, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaLong, r) =>
          checkIter[A, Long](exp, v)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Float, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaFloat, r) =>
          checkIter[A, Float](exp, v)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.Double, r) ||
                  RefInfoOps.isImmutableIterableButNotMap(RefInfo.JavaDouble, r) =>
          checkIter[A, Double](exp, v)
        case r if RefInfoOps.isImmutableIterableButNotMap(RefInfo.String, r) =>
          checkCastIter[A, CharSequence, String](exp, v)(_.toString)
        case t => fail(s"unrecognized type: ${RefInfoOps.toString(t)}")
      }
    }
    else assertTrue(s"$key shouldn't be present.", extractedVal.isEmpty)
  }

  /**
    * Check that the auditor works.
    * @param a top level value to be audited
    * @param b second level value to be audited
    * @param s1 whether the auditing at the top level should be a success (true) or failure (false)
    * @param s2 whether the auditing at the second level should be a success (true) or failure (false)
    * @param inc2 whether to include the second level score in the tree of scores
    * @param p1 the probability of the top level
    * @param p2 the probability of the second level
    * @param record the record to check
    * @tparam A type of top level data
    * @tparam B type of second level data
    */
  private[this] def check[A: RefInfo, B: RefInfo](a: A, b: B, s1: Boolean, s2: Boolean,
                                                  inc2: Boolean, p1: Option[Float], p2: Option[Float],
                                                  record: GenericRecord): Unit = {
    // Serialize and deserialize r to check r against schema.
    val r = serializeRoundTrip(schema, record).head

    // Get the subvalue and check that it is present when expected
    val subval = extractSubvalue(r)
    assertEquals("A subvalue should be present", inc2, subval.isDefined)

    // Check that the model IDs are as expected
    assertEquals(Option(M1), modelId(record))
    assertEquals(Option(M2).filter(_ => inc2), subval.flatMap(modelId))

    // Check that probabilities are properly recorded.
    checkProb(s1, p1, record)
    subval.foreach(sv => checkProb(s2, p2, sv))

    // Check that the values are as expected.
    checkValue(s1, a, ValueField, Option(r.get(ValueField)))
    checkValue(inc2 && s2, b, s"subvalue $ValueField", subval.flatMap(s => Option(s.get(ValueField))))
  }

  /**
    * Get the first subvalue which is a GenericRecord.  A `None` will be returned if the proper
    * type is not found.
    * @param r a record to check for a subvalue
    * @return
    */
  private[this] def extractSubvalue(r: GenericRecord): Option[GenericRecord] = {
    // Check types.  Don't just assume and cast.
    Option(r.get(SubvaluesField)) match {
      case Some(lst: ju.List[_]) => asScalaBuffer(lst) collectFirst { case r: GenericRecord => r }
      case _ => None
    }
  }

  /**
    * Check that probabilities are recorded correctly.  If a success is recorded, then a probability
    * may or not be present.  If a failure is recorded, no probability value should be present.  In other words
    * a probability must exist when `success && pr.isDefined` and must not exist otherwise.
    * @param success whether the auditor recorded a success
    * @param pr an optional probability.
    * @param r a record to check for the existence of the probability.
    */
  private[this] def checkProb(success: Boolean, pr: Option[Float], r: GenericRecord): Unit = {
    val recordProb = Option(r.get(ProbField)).collect{ case p: jl.Float => p.floatValue }
    (success, pr) match {
      case (true, Some(p)) => assertEquals(p, recordProb.get, 0)
      case _ => assertEquals(None, recordProb)
    }
  }

  /**
    * Get a ModelId from the GenericRecord.  If any of the data is missing, return `None`.
    * @param r a record from which a ModelId is to be extracted.
    * @return
    */
  private[this] def modelId(r: GenericRecord)= {
    for {
      mid <- Option(r.get(ModelIdField)) collect { case x: GenericRecord => x }
      id <- Option(mid.get(ModelIdIdField)) collect { case x: jl.Long => x.longValue }
      name <- Option(mid.get(ModelIdNameField)) collect { case x: CharSequence => x.toString }
    } yield com.eharmony.aloha.id.ModelId(id, name)
  }

  /**
    * Serializing round-trip ensures that the data adheres to the schema.
    * @param schema an Avro schema used to validate the data
    * @param records records to serialize.
    * @return
    */
  private[this] def serializeRoundTrip(schema: Schema, records: GenericRecord*): Seq[GenericRecord] = {
    val datumWriter = new GenericDatumWriter[GenericRecord](schema)

    val f = File.createTempFile("avrotest", ".dat")
    f.deleteOnExit()

    // Serialize
    val fileWriter = new DataFileWriter(datumWriter)
    fileWriter.create(schema, f)
    records.foreach(fileWriter.append)
    fileWriter.close()

    // Deserialize
    val datumReader = new GenericDatumReader[GenericRecord](schema)
    val dataFileReader = new DataFileReader(f, datumReader)
    val deserializedRecords = asScalaIterator(dataFileReader.iterator()).toList
    dataFileReader.close()

    deserializedRecords
  }

  /**
    * Test that data is properly audited for given types `A` and `B`.  `A` represents the top-level
    * values in the audit trail and `B` represents the values at the second level of the audit trail.
    * '''Note''': Not testing whether errorMsgs and missingVarNames
    * @param as values of `A` to audit at the top level of the audit trail.
    * @param bs values of `B` to audit at the second level of the audit trail.
    * @param rand an RNG used to create probabilities
    * @tparam A type of the top-level values being audited.
    * @tparam B type of the second-level values being audited.
    */
  private[this] def test[A: RefInfo, B: RefInfo](as: Seq[A], bs: Seq[B])(implicit rand: Random): Unit = {
    // Create auditors for the top-level and second-level types.  This mimic how the auditors will be
    // created inside and outside the factory.  That's why changeType is used.  These statements will throw
    // NoSuchElementExceptions if the auditor for either type A or B doesn't exist.
    val a1 = AvroScoreAuditor[A].get
    val a2 = a1.changeType[B].get

    // Loop over a bunch of different test cases.
    //  a: a value of A to be audited at the top level of the audit trail.
    //  b: a value of B to be audited at the second level of the audit trail.
    //  s1: whether the top level auditor should emit a success (true) or failure (false)
    //  s2: whether the second level auditor should emit a success (true) or failure (false)
    //  inc2: whether the second level value should be included in the audit trail.
    //  p1: A probability to be optionally inserted in the case of successes in the top-level auditor
    //  p2: A probability to be optionally inserted in the case of successes in the second-level auditor
    //  subvalues: the values to include the in the audit trail
    //  r: a GenericRecord representing the audit trail.
    for {
      a <- as
      b <- bs
      s1 <- Seq(true, false)
      s2 <- Seq(true, false)
      inc2 <- Seq(true, false)
      p1 = Option(rand.nextFloat()).filter(p => p > 0.5f)
      p2 = Option(rand.nextFloat()).filter(p => p < 0.5f)
      subvalues = createSubvalues(a2, b, inc2, s2, p2)
      r = createRecord(a1, a, subvalues, s1, p1)
    } check(a, b, s1, s2, inc2, p1, p2, r)
  }

  private[this] def createRecord[A](aud: AvroScoreAuditor[A], a: A,
                                    subvalues: Seq[Score], success: Boolean,
                                    prob: Option[Float]): GenericRecord = {
    if (success)
      aud.success(M1, a, Nil, Set.empty, subvalues, prob)
    else aud.failure(M1, Nil, Set.empty, subvalues)
  }

  private[this] def createSubvalues[B](aud: MorphableAuditor[Score, B, Score],
                                       value: B, includeSubValues: Boolean,
                                       success: Boolean, prob: Option[Float]) = {
    if (includeSubValues) {
      List(
        if (success)
          aud.success(M2, value, Nil, Set.empty, Nil, prob)
        else aud.failure(M2, Nil, Set.empty, Nil)
      )
    }
    else Nil
  }

  private[this] def instantiate[A: RefInfo](): Unit = {
    val auditors = Map(
      RefInfo[A] -> AvroScoreAuditor[A],
      RefInfo[List[A]] -> AvroScoreAuditor[List[A]],
      RefInfo[Vector[A]] -> AvroScoreAuditor[Vector[A]],
      RefInfo[Set[A]] -> AvroScoreAuditor[Set[A]],
      RefInfo[Stream[A]] -> AvroScoreAuditor[Stream[A]],
      RefInfo[sci.IndexedSeq[A]] -> AvroScoreAuditor[sci.IndexedSeq[A]],
      RefInfo[sci.Iterable[A]] -> AvroScoreAuditor[sci.Iterable[A]],
      RefInfo[sci.Seq[A]] -> AvroScoreAuditor[sci.Seq[A]],

      // Fails
      RefInfo[Iterable[A]] -> AvroScoreAuditor[Iterable[A]],
      RefInfo[IndexedSeq[A]] -> AvroScoreAuditor[IndexedSeq[A]],
      RefInfo[Seq[A]] -> AvroScoreAuditor[Seq[A]]
    )

    val fails = auditors.collect { case (ri, None) => ri }.toSet

    val expected = Set(RefInfo[IndexedSeq[A]], RefInfo[Iterable[A]], RefInfo[Seq[A]])
    assertEquals("found failures:", expected, fails)
  }
}

object AvroScoreAuditorTest {
  val ValueField = "value"
  val SubvaluesField = "subvalues"
  val ModelIdField = "model"
  val ModelIdIdField = "id"
  val ModelIdNameField = "name"
  val ErrorMsgsField = "errorMsgs"
  val MissingVarNamesField = "missingVarNames"
  val ProbField = "prob"

  val RawBools = Seq(true, false)
  val RawJavaBools = RawBools.map(jl.Boolean.valueOf)
  val RawBytes = Seq(1.toByte, 127.toByte)
  val RawJavaBytes = RawBytes.map(jl.Byte.valueOf)
  val RawShorts = Seq(2.toShort, Short.MaxValue)
  val RawJavaShorts = RawShorts.map(jl.Short.valueOf)
  val RawInts = Seq(3, Int.MaxValue)
  val RawJavaInts = RawInts.map(jl.Integer.valueOf)
  val RawLongs = Seq(4L, Long.MaxValue)
  val RawJavaLongs = RawLongs.map(jl.Long.valueOf)
  val RawFloats = Seq(5f, Float.MinPositiveValue)
  val RawJavaFloats = RawFloats.map(jl.Float.valueOf)
  val RawDoubles = Seq(6d, 1e200)
  val RawJavaDoubles = RawDoubles.map(jl.Double.valueOf)
  val RawStrings = Seq("Nothing", "Something")
  val Iterables = Seq(sci.Iterable(2), sci.Iterable(3, 4))
  val IndexedSeqs = Seq(sci.IndexedSeq(12), sci.IndexedSeq(13, 14))
  val Seqs = Seq(sci.Seq(112.toByte), sci.Seq(113.toByte, 114.toByte))
  val Vectors = Seq(Vector(1112f), Vector(1113f, 1114f))
  val Sets = Seq(Set(11112d), Set(11113d, 11114d))
  val Streams = Seq(Stream(111112L), Stream(111113L, 111114L))
  val Lists = Seq(List(1111112.toString), List(1111113.toString, 1111114.toString))

  val M1 = com.eharmony.aloha.id.ModelId(1, "one")
  val M2 = com.eharmony.aloha.id.ModelId(2, "two")
}
