package com.eharmony.aloha.dataset

/**
 * This is for backward compatibility, but should someday be removed and SpecBuilder should be updated to remove
 * it too.  Eventually, the spec type should always appear in the JSON used to create the JsonSpec.
 */
object RowCreatorType extends Enumeration {
    type RowCreatorType = Value
    val VW, LibSVM, CSV = Value
}
