package com.eharmony.aloha.factory

import scala.language.higherKinds


trait ParserProviderCompanion {
    def parser: ModelParser
}
