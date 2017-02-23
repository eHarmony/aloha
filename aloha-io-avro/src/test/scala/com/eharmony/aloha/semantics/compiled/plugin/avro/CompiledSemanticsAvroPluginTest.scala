package com.eharmony.aloha.semantics.compiled.plugin.avro

import java.io.File

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import org.apache.avro
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.JavaConversions.{asJavaCollection, asScalaBuffer, asScalaIterator}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CompiledSemanticsAvroPluginTest {
  import CompiledSemanticsAvroPluginTest._

  @Test def testCompilation(): Unit = {
    val plugin = CompiledSemanticsAvroPlugin[GenericRecord](UserSchema, dereferenceAsOptional = false)
    val semantics = CompiledSemantics(
      compiler = new TwitterEvalCompiler(),
      plugin = plugin,
      imports = Seq.empty)

    val user = users(Seq(
      User(
        name = Option("ryan"),
        kidsAges = Seq(7, 3),
        profile = Option(Profile(
          height = Some(72),
          photos = Photos(Some(6)))))
    )).head

    val name = semantics.createFunction[Option[CharSequence]]("Option(${name})", Some(None)).right.get
    val kidAges = semantics.createFunction[Seq[Int]]("${kid_ages}").right.get
    val profile = semantics.createFunction[Option[GenericRecord]]("Option(${profile})", Some(None)).right.get
    val height = semantics.createFunction[Option[Int]]("Option(${profile.height})", Some(None)).right.get
    val photoId = semantics.createFunction[Option[Long]]("Option(${profile.photos.id})", Some(None)).right.get
    val firstKidAge = semantics.createFunction[Int]("${kid_ages[0]}").right.get

    assertEquals("ryan", name(user).get.toString)
    assertEquals(Seq(7, 3), kidAges(user))
    assertTrue(profile(user).isDefined)
    assertEquals(72, height(user).get)
    assertEquals(6L, photoId(user).get)
    assertEquals(7, firstKidAge(user))
  }

  @Test def test1(): Unit = {
    val plugin = CompiledSemanticsAvroPlugin[GenericRecord](UserSchema)

    val userRecords = users(Seq(
      User(
        name = Option("ryan"),
        kidsAges = Seq(3),
        profile = Option(Profile(height = None, photos = Photos(None)))
      )
    ))

    val kidAges = plugin.accessorFunctionCode("kid_ages")
    val name = plugin.accessorFunctionCode("name")
    val profile = plugin.accessorFunctionCode("profile")
    val height = plugin.accessorFunctionCode("profile.height")
    val photos = plugin.accessorFunctionCode("profile.photos")
    val photoId = plugin.accessorFunctionCode("profile.photos.id")
    val a = 1

//    Seq(kidAges, name, profile, height, photos, photoId) foreach { c =>
//      println(c.right.get.pretty + "\n\n")
//    }

//    println(s"${name.right.get.pretty}\n$profile\n$height\n$photos\n$photoId")

    val user: GenericRecord = new GenericData.Record(UserSchema)

    // Apparently, the default is present but not automatically populated in a GenericRecord
    Console.println(user.getSchema.getField("name").defaultVal())

    user.getSchema.getField("kid_ages")

    Console.println("kid ages: " + user.get("kid_ages"))

    Console.println(user.get("name"))

//    val kidAgesFn =
//      (_0: org.apache.avro.generic.GenericRecord) => {
//        import scala.collection.JavaConversions.asScalaBuffer
//        _0.get("kid_ages").asInstanceOf[java.util.List[Int]].toSeq
//      }
//
//    val nameFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get("name")).asInstanceOf[Option[String]]
//
//    val profileFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get("profile")).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]]
//
//    val heightFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get("profile")).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].flatMap(_1 =>
//          Option(_1.get("height")).asInstanceOf[Option[Int]])
//
//    val photosFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get("profile")).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].map(_1 =>
//          _1.get("photos").asInstanceOf[org.apache.avro.generic.GenericRecord])
//
//    val photoIdFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get("profile")).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].flatMap(_1 =>
//          Option(_1.get("photos").asInstanceOf[org.apache.avro.generic.GenericRecord].get("id")).asInstanceOf[Option[Long]])

//
//    val kidAgesFn =
//      (_0: org.apache.avro.generic.GenericRecord) => {
//        import scala.collection.JavaConversions.asScalaBuffer
//        _0.get(1).asInstanceOf[java.util.List[Int]].toSeq
//      }
//
//    val nameFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get(0)).asInstanceOf[Option[String]]
//
//    val profileFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get(2)).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]]
//
//    val heightFn: (GenericRecord) => Option[Int] =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get(2)).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].flatMap(_1 =>
//          Option(_1.get(0)).asInstanceOf[Option[Int]])
//
//    val photosFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get(2)).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].map(_1 =>
//          _1.get(1).asInstanceOf[org.apache.avro.generic.GenericRecord])
//
//    val photoIdFn =
//      (_0: org.apache.avro.generic.GenericRecord) =>
//        Option(_0.get(2)).asInstanceOf[Option[org.apache.avro.generic.GenericRecord]].flatMap(_1 =>
//          Option(_1.get(1).asInstanceOf[org.apache.avro.generic.GenericRecord].get(0)).asInstanceOf[Option[Long]])

//    val results: Seq[(Seq[Int], Option[String], Option[GenericRecord], Option[Int], Option[GenericRecord], Option[Long])] = userRecords.map(u =>
//      (kidAgesFn(u), nameFn(u), profileFn(u), heightFn(u), photosFn(u), photoIdFn(u))
//    )


    // (Buffer(3),Some(ryan),Some({"height": null, "photos": {"id": null}}),None,Some({"id": null}),None) // field names
    // (Buffer(3),Some(ryan),Some({"height": null, "photos": {"id": null}}),None,Some({"id": null}),None) // indexes
  }
}

