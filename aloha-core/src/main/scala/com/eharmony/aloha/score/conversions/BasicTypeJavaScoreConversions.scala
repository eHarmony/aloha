package com.eharmony.aloha.score.conversions

import com.eharmony.aloha.score.Scores.Score
import java.{lang => jl}

trait BasicTypeJavaScoreConversions {
    def asJavaBoolean(s: Score): jl.Boolean
    def asJavaByte(s: Score): jl.Byte
    def asJavaShort(s: Score): jl.Short
    def asJavaInteger(s: Score): jl.Integer
    def asJavaLong(s: Score): jl.Long
    def asJavaFloat(s: Score): jl.Float
    def asJavaDouble(s: Score): jl.Double
    def asJavaString(s: Score): String
}
