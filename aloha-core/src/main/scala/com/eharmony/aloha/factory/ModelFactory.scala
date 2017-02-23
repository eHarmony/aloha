package com.eharmony.aloha.factory

import com.eharmony.aloha
import com.eharmony.aloha.audit.{Auditor, MorphableAuditor}
import com.eharmony.aloha.factory.ModelFactory.{InlineReader, ModelInlineReader, SubmodelInlineReader}
import com.eharmony.aloha.factory.ex.{AlohaFactoryException, RecursiveModelDefinitionException}
import com.eharmony.aloha.factory.jsext.JsValueExtensions
import com.eharmony.aloha.factory.ri2jf.{RefInfoToJsonFormat, StdRefInfoToJsonFormat}
import com.eharmony.aloha.io.{GZippedReadable, LocationLoggingReadable, ReadableByString}
import com.eharmony.aloha.io.multiple.{MultipleAlohaReadable, SequenceMultipleReadable}
import com.eharmony.aloha.io.sources.ReadableSource
import com.eharmony.aloha.models.{Model, Submodel}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.util.Logging
import org.reflections.Reflections
import spray.json.DefaultJsonProtocol.{StringJsonFormat, jsonFormat2, optionFormat}
import spray.json.{CompactPrinter, JsObject, JsValue, JsonFormat, JsonReader, RootJsonFormat, pimpString}

import scala.util.{Failure, Success, Try}



/**
  * A `ModelFactory` is responsible for creating models from model specifications.
  *
  * {{{
  * // ===  SCALA CODE  ========================================================
  *
  * val semantics: Semantics[A] = ???
  * // For instance OptionAuditor[Double]() is a MorphableAuditor[Opiton[_], Double, Option[Double]]
  * val auditor: MorphableAuditor[U, N, B] = ???
  * val json: String = ???
  *
  * val factory = ModelFactory.defaultFactory(semantics, auditor)
  * val modelTry: Try[Model[A, B]] = factory.fromString(json)     // safe
  * val model: Model[A, B] = modelTry.get                         // unsafe
  * }}}
  *
  *
  * '''NOTE''': `ModelFactory` has been completely rewritten.  There's no longer a notion of an untyped model factory.
  *
  * From Scala, it is recommended to use code (like the code above), as type information is retained.
  *
  * From Java, the easiest method of instantiation is to use Spring to instantiate the factory.
  *
  * {{{
  * // ===  JAVA CODE  ========================================================
  * // Set up Spring to inject class ...
  * public class X {
  *     // Injected from Spring
  *     @Resource
  *     private AlohaReadable<Try<Model<TestProto, Double>>> modelFactory = null;
  *
  *     public Model<TestProto, Double> getModelFromClasspath(String path) {
  *         return modelFactory.fromClasspath(path).get();
  *     }
  * }
  * }}}
  *
  *
  * @param semantics
  * @param auditor
  * @param parsers
  * @param refInfoToJsonFormat
  *
  * @tparam U
  * @tparam N
  * @tparam A
  * @tparam B
  */
