package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.algebra.NaturalOptionTransformation
import com.eharmony.aloha.reflect.RefInfo
import spray.json.JsonFormat

/**
  * Created by ryan on 1/25/17.
  */
trait RefInfoToJsonFormat extends NaturalOptionTransformation[RefInfo, JsonFormat]
