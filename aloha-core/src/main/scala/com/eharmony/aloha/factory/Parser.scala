package com.eharmony.aloha.factory

trait Parser[-A, +B] {
    def parse(a: A): B
}
