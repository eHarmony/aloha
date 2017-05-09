package com.eharmony.aloha.util

import org.slf4j.LoggerFactory

/**
 * Mix the `Logging` trait into a class to get:
 *
 * - Logging methods
 * - A `Logger` object, accessible via the `log` property
 *
 * Does not affect the public API of the class mixing it in.
 */
trait Logging {

  /**
   * Get the `Logger` for the class that mixes this trait in. The `Logger`
   * is created the first time this method is call. The other methods (e.g.,
   * `error`, `info`, etc.) call this method to get the logger.
   *
   * @return the `Logger`
   */
  protected def loggerInitName(): String = getClass.getName

  @transient final protected lazy val logger = LoggerFactory.getLogger(loggerInitName())

  /**
   * Get the name associated with this logger.
   *
   * @return the name.
   */
  final protected def loggerName = logger.getName

  /**
   * Determine whether trace logging is enabled.
   */
  final protected def isTraceEnabled = logger.isTraceEnabled

  /**
   * Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected def trace(msg: => Any): Unit =
    if (logger.isTraceEnabled) logger.trace(msg.toString)

  /**
   * Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected def trace(msg: => Any, t: => Throwable): Unit =
    if (logger.isTraceEnabled) logger.trace(msg.toString, t)

  /**
   * Determine whether debug logging is enabled.
   */
  final protected def isDebugEnabled = logger.isDebugEnabled

  /**
   * Issue a debug logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected def debug(msg: => Any): Unit =
    if (logger.isDebugEnabled()) logger.debug(msg.toString)

  /**
   * Issue a debug logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected def debug(msg: => Any, t: => Throwable): Unit =
    if (logger.isDebugEnabled()) logger.debug(msg.toString, t)

  /**
   * Determine whether error logging is enabled.
   */
  final protected def isErrorEnabled = logger.isErrorEnabled

  /**
   * Issue a error logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected def error(msg: => Any): Unit =
    if (logger.isErrorEnabled) logger.error(msg.toString)

  /**
   * Issue a error logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected def error(msg: => Any, t: => Throwable): Unit =
    if (logger.isErrorEnabled) logger.error(msg.toString, t)

  /**
   * Determine whether info logging is enabled.
   */
  final protected def isInfoEnabled = logger.isInfoEnabled

  /**
   * Issue a info logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected def info(msg: => Any): Unit =
    if (logger.isInfoEnabled) logger.info(msg.toString)

  /**
   * Issue a info logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected def info(msg: => Any, t: => Throwable): Unit =
    if (logger.isInfoEnabled) logger.info(msg.toString, t)

  /**
   * Determine whether warn logging is enabled.
   */
  final protected def isWarnEnabled = logger.isWarnEnabled

  /**
   * Issue a warn logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  final protected def warn(msg: => Any): Unit =
    if (logger.isWarnEnabled) logger.warn(msg.toString)

  /**
   * Issue a warn logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  final protected def warn(msg: => Any, t: => Throwable): Unit =
    if (logger.isWarnEnabled) logger.warn(msg.toString, t)
}