object CompiledSemanticsAvroPluginTest {
  private val SchemaJson =
    """
      |{
      | "namespace": "example.avro",
      | "type": "record",
      | "name": "User",
      | "fields": [
      |   { "name": "name", "type": ["string", "null"], "default": "Name not given" },
      |   { "name": "kid_ages",
      |     "type": {
      |       "type": "array",
      |       "items": "int"
      |     }
      |   },
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

  private lazy val UserSchema = {
    val parser = new avro.Schema.Parser
    parser.parse(SchemaJson)
  }

  // ============================================================
  //  Class hierarchy mirroring the Schema in the above JSON
  // ============================================================
  private case class Photos(id: Option[Long])
  private case class Profile(height: Option[Int], photos: Photos)
  private case class User(name: Option[String], kidsAges: Seq[Int], profile: Option[Profile])

  private def users(us: Seq[User]): Seq[GenericRecord] = {
    val profileSchema = UserSchema.getField("profile").schema().getTypes.last
    val photosSchema = profileSchema.getField("photos").schema().getTypes.last

    val userRecords = us.map { u =>
      val userData = new GenericData.Record(UserSchema)

      u.profile foreach { pro =>
        val proData = new GenericData.Record(profileSchema)

        val phoData = new GenericData.Record(photosSchema)
        pro.photos.id.foreach(id => phoData.put("id", id))
        proData.put("photos", phoData)

        pro.height.foreach(h => proData.put("height", h))
        userData.put("profile", proData)
      }

      u.name.foreach(name => userData.put("name", name))
      userData.put("kid_ages", asJavaCollection(u.kidsAges))
      userData
    }

    val datumWriter = new GenericDatumWriter[GenericRecord](UserSchema)

    val f = File.createTempFile("avrotest", ".dat")
    f.deleteOnExit()

    // Serialize
    val fileWriter = new DataFileWriter(datumWriter)
    fileWriter.create(UserSchema, f)
    userRecords.foreach(fileWriter.append)
    fileWriter.close()

    // Deserialize
    val datumReader = new GenericDatumReader[GenericRecord](UserSchema)
    val dataFileReader = new DataFileReader(f, datumReader)
    val usersDeserialized = dataFileReader.iterator().toVector
    dataFileReader.close()
    usersDeserialized
  }
}