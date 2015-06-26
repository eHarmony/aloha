package com.eharmony.aloha.id

import spray.json.{JsValue, RootJsonFormat}
import spray.json.DefaultJsonProtocol.{StringJsonFormat, LongJsonFormat, jsonFormat2}

object ModelIdentityJson {
    implicit val modelIdJsonFormat: RootJsonFormat[ModelId] = jsonFormat2(ModelId.apply)

    implicit val modelIdentityJsonFormat: RootJsonFormat[ModelIdentity] = new RootJsonFormat[ModelIdentity] {
        def write(mid: ModelIdentity) = modelIdJsonFormat.write(ModelId(mid.getId(), mid.getName()))
        def read(json: JsValue) = modelIdJsonFormat.read(json)
    }
}
