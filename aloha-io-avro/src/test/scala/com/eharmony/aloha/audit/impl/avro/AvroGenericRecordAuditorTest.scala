package com.eharmony.aloha.audit.impl.avro

import java.io.File

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.JavaConversions.asScalaIterator
import scala.util.Random

/**
  * Created by deak on 2/27/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AvroGenericRecordAuditorTest {
  import AvroGenericRecordAuditorTest._

  private[this] lazy val schema = {
    val is = VFS.getManager.resolveFile("res:com/eharmony/aloha/audit/impl/avro/generic_record_auditor.avsc").getContent.getInputStream
    try {
      new Schema.Parser().parse(is)
    }
    finally {
      IOUtils.closeQuietly(is)
    }
  }


  @Test def testAuditing(): Unit = {
    test(rawBools, rawBytes)
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
  }

  /**
    * Check that the auditor works
    * @param a top level value to be audited
    * @param b second level value to be audited
    * @param s1 whether the auditing at the top level should be a success (true) or failure (false)
    * @param s2 whether the auditing at the second level should be a success (true) or failure (false)
    * @param inc2 whether to include the second level score in the tree of scores
    * @param p1 the probability of the top level
    * @param p2 the probability of the second level
    * @param r the record to check
    * @tparam A type of top level data
    * @tparam B type of second level data
    */
  private[this] def check[A: RefInfo, B: RefInfo](a: A, b: B, s1: Boolean, s2: Boolean, inc2: Boolean,
                                                  p1: Option[Float], p2: Option[Float], r: GenericRecord): Unit = {

    // Serialize and deserialize r to check r against schema.
    serializeRoundTrip(schema, r).head

    // TODO: do additional checks
    //    assertEquals("", r.toString)
  }

  /**
    * Serializing round-trip ensures that the data adheres to the schema.
    * @param schema
    * @param records
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

  private[this] def test[A: RefInfo, B: RefInfo](_1: Seq[A], _2: Seq[B], seed: Long = 0L): Unit = {
    val m1 = ModelId(1, "one")
    val m2 = ModelId(2, "two")

    val a1 = AvroGenericRecordAuditor[A].get
    val a2 = a1.changeType[B].get
    val r = new Random(seed)

    for {
      a <- _1
      b <- _2
      s1 <- Seq(true, false)
      s2 <- Seq(true, false)
      inc2 <- Seq(true, false)
      p1 = Option(r.nextFloat()).filter(p => p > 0.5f)
      p2 = Option(r.nextFloat()).filter(p => p < 0.5f)
    } {

      val subs =
        if (inc2) {
          List(
            if (s2)
              a2.success(m2, b, Nil, Set.empty, Nil, p2)
            else a2.failure(m2, Nil, Set.empty, Nil)
          )
        }
        else Nil

      val r =
        if (s1)
          a1.success(m1, a, Nil, Set.empty, subs, p1)
        else a1.failure(m1, Nil, Set.empty, subs)

      check(a, b, s1, s2, inc2, p1, p2, r)
    }
  }


  private[this] def instantiate[A: RefInfo](): Unit = {
    val auditors = Map(
      RefInfo[A] -> AvroGenericRecordAuditor[A],
      RefInfo[Iterable[A]] -> AvroGenericRecordAuditor[Iterable[A]],
      RefInfo[IndexedSeq[A]] -> AvroGenericRecordAuditor[IndexedSeq[A]],
      RefInfo[List[A]] -> AvroGenericRecordAuditor[List[A]],
      RefInfo[Seq[A]] -> AvroGenericRecordAuditor[Seq[A]],
      RefInfo[Vector[A]] -> AvroGenericRecordAuditor[Vector[A]],
      RefInfo[Set[A]] -> AvroGenericRecordAuditor[Set[A]],
      RefInfo[Stream[A]] -> AvroGenericRecordAuditor[Stream[A]]
    )

    val fails = auditors.collect{ case (ri, None) => RefInfoOps.toString(ri) }.toList

    assertEquals("found failures:", Nil, fails)
  }
}

object AvroGenericRecordAuditorTest {
  val rawBools = Seq(true, false)
  val rawBytes = Seq(1.toByte, 127.toByte)
  val rawShorts = Seq(2.toShort, Short.MaxValue)
  val rawInts = Seq(3, Int.MaxValue)
  val rawLongs = Seq(4L, Long.MaxValue)
  val rawFloats = Seq(5f, Float.MinPositiveValue)
  val rawDoubles = Seq(6d, 1e200)
  val rawStrings = Seq("Nothing", "Something")
  val Iterables = Seq(Iterable(2), Iterable(3, 4))
  val IndexedSeqs = Seq(IndexedSeq(12), Iterable(13, 14))
  val Seqs = Seq(Seq(112.toByte), Seq(113.toByte, 114.toByte))
  val Vectors = Seq(Vector(1112f), Vector(1113f, 1113f))
  val Sets = Seq(Set(11112d), Set(11113d, 11113d))
  val Streams = Seq(Stream(111112L), Set(111113L, 111113L))
  val Lists = Seq(List(1111112.toString), List(1111113.toString, 1111114.toString))
}