case class ModelFactory[U, N, A, B <: U](
    semantics: Semantics[A],
    auditor: MorphableAuditor[U, N, B],
    parsers: Seq[ModelParser],
    refInfoToJsonFormat: RefInfoToJsonFormat)
   (implicit refInfo: RefInfo[N])
   extends ReadableByString[Try[Model[A, B]]]
      with GZippedReadable[Try[Model[A, B]]]
      with LocationLoggingReadable[Try[Model[A, B]]]
      with MultipleAlohaReadable[Try[Model[A, B]]]
      with SequenceMultipleReadable[ReadableSource, Try, Model[A, B]]
      with Logging { self =>

  // TODO: Determine if defs are OK instead of transient lazy values.

  // TODO: Maybe make constructor private and have a factory method that returns a Try based on the existence of JsonFormat[N].
  // Make sure to throw early if the JSON format cannot be found.
  @transient private[this] implicit lazy val jsonFormatN: JsonFormat[N] = {
    refInfoToJsonFormat[N] match {
      case Some(jf) => jf
      case _ =>
        throw new AlohaFactoryException(
          s"Couldn't find JsonFormat for ${RefInfoOps.toString[N]}." +
          "Consider changing refInfoToJsonFormat")
    }
  }

  @transient private[this] lazy val availableParsers = parsers.map(p => (p.modelType, p)).toMap

  private[this] implicit val importedModelPlaceholderAstJsonFormat: RootJsonFormat[ImportedModelPlaceholderAst] =
    jsonFormat2(ImportedModelPlaceholderAst)

  @transient private[aloha] lazy val submodelFactory: SubmodelFactory[U, A] = new SubmodelFactory[U, A] {
    override def submodel[SN](json: JsValue)(implicit r: RefInfo[SN]): Try[Submodel[SN, A, U]] =
      self.submodel(json)(r)
    override def jsonFormat[M: RefInfo]: Option[JsonFormat[M]] = refInfoToJsonFormat[M]
  }

  /**
    * The recommended API method from Java.  It's also equally valid to use any of the other methods provided by
    * [[com.eharmony.aloha.io.AlohaReadable]] trait.  It throws away the kind information (encoded in the
    * type parameter M) that is retained the in Scala environment.
    * @param s String representation of the model.
    * @return [[http://www.scala-lang.org/api/current/index.html#scala.util.Try scala.util.Try]] of a
    *         [[com.eharmony.aloha.models.Model]][A, B].
    */
  def fromString(s: String): Try[Model[A, B]] = model(s.parseJson)

  private[this] def model(json: JsValue): Try[Model[A, B]] = json match {
    case obj: JsObject =>
      val reader = ModelInlineReader(submodelFactory, semantics, auditor)
      parse(Success(obj), reader)
    case _ => Failure(new AlohaFactoryException(s"json is not a JSON object"))
  }

  private[this] def submodel[SN](json: JsValue)(implicit r: RefInfo[SN]): Try[Submodel[SN, A, U]] = json match {
    case obj: JsObject =>
      refInfoToJsonFormat[SN] match {
        case Some(jf) =>
          auditor.changeType[SN] match {
            case Some(aud) =>
              val inlineReader = SubmodelInlineReader(submodelFactory, semantics, aud)(r, jf)
              parse(Success(obj), inlineReader)(r, jf)
            case None =>
              Failure(new AlohaFactoryException(s"Couldn't find an auditor for ${RefInfoOps.toString[SN]}.  Given submodel: ${json.toString(jsonPrinter)}."))
          }
        case None =>
          Failure(new AlohaFactoryException(
            s"Couldn't find JsonFormat for ${RefInfoOps.toString[N]}." +
              "Consider changing refInfoToJsonFormat. "))
      }
    case _ =>
      Failure(new AlohaFactoryException(s"submodel json is not a JSON object.  Given: ${json.toString(jsonPrinter)}"))
  }


  // TODO: Determine if it's worth adding this back in.
//  /** Combine this model factory with ''mf''.
//    * @param mf a model factory to combine
//    * @throws AlohaFactoryException
//    * @return a ModelFactory with all of the parsers from this ModelFactory and the parsers in ''mf''.
//    */
//  @throws[AlohaFactoryException](cause = "When combining multiple factories with same parser names and different implementations.")
//  def combine(mf: NewModelFactory): NewModelFactory = {
//    val keyOverlap = modelParsers.map(_.modelType).toSet intersect mf.modelParsers.map(_.modelType).toSet
//
//    val differences = keyOverlap.foldLeft(List.empty[String])((l, k) => if (availableParsers(k) == mf.availableParsers(k)) l else k :: l)
//
//    if (differences.nonEmpty) {
//      throw new AlohaFactoryException(s"Couldn't combine ModelFactory instances because the following parsers have different implementations: ${differences.mkString(", ")}")
//    }
//
//    ModelFactory(modelParsers ++ mf.modelParsers.filterNot(keyOverlap contains _.modelType):_*)
//  }

  // TODO: Create a custom printer that truncates string to be shorter for reporting
  private[this] def jsonPrinter: JsValue => String = CompactPrinter


  // json: JsValue, fileStack: List[String], auditor: MorphableAuditor[U, N, B]
  // TODO: Consider using scala.util.control.TailCalls for trampolining to avoid stack overflows.
  /**
    *
    * From old documentation in ModelFactory
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
    * @param jsonMaybe
    * @param inlineReader
    * @param fileStack
    * @tparam M
    * @tparam C
    * @tparam Y
    * @return
    */
  private[this] def parse[M, C <: U, Y](
      jsonMaybe: Try[JsObject], // TODO: Should be a Try[JsObject]
      inlineReader: InlineReader[U, M, A, C, Y],
      fileStack: List[String] = Nil)
     (implicit r: RefInfo[M], jf: JsonFormat[M]): Try[Y] = jsonMaybe match {

    case Failure(f) => Failure(f)
    case Success(json) =>
      val fields = json.fields
      val nFields = fields.size

      val model =
        if ((1 == nFields || 2 == nFields) && fields.contains("import"))
          readImportedModel(json, fileStack, inlineReader)
        else readInlineModel(json, inlineReader)

      model
    }

  private[this] def readInlineModel[M, C <: U, Y](json: JsObject, inlineReader: InlineReader[U, M, A, C, Y])
                                              (implicit r: RefInfo[M], jf: JsonFormat[M]): Try[Y] = {
    for {
      p <- modelParser(json)
      jr <- inlineReader.jsonReader(p)
      m <- Try { json.convertTo(jr) }
    } yield m
  }

  private[this] def readImportedModel[M, C <: U, Y](obj: JsObject, fileStack: List[String], inlineReader: InlineReader[U, M, A, C, Y])
                                                (implicit r: RefInfo[M], jf: JsonFormat[M]): Try[Y] = {
    for {
      ast <- Try { obj.convertTo[ImportedModelPlaceholderAst] }
      im = ast.`import`
      jsonPlaceholder <- if (!(fileStack contains im))
                           ast.toJsValue
                         else recursionDetected(im :: fileStack)
      json = jsonPlaceholder.resolveFileContents()
      m <- parse(json, inlineReader, im :: fileStack)
    } yield m
  }

  private[this] def recursionDetected(fileStack: List[String]): Try[Nothing] =
    Failure { new RecursiveModelDefinitionException(fileStack) }

  /**
    * @param json JSON to be parsed and translated to a model.
    * @return Returns a VALID model type.
    */
  private[this] def modelParser(json: JsObject): Try[ModelParser] = {
    val parser = json.s("modelType").flatMap(t => availableParsers.get(t))

    parser.map(p => Success(p)) getOrElse {
      Failure(new AlohaFactoryException(
        "Bad 'modelType' field.  Must contain field 'modelType' with value as one of the available model types: " +
          availableParsers.keys.toList.sorted.mkString(", ")))
    }
  }
}

