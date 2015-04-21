package com.eharmony.matching.aloha.factory

trait Parser[-A, +B] {
    def parse(a: A): B
}
