package com.eharmony.aloha.models

import java.io.{ByteArrayOutputStream, PrintStream}

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.{Semantics, SemanticsUdfException}

import scala.util.Try

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
// TODO: Determine if thisi should have a Submodel or Model parameter.  How can we use the submodel's auditor?
case class ErrorSwallowingModel[U, N, -A, +B <: U](
    submodel: Submodel[N, A, U],
    auditor: Auditor[U, N, B],
    recordErrorStackTraces: Boolean = true
) extends SubmodelBase[U, N, A, B] {

  def modelId: ModelIdentity = submodel.modelId

  override def subvalue(a: A): Subvalue[B, N] = {
    // Delegate the scoring to the submodel but trap exceptions.
    val sTry: Try[Subvalue[B, N]] = Try {
      val s = submodel.subvalue(a)
      val subs = Seq(s.audited)
      s.natural.fold(failure(Seq(""), Set.empty, subvalues = subs))(n => success(n, subvalues = subs))
    }

    // If we shouldn't record stack traces, just work off the original Try.  If we should, then
    // attempt to map the exception to a failure with a stack trace.  Then be super careful when
    // performing a final recovery attempt with defaultError.  This method should not allow
    // Throwables to pass.

    val orWithTrace =
      if (!recordErrorStackTraces)
        sTry
      else sTry recoverWith errorWithTrace
    val s = (orWithTrace recoverWith defaultError).get
    s
  }

//    private[aloha] def getScore(a: A)(implicit audit: Boolean) = {
//        // Delegate the scoring to the submodel but trap exceptions.
//        val sTry = Try { submodel.getScore(a) }
//
//        // If we shouldn't record stack traces, just work off the original Try.  If we should, then
//        // attempt to map the exception to a failure with a stack trace.  Then be super careful when
//        // performing a final recovery attempt with defaultError.  This method should not allow
//        // Throwables to pass.
//        val s = (if (!recordErrorStackTraces) sTry else sTry recoverWith errorWithTrace).recoverWith { defaultError }.get
//        s
//    }

    /** Attempt to take the Throwable and convert to the output type with a stack trace.
      *
      */
  private[models] val errorWithTrace: PartialFunction[Throwable, Try[Subvalue[B, N]]] = {
    case ex: SemanticsUdfException[A] =>
      Try { failure(basicErrorInfo(ex) ++ extendedErrorInfo(ex), Set.empty) }
    case ex: Throwable =>
      Try { failure(basicErrorInfo(ex), Set.empty) }
  }

//  private[models] def errorWithTrace(implicit audit: Boolean): ReturnVal = {
//        case ex: SemanticsUdfException[A] => Try { failure(errors = basicErrorInfo(ex) ++ extendedErrorInfo(ex)) }
//        case ex: Throwable => Try { failure(errors = basicErrorInfo(ex)) }
//    }

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
    private[models] val defaultError: PartialFunction[Throwable, Try[Subvalue[B, N]]] = {
      case ex: Throwable => Try {
        // Being super protective here to avoid the getMessage throwing exception (such as
        // a message that calls toString on an infinite depth tree that would cause a stack
        // overflow).
        failure(Seq(errorMsg(ex), getMessageFrom(ex), ErrorSwallowingModel.StackTraceOmitted), Set.empty)
      }
    }

  //    private[models] def defaultError(implicit audit: Boolean): ReturnVal = {
//        case ex: Throwable => Try {
//            // Being super protective here to avoid the getMessage throwing exception (such as
//            // a message that calls toString on an infinite depth tree that would cause a stack
//            // overflow).
//            failure(errors = Seq(errorMsg(ex), getMessageFrom(ex), ErrorSwallowingModel.StackTraceOmitted))
//        }
//    }

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
            case _: Throwable => ErrorSwallowingModel.StackTraceOmitted
        }.get

    private[models] def getMessageFrom(ex: Throwable) =
        Try { ex.getMessage }.recover { case e => ErrorSwallowingModel.ExMsgThrewMsg }.get

    override def close(): Unit = Try { submodel.close() }
}

object ErrorSwallowingModel extends ParserProviderCompanion {
  private[models] val ExMsgThrewMsg = "exception getMessage function threw exception.  Message Omitted."
  private[models] val StackTraceOmitted = "Stack trace omitted."

  object Parser extends ModelSubmodelParsingPlugin {
    override val modelType = "ErrorSwallowingModel"

    import spray.json._
    import DefaultJsonProtocol._

    protected[this] case class Ast(submodel: JsValue, recordErrorStackTraces: Option[Boolean] = None)
    protected[this] implicit val astJsonFormat = jsonFormat2(Ast)

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[ErrorSwallowingModel[U, N, A, B]]] = {

      Some(new JsonReader[ErrorSwallowingModel[U, N, A, B]] {
        override def read(json: JsValue): ErrorSwallowingModel[U, N, A, B] = {
          val model = for {
            ast <- Try { json.convertTo[Ast] }
            submodel <- factory.submodel[N](ast.submodel)
          } yield (
            ast.recordErrorStackTraces.fold(ErrorSwallowingModel(submodel, auditor))
                                           (r => ErrorSwallowingModel(submodel, auditor, r))
          )

          model.get
        }
      })
    }
  }

  def parser: NewModelParser = Parser


  //    object Parser extends ModelParser {
//        val modelType = "ErrorSwallowingModel"
//
//        import spray.json._, DefaultJsonProtocol._
//
//        protected[this] case class Ast(submodel: JsValue, recordErrorStackTraces: Option[Boolean] = None)
//        protected[this] implicit val astJsonFormat = jsonFormat2(Ast)
//
//        def modelJsonReader[A, B](factory: ModelFactory, semantics: Option[Semantics[A]])(implicit jr: JsonReader[B], sc: ScoreConverter[B]) = new JsonReader[ErrorSwallowingModel[A, B]] {
//            def read(json: JsValue): ErrorSwallowingModel[A, B] = {
//                semantics map { sem => {
//                    implicit val riA = sem.refInfoA
//                    implicit val riB = sc.ri
//
//                    val model = for {
//                        ast <- Try { json.convertTo[Ast] }
//                        submodel <- factory.getModel[A, B](ast.submodel, semantics)
//                    } yield ast.recordErrorStackTraces.fold(ErrorSwallowingModel(submodel))(ErrorSwallowingModel(submodel, _))
//
//                    model.get
//                }} getOrElse {
//                    throw new AlohaFactoryException("No semantics present.")
//                }
//            }
//        }
//    }

}
