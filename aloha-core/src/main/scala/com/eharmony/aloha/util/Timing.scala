package com.eharmony.aloha.util

trait Timing {

    /** A standard timing function.
      * @param timeThis something to time
      * @tparam A the output type of the thing being timed.
      * @return
      */
    protected[this] final def time[A](timeThis: => A): (A, Float) = {
        val t1 = System.nanoTime
        val r = timeThis
        val t2 = System.nanoTime
        (r, (1.0e-9*(t2 - t1)).toFloat)
    }
}
