package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import spray.json.DefaultJsonProtocol.{immSeqFormat, mapFormat}
import spray.json.JsonFormat

/**
  * Created by ryan on 1/25/17.
  */
class CollectionTypes extends RefInfoToJsonFormatConversions {
  def apply[A](implicit conv: RefInfoToJsonFormat,
               r: RefInfo[A],
               jf: JsonFormatCaster[A]): Option[JsonFormat[A]] = {

    val typeParams = RefInfoOps.typeParams[A]

    if (RefInfoOps.isSubType[A, collection.immutable.Map[Any, Any]])
      typeParams match {
        case List(tKey, tVal) =>
          for {
            k <- conv(tKey)
            v <- conv(tVal)
            f <- jf(mapFormat(k, v))
          } yield f
        case _ =>
          // TODO: perhaps change the API at some point to better report errors here.
          None
      }
    else if (RefInfoOps.isSubType[A, collection.immutable.Seq[Any]])
      for {
        tEl <- typeParams.headOption
        el  <- conv(tEl)
        f   <- jf(immSeqFormat(el))
      } yield f
    else None
  }
}
