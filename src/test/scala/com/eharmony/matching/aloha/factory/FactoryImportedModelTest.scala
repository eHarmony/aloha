package com.eharmony.matching.aloha.factory

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import spray.json.DefaultJsonProtocol._
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._

import com.eharmony.matching.aloha.models.{ConstantModel, ErrorModel}
import com.eharmony.matching.aloha.factory.ex.{AlohaFactoryException, RecursiveModelDefinitionException}

@RunWith(classOf[BlockJUnit4ClassRunner])
class FactoryImportedModelTest {

    @Test(expected = classOf[RecursiveModelDefinitionException])
    def test1CycleDetected() {
        ModelFactory(ErrorModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/cycle1_A.json").
            get
    }

    @Test(expected = classOf[RecursiveModelDefinitionException])
    def test2CycleDetected() {
        ModelFactory(ErrorModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/cycle2_A.json").
            get
    }

    @Test(expected = classOf[RecursiveModelDefinitionException])
    def test3CycleDetected() {
        ModelFactory(ErrorModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/cycle3_A.json").
            get
    }

    @Test def test1LevelSuccessDefault() {
        val m = ModelFactory(ConstantModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/success_1_level_default.json").
            get
        assertEquals(1, m(null).get)
    }

    @Test def test1LevelSuccessVfs1() {
        val m = ModelFactory(ConstantModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/success_1_level_vfs1.json").
            get
        assertEquals(3, m(null).get)
    }

    @Test def test1LevelSuccessVfs2() {
        val m = ModelFactory(ConstantModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/success_1_level_vfs2.json").
            get
        assertEquals(4, m(null).get)
    }

    @Test def test1LevelSuccessFile() {
        val m = ModelFactory(ConstantModel.parser).
            toTypedFactory[Any, Int].
            fromResource("com/eharmony/matching/aloha/factory/success_1_level_file.json").
            get
        assertEquals(2, m(null).get)
    }

    @Test def test1LevelApproprivateFailureWithDefaultProtocol() {
        try {
            ModelFactory(ErrorModel.parser).
                toTypedFactory[Any, Int].
                fromResource("com/eharmony/matching/aloha/factory/bad_reference_default.json").
                get
            fail()
        }
        catch {
            case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't resolve VFS2 file"))
            case e: Exception => fail()
        }
    }

    @Test def test1LevelApproprivateFailureWithVfs1() {
        try {
            ModelFactory(ErrorModel.parser).
                toTypedFactory[Any, Int].
                fromResource("com/eharmony/matching/aloha/factory/bad_reference_vfs1.json").
                get
            fail()
        }
        catch {
            case e: AlohaFactoryException => assertTrue(e.getMessage, e.getMessage.startsWith("Couldn't resolve VFS1 file"))
            case e: Exception => fail()
        }
    }

    @Test def test1LevelApproprivateFailureWithVfs2() {
        try {
            ModelFactory(ErrorModel.parser).
                toTypedFactory[Any, Int].
                fromResource("com/eharmony/matching/aloha/factory/bad_reference_vfs2.json").
                get
            fail()
        }
        catch {
            case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't resolve VFS2 file"))
            case e: Exception => fail()
        }
    }

    @Test def test1LevelApproprivateFailureWithFile() {
        try {
            ModelFactory(ErrorModel.parser).
                toTypedFactory[Any, Int].
                fromResource("com/eharmony/matching/aloha/factory/bad_reference_file.json").
                get
            fail()
        }
        catch {
            case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't get JSON for file"))
            case e: Exception => fail()
        }
    }
}
