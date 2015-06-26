package com.eharmony.aloha.io

trait AlohaReadable[A]
    extends FileReadable[A]
    with NonFileReadable[A]
