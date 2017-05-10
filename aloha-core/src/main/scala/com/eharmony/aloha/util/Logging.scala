package com.eharmony.aloha.util

import org.slf4j.{Logger, LoggerFactory}

/**
 * Mix the `Logging` trait into a class to get:
 *
 - protected Logging methods
 - protected A `org.slf4j.Logger` object, accessible via the `logger` property.
 */
trait Logging {

  /**
    * The name with which the logger is initialized.  This can be overridden in a derived class.
    * @return
    */
  protected def loggerInitName(): String = getClass.getName

  /**
    * The logger is a `@transient lazy val` to enable proper working with Spark.
    * The logger will not be serialized with the rest of the class with which this
    * trait is mixed-in.
    */
  @transient final protected[this] lazy val logger: Logger = LoggerFactory.getLogger(loggerInitName())

  /**
   * Get the name associated with this logger.
   *
   * @return the name.
   */
  final protected[this] def loggerName: String = logger.getName

  /**
   * Determine whether trace logging is enabled.
   */
  final protected[this] def isTraceEnabled: Boolean = logger.isTraceEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected[this] def trace(msg: => Any): Unit =
    if (logger.isTraceEnabled) logger.trace(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected[this] def trace(msg: => Any, t: => Throwable): Unit =
    if (logger.isTraceEnabled) logger.trace(msg.toString, t)

  /**
   * Determine whether debug logging is enabled.
   */
  final protected[this] def isDebugEnabled: Boolean = logger.isDebugEnabled

  /**
   * Issue a debug logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected[this] def debug(msg: => Any): Unit =
    if (logger.isDebugEnabled()) logger.debug(msg.toString)

  /**
   * Issue a debug logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected[this] def debug(msg: => Any, t: => Throwable): Unit =
    if (logger.isDebugEnabled()) logger.debug(msg.toString, t)

  /**
   * Determine whether error logging is enabled.
   */
  final protected[this] def isErrorEnabled: Boolean = logger.isErrorEnabled

  /**
   * Issue a error logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected[this] def error(msg: => Any): Unit =
    if (logger.isErrorEnabled) logger.error(msg.toString)

  /**
   * Issue a error logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected[this] def error(msg: => Any, t: => Throwable): Unit =
    if (logger.isErrorEnabled) logger.error(msg.toString, t)

  /**
   * Determine whether info logging is enabled.
   */
  final protected[this] def isInfoEnabled: Boolean = logger.isInfoEnabled

  /**
   * Issue a info logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected[this] def info(msg: => Any): Unit =
    if (logger.isInfoEnabled) logger.info(msg.toString)

  /**
   * Issue a info logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected[this] def info(msg: => Any, t: => Throwable): Unit =
    if (logger.isInfoEnabled) logger.info(msg.toString, t)

  /**
   * Determine whether warn logging is enabled.
   */
  final protected[this] def isWarnEnabled: Boolean = logger.isWarnEnabled

  /**
   * Issue a warn logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected[this] def warn(msg: => Any): Unit =
    if (logger.isWarnEnabled) logger.warn(msg.toString)

  /**
   * Issue a warn logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected[this] def warn(msg: => Any, t: => Throwable): Unit =
    if (logger.isWarnEnabled) logger.warn(msg.toString, t)
}
