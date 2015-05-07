package com.eharmony.matching.featureSpecExtractor.csv.json

import scala.collection.{immutable => sci}
import spray.json.DefaultJsonProtocol
import com.eharmony.matching.featureSpecExtractor.density.Dense
import com.eharmony.matching.featureSpecExtractor.json.{CovariateJson, DenseSpec}


case class CsvJson(imports: Seq[String], features: sci.IndexedSeq[DenseSpec]) extends CovariateJson[Dense]

object CsvJson extends DefaultJsonProtocol {
    implicit val csvJsonFormat = jsonFormat2(CsvJson.apply)
}