object ModelFactory {

  /** Provides a default factory capable of producing models defined in aloha-core.  The list of models come from
    * the knownModelParsers method.
    *
    * This is useful to simplify the process of creating a factory capable of parsing the basic models provided by
    * aloha.
    */
  def defaultFactory[U, N, A, B <: U](
      semantics: Semantics[A],
      auditor: MorphableAuditor[U, N, B]
  )(implicit refInfo: RefInfo[N]): ModelFactory[U, N, A, B] = {
    ModelFactory(semantics, auditor, knownModelParsers(), new StdRefInfoToJsonFormat)
  }

  /** Get the list of models on the classpath with parsers that can be used by a model factory.
    * @return
    */
  def knownModelParsers(): Seq[ModelParser] = {
    val reflections = new Reflections(aloha.pkgName)
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

  private sealed trait InlineReader[U, N, -A, +B <: U, Y] {
    def jsonReader(parser: ModelParser): Try[JsonReader[_ <: Y]]
  }

  private case class ModelInlineReader[U, N: RefInfo: JsonFormat, A, B <: U](
      factory: SubmodelFactory[U, A],
      semantics: Semantics[A],
      auditor: Auditor[U, N, B]
  ) extends InlineReader[U, N, A, B, Model[A, B]] {
    override def jsonReader(parser: ModelParser): Try[JsonReader[_ <: Model[A, B]]] = parser match {
      case plugin: ModelParsingPlugin =>
        Try { plugin.modelJsonReader(factory, semantics, auditor) } flatMap { jr =>
          jr.
            map { Success.apply } .
            getOrElse {
              Failure(new AlohaFactoryException(
                s"Couldn't find JsonReader to parse model of type ${parser.modelType}. Check that " +
                  s"${parser.modelType} can have a natural type of ${RefInfoOps.toString[N]}"))
            }
        }
      case p => Failure(new AlohaFactoryException(s"${p.modelType} cannot be used to parse submodels, only top level models."))
    }
  }

  private case class SubmodelInlineReader[U, N: RefInfo: JsonFormat, A, B <: U](
      factory: SubmodelFactory[U, A],
      semantics: Semantics[A],
      auditor: Auditor[U, N, B]
  ) extends InlineReader[U, N, A, B, Submodel[N, A, U]] {
    override def jsonReader(parser: ModelParser): Try[JsonReader[_ <: Submodel[N, A, U]]] = parser match {
      case plugin: SubmodelParsingPlugin =>
        Try { plugin.submodelJsonReader(factory, semantics, auditor) } flatMap { jr =>
          jr.
            map { Success.apply } .
            getOrElse {
              Failure(new AlohaFactoryException(
                s"Couldn't find JsonReader to parse submodel of type ${parser.modelType}. Check that " +
                s"${parser.modelType} can have a natural type of ${RefInfoOps.toString[N]}"))
            }
        }
      case p => Failure(new AlohaFactoryException(s"${p.modelType} cannot be used to parse top level models, only submodels"))
    }
  }
}
