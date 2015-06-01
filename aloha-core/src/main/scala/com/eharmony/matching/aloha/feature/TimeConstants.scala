package com.eharmony.matching.aloha.feature

/** See trait documentation.
  */
object TimeConstants extends TimeConstants

/** A bunch of constants taken from
  * [[http://joda-time.sourceforge.net/apidocs/constant-values.html#org.joda.time.DateTimeConstants.SECONDS_PER_DAY Joda-Time]]
  * because we don't necessarily want to include Joda-Time but want access to the constants.  If using Joda-Time, just
  * include those.
  */
trait TimeConstants {

    /** Days in one week (7) (ISO)
      */
    final val DAYS_PER_WEEK      = 7

    /** Hours in a typical day (24) (ISO).
      */
    final val HOURS_PER_DAY      = 24

    /** Hours in a typical week.
      */
    final val HOURS_PER_WEEK     = 168

    /** Milliseconds in a typical day (ISO).
      */
    final val MILLIS_PER_DAY     = 86400000

    /** Milliseconds in one hour (ISO)
      */
    final val MILLIS_PER_HOUR    = 3600000

    /** Milliseconds in one minute (ISO)
      */
    final val MILLIS_PER_MINUTE  = 60000

    /** Milliseconds in one second (1000) (ISO)
      */
    final val MILLIS_PER_SECOND  = 1000

    /** Milliseconds in a typical week (ISO).
      */
    final val MILLIS_PER_WEEK    = 604800000

    /** Minutes in a typical day (ISO).
      */
    final val MINUTES_PER_DAY    = 1440

    /** Minutes in one hour (ISO)
      */
    final val MINUTES_PER_HOUR   = 60

    /** Minutes in a typical week (ISO).
      */
    final val MINUTES_PER_WEEK   = 10080

    /** Seconds in a typical day (ISO).
      */
    final val SECONDS_PER_DAY    = 86400

    /** Seconds in one hour (ISO)
      */
    final val SECONDS_PER_HOUR   = 3600

    /** Seconds in one minute (60) (ISO)
      */
    final val SECONDS_PER_MINUTE = 60

    /** Seconds in a typical week (ISO).
      */
    final val SECONDS_PER_WEEK   = 604800
}
