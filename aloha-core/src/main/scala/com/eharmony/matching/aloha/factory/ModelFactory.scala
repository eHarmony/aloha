package com.eharmony.matching.aloha.factory

import java.{lang => jl}

import com.eharmony.matching.aloha
import com.eharmony.matching.aloha.factory.ex.{AlohaFactoryException, RecursiveModelDefinitionException}
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.matching.aloha.interop.FactoryInfo
import com.eharmony.matching.aloha.models._
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import org.reflections.Reflections
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, JsonReader}

import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

case class ModelFactory(modelParsers: ModelParser*) extends JsValuePimpz {
    def this (modelParsers: jl.Iterable[ModelParser]) = this(collection.JavaConversions.iterableAsScalaIterable(modelParsers).toSeq:_*)

    val availableParsers = modelParsers.map(p => (p.modelType, p)).toMap

    private[this] implicit val importedModelPlaceholderAstJsonFormat = jsonFormat2(ImportedModelPlaceholderAst)

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
            semantics: Option[Semantics[A]]): Try[Model[A, B]] = jsonMaybe.flatMap{ json =>

        val obj = json.asJsObject
        val fields = obj.fields
        val nFields = fields.size

        val model =
            if ((1 == nFields || 2 == nFields) && fields.contains("import")) {
                for {
                    ast <- Try { obj.convertTo[ImportedModelPlaceholderAst] }
                    im = ast.`import`
                    jsonPlaceholder <- if (!(fileStack contains im)) ast.toJsValue
                                       else recursionDetected(im :: fileStack)
                    json = jsonPlaceholder.resolveFileContents()
                    m <- parseHelper[A, B](json, im :: fileStack, semantics)
                } yield m
            }
            else {
                for {
                    mt <- modelType(json)
                    p <- modelParser[A, B](mt, semantics)
                    m <- Try { p.parse(json) }
                } yield m
            }

        model
    }

    private[this] def recursionDetected(fileStack: List[String]) =
        Failure { throw new RecursiveModelDefinitionException(fileStack) }

    /**
      * @param json JSON to be parsed and translated to a model.
      * @return Returns a VALID model type.
      */
    private[this] def modelType(json: JsValue): Try[String] =
        json.s("modelType").collect{case t if availableParsers contains t => Success(t)} getOrElse Failure(new AlohaFactoryException(
            "Bad 'modelType' field.  Must contain field 'modelType' with value as one of the available model types: " +
                availableParsers.keys.toList.sorted.mkString(", ")))

    private[this] def modelParser[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](modelType: String, semantics: Option[Semantics[A]]) =
        Try { availableParsers(modelType).getParser[A, B](this, semantics) }

    /** Get a model of the appropriate input and output types and implementation.  This method relies on the proper
      * implicits in the calling scope.
      * @param json JSON to be parsed and translated to a model.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return A [[scala.util.Try]] statement potentially containing a subtype of Model.
      */
    def getModel[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](json: JsValue, semantics: Option[Semantics[A]] = None): Try[Model[A, B]] =
        getModelAndInfo[A, B](json, semantics).map(_.model)

    /** Get a model and related information.
      * @param json JSON to be parsed and translated to a model.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return
      */
    def getModelAndInfo[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](json: JsValue, semantics: Option[Semantics[A]] = None): Try[ModelInfo[Model[A, B]]] = {
        val model: Try[Model[A, B]] = parseHelper[A, B](Success(json), Nil, semantics)

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
    def toTypedFactory[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](semantics: Semantics[A]): TypedModelFactory[A, B] =
        TypedModelFactory[A, B](this, Option(semantics))

    def toTypedFactory[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter]: TypedModelFactory[A, B] =
        TypedModelFactory[A, B](this, None)

    /** JAVA VERSION to transform this untyped factory to a typed factory.
      * @param semantics a semantics object.  Can be null.
      * @param fI Explicit version of the implicit information that is passed at scala call sites.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return a TypedModelFactory of the proper input and output types.
      */
    def toTypedFactory[A, B](semantics: Semantics[A], fI: FactoryInfo[A, B]): TypedModelFactory[A, B] =
        toTypedFactory[A, B](semantics)(fI.inRefInfo, fI.outRefInfo, fI.jsonReader, fI.scoreConverter)


    /** JAVA VERSION to transform this untyped factory to a typed factory.
      * @param fI Explicit version of the implicit information that is passed at scala call sites.
      * @tparam A input type of the resulting model
      * @tparam B output type of the resulting model
      * @return a TypedModelFactory of the proper input and output types.
      */
    def toTypedFactory[A, B](fI: FactoryInfo[A, B]): TypedModelFactory[A, B] =
        toTypedFactory[A, B](fI.inRefInfo, fI.outRefInfo, fI.jsonReader, fI.scoreConverter)


    /** Combine this model factory with ''mf''.
      * @param mf a model factory to combine
      * @throws AlohaFactoryException
      * @return a ModelFactory with all of the parsers from this ModelFactory and the parsers in ''mf''.
      */
    @throws[AlohaFactoryException](cause = "When combining multiple factories with same parser names and different implementations.")
    def combine(mf: ModelFactory): ModelFactory = {
        val keyOverlap = modelParsers.map(_.modelType).toSet intersect mf.modelParsers.map(_.modelType).toSet

        val differences = keyOverlap.foldLeft(List.empty[String])((l, k) => if (availableParsers(k) == mf.availableParsers(k)) l else k :: l)

        if (differences.nonEmpty) {
            throw new AlohaFactoryException(s"Couldn't combine ModelFactory instances because the following parsers have different implementations: ${differences.mkString(", ")}")
        }

        ModelFactory(modelParsers ++ mf.modelParsers.filterNot(keyOverlap contains _.modelType):_*)
    }
}

object ModelFactory {

    /** Provides a default factory capable of producing models defined in aloha-core.  The list of models come from
      * the knownModelParsers method.
      *
      * This is useful to simplify the process of creating a factory capable of parsing the basic models provided by
      * aloha.
      */
    def defaultFactory = ModelFactory(knownModelParsers():_*)

    /** Get the list of models on the classpath with parsers that can be used by a model factory.
      * @return
      */
    def knownModelParsers(): Seq[ModelParser] = {
        // TODO: Change this back after the package change from com.eharmony.matching.aloha to com.eharmony.aloha
        //        val reflections = new Reflections(aloha.pkgName)
        val reflections = new Reflections(aloha.pkgName, "com.eharmony.aloha")
        import scala.collection.JavaConversions.asScalaSet
        val parserProviderCompanions = reflections.getSubTypesOf(classOf[ParserProviderCompanion]).toSeq

        parserProviderCompanions.flatMap {
            case ppc if ppc.getCanonicalName.endsWith("$") =>
                Try {
                    val c = Class.forName(ppc.getCanonicalName.dropRight(1))
                    c.getMethod("parser").invoke(null) match {
                        case mp: ModelParser => mp
                        case _ => throw new IllegalStateException()
                    }
                }.toOption
            case _ => None
        }
    }
}
