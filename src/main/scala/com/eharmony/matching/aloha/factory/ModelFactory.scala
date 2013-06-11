package com.eharmony.matching.aloha.factory

import scala.language.higherKinds
import scala.util.{Success, Failure, Try}

import java.{lang => jl}

import org.apache.commons.vfs2.VFS

import spray.json.{JsValue, JsonReader}
import spray.json.pimpString

import com.eharmony.matching.aloha.factory.ex.{RecursiveModelDefinitionException, AlohaFactoryException}
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.interop.FactoryInfo


case class ModelFactory[M[-_, +_] <: Model[_, _]](modelParsers: ModelParser[M]*) extends JsValuePimpz {
    def this (modelParsers: jl.Iterable[ModelParser[M]]) = this(collection.JavaConversions.iterableAsScalaIterable(modelParsers).toSeq:_*)

    val availableParsers = modelParsers.map(p => (p.modelType, p)).toMap

    /**
      * {{{
      *
      * // OK.  Import the model from an external file.
      * val j1 = """{ "import": "file:/home/alice/model.json" }"""
      *
      *
      * // OK.  Specify the model type and model definition.
      * val j2 = """
      *            |{
      *            |  "modelType": "Constant",
      *            |  "modelId": {"id": 0, "name": ""},
      *            |  "value": 1234
      *            |}
      *          """.stripMargin.trim
      *
      * // NOT OK!  Specifying an import and including any other information in the JSON specification.
      * val j3 = """
      *            |{
      *            |  "modelType": "Constant",
      *            |  "import": "file:/home/alice/model.json"
      *            |}
      *          """.stripMargin.trim
      * }}}
      * @param json
      * @tparam A
      * @tparam B
      * @return
      */
    private[this] def parseHelper[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](
            jsonMaybe: Try[JsValue],
            fileStack: List[String],
            semantics: Option[Semantics[A]]): Try[M[A, B]] = jsonMaybe.flatMap{ json =>

        if (1 == json.asJsObject.fields.size) {
            json.s("import") map { im =>
                if (fileStack contains im)
                    recursionDetected(im :: fileStack)
                else parseHelper[A, B](foToJs(im), im :: fileStack, semantics)
            } getOrElse Failure(new AlohaFactoryException("No import field and only field: " + json.asJsObject.fields.head))
        }
        else {
            for {
                mt <- modelType(json)
                p <- modelParser[A, B](mt, semantics)
                m <- Try { p.parse(json) }
            } yield m
        }
    }

    private[this] def recursionDetected(fileStack: List[String]) =
        Failure { throw new RecursiveModelDefinitionException(fileStack) }

    /**
      *
      * @param json
      * @return Returns a VALID model type.
      */
    private[this] def modelType(json: JsValue): Try[String] =
        json.s("modelType").collect{case t if availableParsers contains t => Success(t)} getOrElse Failure(new AlohaFactoryException(
            "Bad 'modelType' field.  Must contain field 'modelType' with value as one of the available model types: " +
                availableParsers.keys.toList.sorted.mkString(", ")))

    private[this] def modelParser[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](modelType: String, semantics: Option[Semantics[A]]) =
        Try { availableParsers(modelType).getParser[M, A, B](Option(this), semantics) }

    private[this] def foToJs(vfsUrl: String) = Try {
        io.Source.fromInputStream(VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream).getLines.mkString("\n").asJson
    }

    /** Get a model of the appropriate input and output types and implementation.  This method relies on the proper
      * implicits in the calling scope.
      * @param json JSON to be parsed and translated to a model.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return A [[scala.util.Try]] statement potentially containing a subtype of Model.
      */
    def getModel[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](json: JsValue, semantics: Option[Semantics[A]] = None): Try[M[A, B]] =
        getModelAndInfo[A, B](json, semantics).map(_.model)

    /** Get a model and related information.
      *
      * @param json
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return
      */
    def getModelAndInfo[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](json: JsValue, semantics: Option[Semantics[A]] = None): Try[ModelInfo[M[A, B]]] = {
        val model: Try[M[A, B]] = parseHelper[A, B](Success(json), Nil, semantics)

        val fieldsInModel: Seq[String] = Nil

        model.map(m => ModelInfo(m, fieldsInModel))
    }

    /** Convert this untyped factory to a typed factory.
      * {{{
      * val supportedModels = Seq(ErrorModel.parser)
      * val f = ModelFactory(supportedModels).toTypedFactory[Map[String, Long], Double]
      * val json = """
      *              |{
      *              |  "modelType": "Error",
      *              |  "modelId": {"id": 1, "name": ""},
      *              |  "errors": [ "error 1" ]
      *              |}
      *            """.stripMargin
      * val model = f.fromString(json).get
      * println(model.errors)              // <-- Very cool.  Get back an ErrorModel (not just Model) from the factory.
      * }}}
      * @tparam A
      * @tparam B
      * @return
      */
    def toTypedFactory[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](semantics: Option[Semantics[A]]): TypedModelFactory[A, B, M] =
        TypedModelFactory[A, B, M](this, semantics)

    def toTypedFactory[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter]: TypedModelFactory[A, B, M] = toTypedFactory[A, B](None)

    /** JAVA VERSION to transform this untyped factory to a typed factory.
      * @param semantics a semantics object.  Can be null.
      * @param fI Explicit version of the implicit information that is passed at scala call sites.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return a TypedModelFactory of the proper input and output types.
      */
    def toTypedFactory[A, B](semantics: Semantics[A], fI: FactoryInfo[A, B]): TypedModelFactory[A, B, M] =
        toTypedFactory[A, B](Option(semantics))(fI.inRefInfo, fI.outRefInfo, fI.jsonReader, fI.scoreConverter)
}


