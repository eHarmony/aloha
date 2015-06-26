package com.eharmony.aloha.util

import org.slf4j.{ Logger => SLF4JLogger }

/**
 * Scala front-end to a SLF4J logger.
 */
class Logger(val logger: SLF4JLogger) extends Serializable {
  import scala.language.implicitConversions

  /**
   * Get the name associated with this logger.
   *
   * @return the name.
   */
  @inline final def name = logger.getName

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isTraceEnabled = logger.isTraceEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def trace(msg: => Any): Unit =
    if (isTraceEnabled) logger.trace(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def trace(msg: => Any, t: => Throwable): Unit =
    if (isTraceEnabled) logger.trace(msg, t)

  /**
   * Determine whether debug logging is enabled.
   */
  @inline final def isDebugEnabled = logger.isDebugEnabled

  /**
   * Issue a debug logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def debug(msg: => Any): Unit =
    if (isDebugEnabled) logger.debug(msg.toString)

  /**
   * Issue a debug logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def debug(msg: => Any, t: => Throwable): Unit =
    if (isDebugEnabled) logger.debug(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isErrorEnabled = logger.isErrorEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def error(msg: => Any): Unit =
    if (isErrorEnabled) logger.error(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def error(msg: => Any, t: => Throwable): Unit =
    if (isErrorEnabled) logger.error(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isInfoEnabled = logger.isInfoEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def info(msg: => Any): Unit =
    if (isInfoEnabled) logger.info(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def info(msg: => Any, t: => Throwable): Unit =
    if (isInfoEnabled) logger.info(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  @inline final def isWarnEnabled = logger.isWarnEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def warn(msg: => Any): Unit =
    if (isWarnEnabled) logger.warn(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def warn(msg: => Any, t: => Throwable): Unit =
    if (isWarnEnabled) logger.warn(msg, t)

  /**
   * Converts any type to a String. In case the object is null, a null
   * String is returned. Otherwise the method `toString()` is called.
   *
   * @param msg  the message object to be converted to String
   *
   * @return the String representation of the message.
   */
  private implicit def _any2String(msg: Any): String =
    msg match {
      case null => "<null>"
      case _ => msg.toString
    }
}

/**
 * Mix the `Logging` trait into a class to get:
 *
 * - Logging methods
 * - A `Logger` object, accessible via the `log` property
 *
 * Does not affect the public API of the class mixing it in.
 */
trait Logging {
  // The logger. Instantiated the first time it's used.
  private lazy val _logger = Logger(getClass)

  /**
   * Get the `Logger` for the class that mixes this trait in. The `Logger`
   * is created the first time this method is call. The other methods (e.g.,
   * `error`, `info`, etc.) call this method to get the logger.
   *
   * @return the `Logger`
   */
  protected def logger: Logger = _logger

  /**
   * Get the name associated with this logger.
   *
   * @return the name.
   */
  protected def loggerName = logger.name

  /**
   * Determine whether trace logging is enabled.
   */
  protected def isTraceEnabled = logger.isTraceEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  protected def trace(msg: => Any): Unit = logger.trace(msg)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  protected def trace(msg: => Any, t: => Throwable): Unit =
    logger.trace(msg, t)

  /**
   * Determine whether debug logging is enabled.
   */
  protected def isDebugEnabled = logger.isDebugEnabled

  /**
   * Issue a debug logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  protected def debug(msg: => Any): Unit = logger.debug(msg)

  /**
   * Issue a debug logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  protected def debug(msg: => Any, t: => Throwable): Unit =
    logger.debug(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  protected def isErrorEnabled = logger.isErrorEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  protected def error(msg: => Any): Unit = logger.error(msg)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  protected def error(msg: => Any, t: => Throwable): Unit =
    logger.error(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  protected def isInfoEnabled = logger.isInfoEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  protected def info(msg: => Any): Unit = logger.info(msg)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  protected def info(msg: => Any, t: => Throwable): Unit =
    logger.info(msg, t)

  /**
   * Determine whether trace logging is enabled.
   */
  protected def isWarnEnabled = logger.isWarnEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  protected def warn(msg: => Any): Unit = logger.warn(msg)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  protected def warn(msg: => Any, t: => Throwable): Unit =
    logger.warn(msg, t)
}

/**
 * A factory for retrieving an SLF4JLogger.
 */
object Logger {
  import scala.reflect.{ classTag, ClassTag }

  /**
   * The name associated with the root logger.
   */
  val RootLoggerName = SLF4JLogger.ROOT_LOGGER_NAME

  /**
   * Get the logger with the specified name. Use `RootName` to get the
   * root logger.
   *
   * @param name  the logger name
   *
   * @return the `Logger`.
   */
  def apply(name: String): Logger =
    new Logger(org.slf4j.LoggerFactory.getLogger(name))

  /**
   * Get the logger for the specified class, using the class's fully
   * qualified name as the logger name.
   *
   * @param cls  the class
   *
   * @return the `Logger`.
   */
  def apply(cls: Class[_]): Logger = apply(cls.getName)

  /**
   * Get the logger for the specified class type, using the class's fully
   * qualified name as the logger name.
   *
   * @return the `Logger`.
   */
  def apply[C: ClassTag](): Logger = apply(classTag[C].runtimeClass.getName)

  /**
   * Get the root logger.
   *
   * @return the root logger
   */
  def rootLogger = apply(RootLoggerName)
}
