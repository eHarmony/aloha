package com.eharmony.matching.aloha.factory

import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import org.junit.Test

import spray.json.DefaultJsonProtocol._
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._

import com.eharmony.matching.aloha.models.ErrorModel
import com.eharmony.matching.aloha.factory.ex.RecursiveModelDefinitionException

@RunWith(classOf[JUnit4ClassRunner])
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
}
