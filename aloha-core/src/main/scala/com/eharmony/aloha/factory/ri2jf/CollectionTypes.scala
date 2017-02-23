package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol.immSeqFormat

/**
  * Created by ryan on 1/25/17.
  */
class CollectionTypes extends RefInfoToJsonFormatConversions {
  def apply[A](implicit conv: RefInfoToJsonFormat, r: RefInfo[A], jf: JsonFormatCaster[A]): Option[JsonFormat[A]] = {
    if (RefInfoOps.isSubType[A, collection.immutable.Seq[Any]])
      conv(r.typeArguments.head).flatMap(f => jf(immSeqFormat(f)))
    else None
  }
}
