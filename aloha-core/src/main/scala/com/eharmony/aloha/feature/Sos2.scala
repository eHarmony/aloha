package com.eharmony.aloha.feature

/**
  * Sos2 takes a value and breaks it apart into a linear combination of the two closest values as specified by the
  * ''min'', ''max'', and ''delta''.  As long as the value being sos,,2,, binned exists in the interval
  * [''min'', ''max''], then there exists an isomorphism between the value and the sos,,2,, binned value.
  *
  * {{{
  * //                 delta = 1
  * //         |<--------------------->|<--------------------->|
  * //         |     |     |     |     |     |     |     |     |
  * //         |     |     |     |     |     |     |     |     |
  * //         0    0.25  0.5   0.75   1    1.25  1.5   1.75   2
  *
  * // Show the keys output ('=' followed by a value)
  * val v = 1.25
  * val s = sos2(v, 0, 2, 1)
  * assert(s == List(("=1", 0.75), ("=2", 0.25)))
  *
  * // Show the existing isomorphism between v and sos2I(v, min, max, delta) (if min <= v <= max)
  * val vPrime = sos2I(v, 0, 2, 1).foldLeft(0.0){case(s, (k, v)) => s + k * v}
  * assert(v == vPrime)
  * }}}
  */
trait Sos2 { self: DefaultPossessor with BasicMath =>
    private[this] val UnknownSeq = Option(DefaultForMissingDataInReg)
    private[this] val UnderflowKey = Some("UNDERFLOW")

    @inline def sos2(value: Option[Double], min: Long, max: Long, delta: Long): Iterable[(String, Double)] =
        sos2U(value, min, max, delta, None, None)

    /**
      * See [[com.eharmony.aloha.feature.Sos2]].
      *
      * @param value number to be sos,,2,, binned
      * @param min minimum bin value
      * @param max minimum bin value
      * @param delta bin size
      * @param underflowKey When value < min, an underflow key-value pair is emitted.  This controls the key that is
      *                     emitted.
      * @param unknownKey When value is missing, a separate key-value pair is emitted.  This controls the key that is
      *                   emitted.
      * @return sos,,2,, binned value
      */
    @inline def sos2(value: Option[Double], min: Long, max: Long, delta: Long, underflowKey: String, unknownKey: String): Iterable[(String, Double)] =
        sos2U(value, min, max, delta, Option(underflowKey), Option(Seq((s"=$unknownKey", 1.0))))

    @inline def sos2U(value: Option[Double], min: Long, max: Long, delta: Long): Iterable[(String, Double)] =
        sos2U(value, min, max, delta, UnderflowKey, UnknownSeq)

    /**
      * {{{
      * scala>  (0 to 10).map(_ / 4.0 - 0.25).map(v => s"$v\t${sos2U(v, 0, 2, 1)}").foreach(println)
      * -0.25  List((=UNDERFLOW,1.0))
      * 0.0    List((=0,1.0))
      * 0.25   List((=0,0.75), (=1,0.25))
      * 0.5    List((=0,0.5), (=1,0.5))
      * 0.75   List((=0,0.25), (=1,0.75))
      * 1.0    List((=1,1.0))
      * 1.25   List((=1,0.75), (=2,0.25))
      * 1.5    List((=1,0.5), (=2,0.5))
      * 1.75   List((=1,0.25), (=2,0.75))
      * 2.0    List((=2,1.0))
      * 2.25   List((=2,1.0))
      * }}}
      * @param value number to be sos,,2,, binned
      * @param min minimum bin value
      * @param max minimum bin value
      * @param delta bin size
      * @return sos,,2,, binned value
      */
    @inline def sos2U(value: Double, min: Long, max: Long, delta: Long): Iterable[(String, Double)] =
        sos2U(Some(value), min, max, delta, UnderflowKey, UnknownSeq)

    /** Like sos2U but no underflows are reported.  Instead the values are first clamped to be in range so in the
      * event value < min, return a tuple representing the min.
      * @param value number to be sos,,2,, binned
      * @param min minimum bin value
      * @param max minimum bin value
      * @param delta bin size
      * @return sos,,2,, binned value
      */
    @inline def sos2(value: Double, min: Long, max: Long, delta: Long): Iterable[(String, Double)] =
        sos2I(value, min, max, delta).map(p => (s"=${p._1}", p._2))

    /**
      *
      * @param value number to be sos,,2,, binned
      * @param min minimum bin value
      * @param max minimum bin value
      * @param delta bin size
      * @param underflowKey When value < min, an underflow key-value pair is emitted.  This controls the key that is
      *                     emitted.
      * @param unknown When value is missing (None), a separate key-value pair is emitted.  This controls the pair(s)
     *                 that is/are emitted.
      * @return
      */
    def sos2U(value: Option[Double], min: Long, max: Long, delta: Long, underflowKey: Option[String], unknown: Option[Iterable[(String, Double)]]): Iterable[(String, Double)] = {
        value flatMap {_ match {
            case v if v.isNaN => unknown
            case v if v < min => badPair(underflowKey)
            case v => Option(sos2(v, min, max, delta))
        }} orElse unknown getOrElse Nil
    }

    /** This is the purest form of sos,,2,, binning that clamps the values in the [''min'', ''max''] interval and then
      * bins.
      * {{{
      * scala>  (0 to 10).map(_ / 4.0 - 0.25).map(v => s"$v\t${sos2(v, 0, 2, 1)}").foreach(println)
      * -0.25  List((0,1.0))
      * 0.0    List((0,1.0))
      * 0.25   List((0,0.75), (1,0.25))
      * 0.5    List((0,0.5), (1,0.5))
      * 0.75   List((0,0.25), (1,0.75))
      * 1.0    List((1,1.0))
      * 1.25   List((1,0.75), (2,0.25))
      * 1.5    List((1,0.5), (2,0.5))
      * 1.75   List((1,0.25), (2,0.75))
      * 2.0    List((2,1.0))
      * 2.25   List((2,1.0))
      * }}}
      * @param value number to be sos,,2,, binned
      * @param min minimum bin value
      * @param max minimum bin value
      * @param delta bin size
      * @return sos,,2,, binned value
      */
    def sos2I(value: Double, min: Long, max: Long, delta: Long) = {
        val v = (clamp(value, min, max) - min) / delta

        val bin = v.toInt
        val binName = (min + bin * delta).toInt

        val fraction = v - bin
        val oneMinus = 1 - fraction

        if (1 == oneMinus) {
            Seq((binName, oneMinus))
        }
        else {
            // TODO: Determine whether this variable is intended to equal binName when (value / delta) is an integer.
            // If so, then the two keys will be the same and the key specified second will clobber the first key-value
            // pair.  This is fine but, it should be noted.  It also has the implication that the second key-value
            // pair should be declared first because it seems like a better idea that if we have to specify one k-v
            // pair, then it should be the one that has an associated value of 1.  It seems weird to specify it so
            // that the key with value 0 is the declared one.
            val binNameP1 = (min + (bin + 1) * delta).toInt

            // See above note!
            Seq((binName, oneMinus), (binNameP1, fraction))
        }
    }

    @inline private[this] def badPair(s: Option[String]) = s.map(k => Seq((s"=$k", 1.0)))
}
