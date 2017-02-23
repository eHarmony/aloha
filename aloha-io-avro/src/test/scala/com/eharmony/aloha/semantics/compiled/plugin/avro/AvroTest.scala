package com.eharmony.aloha.semantics.compiled.plugin.avro

import java.io.File
import org.apache.avro

import scala.collection.JavaConversions.asScalaBuffer
import com.eharmony.aloha.semantics.compiled.plugin.avro.AvroTest.RC_1
import org.apache.avro.Schema.Type
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{Ignore, Test}

/**
  * Created by ryan.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AvroTest {
  @Test def test2(): Unit = {
    val schemaJson =
      """
        |{
        | "namespace": "example.avro",
        | "type": "record",
        | "name": "User",
        | "fields": [
        |   { "name": "name", "type": ["null", "string"] },
        |   { "name": "profile",
        |     "type": ["null", {
        |       "type": "record",
        |       "name": "Profile",
        |       "fields": [
        |         { "name": "height", "type": ["null", "int"] },
        |         { "name": "photos",
        |           "type": [{
        |             "type": "record",
        |             "name": "Photos",
        |             "fields": [
        |               { "name": "id", "type": ["null", "long"] }
        |             ]
        |           }]
        |         }
        |       ]
        |     }]
        |   }
        | ]
        |}
      """.stripMargin


    val parser = new avro.Schema.Parser
    val userSchema = parser.parse(schemaJson)
    val profileSchema = userSchema.getField("profile").schema().getTypes.last
    val photosSchema = profileSchema.getField("photos").schema().getTypes.last

    val pho1 = new GenericData.Record(photosSchema)
    pho1.put("id", 2L)

    val pro1 = new GenericData.Record(profileSchema)
    pro1.put("height", 1)
    pro1.put("photos", pho1)

    val u1 = new GenericData.Record(userSchema)
    u1.put("name", "Jason")
    u1.put("profile", pro1)

    val datumWriter = new GenericDatumWriter[GenericRecord](userSchema)


    val f = File.createTempFile("avrotest", ".dat")
    f.deleteOnExit()

    // Serialize
    val fileWriter = new DataFileWriter(datumWriter)
    fileWriter.create(userSchema, f)
    fileWriter.append(u1)
    fileWriter.close()

    // Deserialize
    val datumReader = new GenericDatumReader[GenericRecord](userSchema)
    val dataFileReader = new DataFileReader(f, datumReader)
    val userDeserialized = dataFileReader.iterator().next()
    dataFileReader.close()

    println(userDeserialized.getSchema.getFields.map(f => (f.name(), f.pos())))
    println(userDeserialized.get("profile").asInstanceOf[GenericRecord].getSchema.getFields.map(f => (f.name(), f.pos())))

    println(userDeserialized)
    val a = 1
  }

  @Ignore @Test def test1(): Unit = {



    val schemaJson =
      """
        |{
        | "namespace": "example.avro",
        | "type": "record",
        | "name": "User",
        | "fields": [
        |   { "name": "name",             "type": ["null", "string"] },
        |   { "name": "favorite_number",  "type": ["null", "int"] },
        |   { "name": "favorite_color",   "type": ["null", "string"] },
        |   { "name": "oc_1",
        |     "type": ["null", {
        |       "name": "OC_1",
        |       "type": "record",
        |       "fields": [
        |         { "name": "ri", "type": ["int"] }
        |       ]
        |     }]
        |   },
        |   { "name": "rc_1",
        |     "type": [{
        |       "name": "RC_1",
        |       "type": "record",
        |       "fields": [
        |         { "name": "ri", "type": ["int"] }
        |       ]
        |     }]
        |   }
        | ]
        |}
      """.stripMargin

    val j2 =
      """
        |{
        |  "namespace": "com.treselle.db.model",
        |  "type": "record",
        |  "doc": "http://www.treselle.com/blog/advanced-avro-schema-design-reuse/",
        |  "name": "Order",
        |  "fields": [
        |    { "name": "order_id", "type": "long" },
        |    { "name": "customer_id", "type": "long" },
        |    { "name": "total", "type": "float" },
        |    {
        |      "name": "order_details",
        |      "type": {
        |        "type": "array",
        |        "items": {
        |          "namespace": "com.treselle.db.model",
        |          "name": "OrderDetail",
        |          "type": "record",
        |          "fields": [
        |            { "name": "quantity", "type": "int" },
        |            { "name": "total", "type": "float" },
        |            {
        |              "name": "product_detail",
        |              "type": {
        |                "namespace": "com.treselle.db.model",
        |                "type": "record",
        |                "name": "Product",
        |                "fields": [
        |                  { "name": "product_id", "type": "long" },
        |                  { "name": "product_name", "type": "string" },
        |                  { "name": "product_description", "type": [ "string", "null" ], "default": "" },
        |                  {
        |                    "name": "product_status",
        |                    "type": {
        |                      "name": "product_status",
        |                      "type": "enum",
        |                      "symbols": [ "AVAILABLE", "OUT_OF_STOCK", "ONLY_FEW_LEFT" ]
        |                    },
        |                    "default": "AVAILABLE"
        |                  },
        |                  {
        |                    "name": "product_category",
        |                    "type": {
        |                      "type": "array",
        |                      "items": "string"
        |                    }
        |                  },
        |                  { "name": "price", "type": "float" },
        |                  {
        |                    "name": "product_hash",
        |                    "type": {
        |                      "type": "fixed",
        |                      "name": "product_hash",
        |                      "size": 5
        |                    }
        |                  }
        |                ]
        |              }
        |            }
        |          ]
        |        }
        |      }
        |    }
        |  ]
        |}
      """.stripMargin

    val parser = new avro.Schema.Parser
    val schema = parser.parse(schemaJson)

    val s2 = parser.parse(j2)

    val x: GenericRecord = new GenericData.Record(schema)

    val order: GenericRecord = new GenericData.Record(s2)
    order.put("order_id", 1d)

    x.put("rc_1", RC_1(1))


    Console.println(x.get("rc_1").getClass.getCanonicalName)

    // Fields can be indexed by position within the containing record.
    val fieldAtIdx1: AnyRef = x.get(1)

    Console.println(schema.getField("oc_1").schema().getTypes.toVector.last.getField("ri").schema().getType)
    Console.println(schema.getField("rc_1").schema().getTypes.toVector.last.getField("ri").schema().getType)

    schema.getFields.map{f =>
      val name = f.name

      // Type = RECORD, ENUM, ARRAY, MAP, UNION, FIXED, STRING, BYTES, INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL;

      // Position of the field in the containing record.
      val idx = f.pos

      val s = f.schema
      val tpe = s.getType
      val types = Set.empty

      if (tpe == Type.UNION) {
        // Check for Null and one other field type.  If more than one, supported.
        // mark as nullable and descend into non-nullable type
      }
//          Set(tpe)
//        else if (tpe == Type.ARRAY)
//          s.getElementType
//        else s.getTypes.map{t => t.getType}
//      s.
      (name, types)
    }.foreach(println)

  }
}

object AvroTest {
  case class RC_1(var ri: Int)
}