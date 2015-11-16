package com.eharmony.aloha.models

import scala.util.Try
import java.io.{ByteArrayOutputStream, PrintStream}
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.factory.{ParserProviderCompanion, ModelFactory, ModelParser}
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.{SemanticsUdfException, Semantics}
import com.eharmony.aloha.factory.ex.AlohaFactoryException

/** A model that swallows exceptions thrown by submodel.
  *
  * When exceptions are thrown by ''submodel'', then a score containing a ScoreError should be produces where the error
  * list should be either:
  *
  - com.eharmony.aloha.models.ErrorSwallowingModel.errorMsg(ex)
  - [Exception error message ...]
  - [Stack trace here ...]
  *
  * when recordErrorStackTraces and the stack trace and message could be retrieved; otherwise, the error list
  * should be:
  *
  - com.eharmony.aloha.models.ErrorSwallowingModel.errorMsg(ex)
  - ''com.eharmony.aloha.models.ErrorSwallowingModel.ExMsgThrewMsg''
  - ''com.eharmony.aloha.models.ErrorSwallowingModel.StackTraceOmitted''
  *
  * If the exception that is caught is a [[com.eharmony.aloha.semantics.SemanticsUdfException]], then
  * 3 additional fields are added to the end of the errors list in indices 3, 4, 5:
  *
  - The specification of the feature in error.
  - The feature accessors in the feature that are in err.
  - The feature accessors in the feature that are missing an output value.
  *
  * @param submodel the submodel to which the score calculations are delegated.
  * @param recordErrorStackTraces whether stack traces should be recorded.
  * @tparam A model input type
  * @tparam B model output type
  */
case class ErrorSwallowingModel[-A, +B](submodel: Model[A, B], recordErrorStackTraces: Boolean = true) extends BaseModel[A, B] {
    val modelId = submodel.modelId

    private[this] type ReturnVal = PartialFunction[Throwable, Try[(ModelOutput[B], Option[Score])]]

    private[aloha] def getScore(a: A)(implicit audit: Boolean) = {
        // Delegate the scoring to the submodel but trap exceptions.
        val sTry = Try { submodel.getScore(a) }

        // If we shouldn't record stack traces, just work off the original Try.  If we should, then
        // attempt to map the exception to a failure with a stack trace.  Then be super careful when
        // performing a final recovery attempt with defaultError.  This method should not allow
        // Throwables to pass.
        val s = (if (!recordErrorStackTraces) sTry else sTry recoverWith errorWithTrace).recoverWith { defaultError }.get
        s
    }

    /** Attempt to take the Throwable and convert to the output type with a stack trace.
      *
      */
    private[models] def errorWithTrace(implicit audit: Boolean): ReturnVal = {
        case ex: SemanticsUdfException[A] => Try { failure(errors = basicErrorInfo(ex) ++ extendedErrorInfo(ex)) }
        case ex: Throwable => Try { failure(errors = basicErrorInfo(ex)) }
    }

    private[models] def basicErrorInfo(ex: Throwable) = Seq(errorMsg(ex), getMessageFrom(ex), getStackTrace(ex))

    private[models] def extendedErrorInfo(ex: SemanticsUdfException[A]) = {
        val spec = Option(ex.specification) map { s => s"specification in error: $s" } getOrElse "no specification provided"
        val err = Option(ex.accessorsInErr) map { e => e.mkString("accessors in error: ", ", ", "") } getOrElse "no accessorsInErr provided"
        val missing = Option(ex.accessorsMissingOutput) map { e => e.mkString("accessors missing output: ", ", ", "") } getOrElse "no accessorsMissingOutput provided"
        Seq(spec, err, missing)
    }

    /** This needs to be impervious to Throwables so it needs to be coded very carefully.  That is,
      * it doesn't trust the exception class at all.
      */
    private[models] def defaultError(implicit audit: Boolean): ReturnVal = {
        case ex: Throwable => Try {
            // Being super protective here to avoid the getMessage throwing exception (such as
            // a message that calls toString on an infinite depth tree that would cause a stack
            // overflow).
            failure(errors = Seq(errorMsg(ex), getMessageFrom(ex), ErrorSwallowingModel.StackTraceOmitted))
        }
    }

    private[models] def errorMsg(e: Throwable) = {
        val modelId = (for {
            m <- Option(submodel)
            mId <- Option(m.modelId)
            id = mId.getId().toString
        } yield id) getOrElse { " with unknown id" }

        s"${e.getClass.getCanonicalName} thrown in model $modelId."
    }

    private[models] def getStackTrace(ex: Throwable) =
        Try {
            val baos = new ByteArrayOutputStream
            val ps = new PrintStream(baos)
            ex.printStackTrace(ps)
            baos.toString
        }.recover {
            case e => ErrorSwallowingModel.StackTraceOmitted
        }.get

    private[models] def getMessageFrom(ex: Throwable) =
        Try { ex.getMessage }.recover { case e => ErrorSwallowingModel.ExMsgThrewMsg }.get

    override def close() = submodel.close()
}

object ErrorSwallowingModel extends ParserProviderCompanion {
    private[models] val ExMsgThrewMsg = "exception getMessage function threw exception.  Message Omitted."
    private[models] val StackTraceOmitted = "Stack trace omitted."

    object Parser extends ModelParser {
        val modelType = "ErrorSwallowingModel"

        import spray.json._, DefaultJsonProtocol._

        protected[this] case class Ast(submodel: JsValue, recordErrorStackTraces: Option[Boolean] = None)
        protected[this] implicit val astJsonFormat = jsonFormat2(Ast)

        def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])(implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[ErrorSwallowingModel[A, B]] {
            def read(json: JsValue): ErrorSwallowingModel[A, B] = {
                semantics map { sem => {
                    implicit val riA = sem.refInfoA
                    implicit val riB = sc.ri

                    val model = for {
                        ast <- Try { json.convertTo[Ast] }
                        submodel <- factory.getModel[A, B](ast.submodel, semantics)
                    } yield ast.recordErrorStackTraces.fold(ErrorSwallowingModel(submodel))(ErrorSwallowingModel(submodel, _))

                    model.get
                }} getOrElse {
                    throw new AlohaFactoryException("No semantics present.")
                }
            }
        }
    }

    def parser: ModelParser = Parser
}
