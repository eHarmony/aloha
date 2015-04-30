package com.eharmony.matching.featureSpecExtractor


/**
 * This is for backward compatibility, but should someday be removed and SpecBuilder should be updated to remove
 * it too.  Eventually, the spec type should always appear in the JSON used to create the JsonSpec.
 */
object SpecType extends Enumeration {
    type SpecType = Value
    val VW, LibSVM = Value
}
