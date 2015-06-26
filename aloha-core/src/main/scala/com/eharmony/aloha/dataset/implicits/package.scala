package com.eharmony.aloha.dataset

import scala.language.implicitConversions

package object implicits {
    implicit def any2string(a: Any): String = a.toString
    implicit def opt2string(a: Option[Any]): String = a.getOrElse("").toString
}
