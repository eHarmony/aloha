package com.eharmony.matching.aloha.ex

import java.io.{PrintWriter, PrintStream}
import com.eharmony.matching.aloha.AlohaException

/** An exception whose methods, save ''fillInStackTrace'', throw a ''SchrodingerException''.
  * ''fillInStackTrace'' returns a ''SchrodingerException''.  The implication is that any time you
  * try to use it, it throws an exception.  Like Schrodinger's thought experiment, when you peek
  * inside the box, you kill what's inside.  This is a juggernaut that will kill a lot of code
  * that most likely won't protect against it.
  *
  * This should not throw a Throwable at creation time, but creating other Throwables with a
  * SchrodingerException as a cause will most likely throw a Throwable at creation time.
  *
  * SchrodingerException with a cause that is a SchrodingerException will not throw an exception
  * at creation time.
  *
  * ''This is mainly for testing but included as part of main for use in downstream projects.''
  */
final class SchrodingerException(message: String, cause: Throwable) extends AlohaException(message, cause) {
    def this() = this(null, null)
    def this(message: String) = this(message, null)
    def this(cause: Throwable) = this(null, cause)

    /** This function is special.  If it throws, the creation of this exception throws.
      * So, do the next best thing: return this.
      * @return '''this'''
      */
    override def fillInStackTrace() = this
    override def getCause() = throw this
    override def getLocalizedMessage() = throw this
    override def getMessage() = throw this
    override def getStackTrace() = throw this
    override def initCause(cause: Throwable) = throw this
    override def printStackTrace() = throw this
    override def printStackTrace(s: PrintStream) = throw this
    override def printStackTrace(s: PrintWriter) = throw this
    override def setStackTrace(stackTrace: Array[StackTraceElement]) = throw this
    override def toString() = throw this

    def safeToString() = {
        val m = Option(message) getOrElse ""
        s"SchrodingerException($m)"
    }
}

object SchrodingerException {
    val Instance = new SchrodingerException
}
