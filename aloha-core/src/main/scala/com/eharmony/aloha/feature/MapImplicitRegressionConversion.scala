package com.eharmony.aloha.feature

import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}

import scala.language.implicitConversions
import scala.{collection => sc}

/**
 * This class is designed to convert Map[String, X] to Iterable[(String, Double)] if it's easy to convert
 * X to Double.
 * @author R M Deak
 *
 */
trait MapImplicitRegressionConversion {
    implicit def intMapToDoubleConversion(m: sc.Map[String, Int]): sc.Map[String, Double] = m.mapValues(_.toDouble)
    implicit def longMapToDoubleConversion(m: sc.Map[String, Long]): sc.Map[String, Double] = m.mapValues(_.toDouble)
    implicit def floatMapToDoubleConversion(m: sc.Map[String, Float]): sc.Map[String, Double] = m.mapValues(_.toDouble)
    implicit def atomicIntMapToDoubleConversion(m: sc.Map[String, AtomicInteger]): sc.Map[String, Double] = m.mapValues(_.get().toDouble)
    implicit def atomicLongMapToDoubleConversion(m: sc.Map[String, AtomicLong]): sc.Map[String, Double] = m.mapValues(_.get().toDouble)
}

object MapImplicitRegressionConversion extends MapImplicitRegressionConversion
