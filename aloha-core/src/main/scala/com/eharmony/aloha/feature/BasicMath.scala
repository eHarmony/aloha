package com.eharmony.aloha.feature

trait BasicMath {
    private[this] val _1_ln2 = 1 / math.log(2)
    @inline def clamp(x: Double, min: Double, max: Double) = math.min(math.max(min, x), max)
    @inline def clamp(x: Float, min: Float, max: Float) = math.min(math.max(min, x), max)
    @inline def clamp(x: Long, min: Long, max: Long) = math.min(math.max(min, x), max)
    @inline def clamp(x: Int, min: Int, max: Int) = math.min(math.max(min, x), max)
    @inline def log2(x: Double): Double =  _1_ln2 * math.log(x)
}
