package com.eharmony.matching.aloha.factory

import scala.language.higherKinds


trait ParserProviderCompanion {
    def parser: ModelParser
}
