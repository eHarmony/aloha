package com.eharmony.matching.aloha.factory

import scala.language.higherKinds

import com.eharmony.matching.aloha.models.Model

trait ParserProviderCompanion {
    protected[this] type M[-_, +_] <: Model[_, _]
    def parser: ModelParser[M]
}
