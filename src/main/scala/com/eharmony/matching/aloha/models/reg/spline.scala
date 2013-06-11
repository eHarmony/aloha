package com.eharmony.matching.aloha.models.reg

private[reg] sealed trait Spline extends (Double => Double)

/** A spline with the property the delta between consecutive domain values is a fixed constant.  Because of this,
  * we just need to specify the min and max and the values in the image of the function.
  * @param min the minimum domain value
  * @param max the maximum domain value (strictly greater than min IFF spline has at least two knots,
  *            or equal to min IFF spline has one knot)
  * @param knots Required to have a positive number of knots (size > 0).
  */
private[reg] case class ConstantDeltaSpline(min: Double, max: Double, knots: IndexedSeq[Double]) extends Spline {
    require((min < max && 1 < knots.size) || (min == max && 1 == knots.size))

    /** If knots is one, then we want bin equal to be 1 so that the division to get the value of
      * k in calibrate doesn't divide by zero.  If there are 2+ knots, then bin contains the
      * dx value for the spline.
      */
    val bin : Double = if (knots.size == 1) 1.0 else (max - min) / (knots.size - 1)

    def apply(score : Double) : Double = {
        val xp = math.max(min, math.min(score, max))

        // k is necessarily positive because domainMin <= xp.  The integer part of
        // the value is the lower index into the spline and the remainder to the right
        // of the decimal point is the weight associated with the lower value.
        val k = (xp - min) / bin
        val iLow = k.toInt
        if (k == iLow) knots(iLow)
        else {
            val wHigh = (k - iLow)
            val wLow = 1 - wHigh
            wLow * knots(iLow) + wHigh * knots(iLow + 1)
        }
    }
}
