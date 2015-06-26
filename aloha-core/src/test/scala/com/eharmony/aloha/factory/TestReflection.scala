package com.eharmony.aloha.factory

import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import org.junit.{Ignore, Test}
import com.eharmony.aloha.reflect.RefInfo

@RunWith(classOf[JUnit4ClassRunner])
class TestReflection {

    @Ignore
    @Test def test1() {
//        val f = new IntFactory[java.util.Map[java.lang.String, java.lang.Integer]](
//            new TypeResolver().resolve(new GenericType[java.util.Map[java.lang.String, java.lang.Integer]]{})
//        )
//
//        val model = f.factory.fromString(
//            """
//              |{
//              |  "modelType": "Error",
//              |  "modelId": {"id": 1, "name": ""},
//              |  "errors": [
//              |    "error 1"
//              |  ]
//              |}
//            """.stripMargin).get
//
//        val m = mapAsJavaMap(Map("one" -> jl.Integer.valueOf(1)))
//        val s = model.score(m)
    }

    def getRefInfo[A](a: A)(implicit t: RefInfo[A]) = t
}
