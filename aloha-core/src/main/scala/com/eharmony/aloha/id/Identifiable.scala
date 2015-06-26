package com.eharmony.aloha.id

trait Identifiable[+A] {
    val modelId: A
}
