package com.eharmony.aloha.factory.ri2jf

/**
  * Created by ryan on 1/25/17.
  */
final class StdRefInfoToJsonFormat extends ChainedRefInfoToJsonFormat
                                      with Serializable {
  @transient lazy val conversionTypes = Stream(new BasicTypes, new JavaTypes, new CollectionTypes)
}
