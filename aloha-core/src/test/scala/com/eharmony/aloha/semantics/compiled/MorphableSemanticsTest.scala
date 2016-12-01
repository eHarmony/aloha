package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.reflect.{RefInfoOps, RefInfo}
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.semantics.{MorphableSemantics, Semantics}
import com.eharmony.aloha.test.proto.Testing.{PhotoProto, UserProto}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan on 9/4/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class MorphableSemanticsTest {
  import MorphableSemanticsTest._

  private[this] val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

  @Test def testMorphingWorksAndSemanticsWorksAfterMorphing(): Unit = {
    implicit val sUserProto: Semantics[UserProto] = CompiledSemantics(
      compiler,
      CompiledSemanticsProtoPlugin[UserProto],
      Seq("scala.math._"))

    val u = userProtoExample

    // Create some functions with the user protocol buffer semantics.
    val userId = createUserIdFn(sUserProto)
    val photos = createPhotosFn(sUserProto)

    // Morph to a different kind of protobuf.  Use the new semantics to create a function.
    val sPhotoProto = morphedSemantics[PhotoProto](sUserProto)
    val height = createPhotoHeightFn(sPhotoProto)

    // Compose the functions.
    val heights = photos.andThen(p => p.map(height).flatten)

    // Assert the composed function works.
    assertEquals(Seq(1, 3), heights(u))

    // Test that we can go back and use the original semantics.
    val userId2 = createUserIdFn(sUserProto)
    assertEquals(userId, userId2)
  }

  /**
    * An example of how to morph semantics.
    * @param s semantics to change
    * @tparam B type to change to which the Semantics should be changed.
    * @return
    */
  private[this] def morphedSemantics[B: RefInfo](s: Semantics[UserProto]): Semantics[B] = {
    val sO: Option[Semantics[B]] = s match {
      case ms: MorphableSemantics[_, _] => ms.morph[B]
      case _ => None
    }

    // This is unsafe but for illustrative purposes.
    sO.get
  }


  private[this] def createUserIdFn(s: Semantics[UserProto]): GenAggFunc[UserProto, Long] =
    s.createFunction[Long]("${id}").right.get

  private[this] def createPhotosFn(s: Semantics[UserProto]): GenAggFunc[UserProto, Seq[PhotoProto]] =
     s.createFunction[Seq[PhotoProto]]("${photos}").right.get

  private[this] def createPhotoHeightFn(s: Semantics[PhotoProto]): GenAggFunc[PhotoProto, Option[Int]] =
    s.createFunction[Option[Int]]("Option(abs(${height}))", Some(None)).right.get


  private[this] def userProtoExample = {
    UserProto.newBuilder.
      setId(1).
      addPhotos(PhotoProto.newBuilder.setId(1).setHeight(1)).
      addPhotos(PhotoProto.newBuilder.setId(2)).
      addPhotos(PhotoProto.newBuilder.setId(3).setHeight(3)).
      build
  }

  @Test def testSem(): Unit = {
    assertFalse(new NoTypeParamSem().morph[Float].isDefined)
    assertTrue(new NoTypeParamSem().morph[Double].isDefined)
  }
}

object MorphableSemanticsTest {

  /**
    * An example of a MorphableSemantics class '''not parameterized by type'''.
    * The focus here is just the types necessary.  Notice the type lambdas and casting.
    */
  class NoTypeParamSem extends NonFunctionalSemantics[Double]
                          with MorphableSemantics[({type L[_] = NoTypeParamSem})#L, Double] {
    override def refInfoA = RefInfo[Double]
    override def morph[B: RefInfo] =
      if (RefInfoOps.isSubType[B, Double])
        // Need to cast `this` to the proper type.  Could create a new one if desired.
        Option(this.asInstanceOf[MorphableSemantics[({type L[_] = NoTypeParamSem})#L, B]])
      else None
  }

  trait NonFunctionalSemantics[A] extends Semantics[A] {
    override def accessorFunctionNames = Seq.empty
    override def createFunction[B: RefInfo](codeSpec: String, default: Option[B]) = Left(Nil)
    override def close(): Unit = ()
  }
}
