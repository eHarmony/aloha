package com.eharmony.matching.featureSpecExtractor.json.validation

trait Validation {

    /**
     * If an error occurred, provide the error message; otherwise, return None for no error.
     * @return
     */
    def validate(): Option[String]
}