//case class ModelFactory[M[-_, +_] <: Model[_, _]](modelParsers: ModelParser[M]*) extends JsValuePimpz {
//    def this (modelParsers: jl.Iterable[ModelParser[M]]) = this(collection.JavaConversions.iterableAsScalaIterable(modelParsers).toSeq:_*)
//
//    type WrappedInterpreter = Any
//
//    val availableParsers = modelParsers.map(p => (p.modelType, p)).toMap
//
//    /**
//     * {{{
//     *
//     * // OK.  Import the model from an external file.
//     * val j1 = """{ "import": "file:/home/alice/model.json" }"""
//     *
//     *
//     * // OK.  Specify the model type and model definition.
//     * val j2 = """
//     *            |{
//     *            |  "modelType": "Constant",
//     *            |  "modelId": {"id": 0, "name": ""},
//     *            |  "value": 1234
//     *            |}
//     *          """.stripMargin.trim
//     *
//     * // NOT OK!  Specifying an import and including any other information in the JSON specification.
//     * val j3 = """
//     *            |{
//     *            |  "modelType": "Constant",
//     *            |  "import": "file:/home/alice/model.json"
//     *            |}
//     *          """.stripMargin.trim
//     * }}}
//     * @param json
//     * @tparam A
//     * @tparam B
//     * @return
//     */
//    private[this] def parseHelper[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter](
//                                                                                         jsonMaybe: Try[JsValue],
//                                                                                         fileStack: List[String],
//                                                                                         featureInterpretter: WrappedInterpreter): Try[M[A, B]] = jsonMaybe.flatMap{ json =>
//
//        if (1 == json.asJsObject.fields.size) {
//            json.s("import") map { im =>
//                if (fileStack contains im)
//                    recursionDetected(im :: fileStack)
//                else parseHelper[A, B](foToJs(im), im :: fileStack, featureInterpretter)
//            } getOrElse Failure(new AlohaFactoryException("No import field and only field: " + json.asJsObject.fields.head))
//        }
//        else {
//            for {
//                mt <- modelType(json)
//                p <- modelParser[A, B](mt)      // Gets the parser. Need to pass the Semantics here to the
//                m <- Try { p.parse(json) }
//            } yield m
//        }
//    }
//
//    private[this] def recursionDetected(fileStack: List[String]) =
//        Failure { throw new RecursiveModelDefinitionException(fileStack) }
//
//    /**
//     *
//     * @param json
//     * @return Returns a VALID model type.
//     */
//    private[this] def modelType(json: JsValue): Try[String] =
//        json.s("modelType").collect{case t if availableParsers contains t => Success(t)} getOrElse Failure(new AlohaFactoryException(
//            "Bad 'modelType' field.  Must contain field 'modelType' with value as one of the available model types: " +
//                availableParsers.keys.toList.sorted.mkString(", ")))
//
//    private[this] def modelParser[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter](modelType: String) =
//        Try { availableParsers(modelType).getParser[A, B] }
//
//    private[this] def foToJs(vfsUrl: String) = Try {
//        io.Source.fromInputStream(VFS.getManager.resolveFile(vfsUrl).getContent.getInputStream).getLines.mkString("\n").asJson
//    }
//
//    /** Get a model of the appropriate input and output types and implementation.  This method relies on the proper
//      * implicits in the calling scope.
//      * @param json JSON to be parsed and translated to a model.
//      * @tparam A input type of the resulting model
//      * @tparam B output type of the resulting model
//      * @return A [[scala.util.Try]] statement potentially containing a subtype of Model.
//      */
//    def getModel[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter](json: JsValue): Try[M[A, B]] =
//        getModelAndInfo[A, B](json).map(_.model)
//
//    /** Get a model and related information.
//      *
//      * @param json
//      * @tparam A
//      * @tparam B
//      * @return
//      */
//    //    def getModelAndInfo[A, B](json: JsValue)(implicit ta: TypeTag[A], tb: TypeTag[B], r: JsonReader[B], c: ScoreConverter[B], s: Semantics[A]): Try[ModelInfo[M[A, B]]] = {
//    def getModelAndInfo[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter](json: JsValue): Try[ModelInfo[M[A, B]]] = {
//
//        val wrappedInterpreter: WrappedInterpreter =
//            "This should be a new WrappedInterpretter every time.  Maybe it should be lazy."
//
//        val model: Try[M[A, B]] = parseHelper[A, B](Success(json), Nil, wrappedInterpreter)
//
//        val fieldsInModel: Seq[String] = Nil
//
//        model.map(m => ModelInfo(m, fieldsInModel))
//    }
//
//    /** Convert this untyped factory to a typed factory.
//      * {{{
//      * val supportedModels = Seq(ErrorModel.parser)
//      * val f = ModelFactory(supportedModels).toTypedFactory[Map[String, Long], Double]
//      * val json = """
//      *              |{
//      *              |  "modelType": "Error",
//      *              |  "modelId": {"id": 1, "name": ""},
//      *              |  "errors": [ "error 1" ]
//      *              |}
//      *            """.stripMargin
//      * val model = f.fromString(json).get
//      * println(model.errors)              // <-- Very cool.  Get back an ErrorModel (not just Model) from the factory.
//      * }}}
//      * @tparam A
//      * @tparam B
//      * @return
//      */
//    def toTypedFactory[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter]: TypedModelFactory[A, B, M] =
//        TypedModelFactory[A, B, M](this)
//}
