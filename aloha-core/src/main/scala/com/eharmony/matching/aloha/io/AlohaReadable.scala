package com.eharmony.matching.aloha.io

trait AlohaReadable[A]
    extends FileReadable[A]
    with NonFileReadable[A]
