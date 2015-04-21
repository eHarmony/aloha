package com.eharmony.matching.aloha.id

trait Identifiable[+A] {
    val modelId: A
}
