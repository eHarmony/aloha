package com.eharmony.aloha.id

trait Identifiable[+A] {
    def modelId: A
}
