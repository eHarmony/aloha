package com.eharmony.aloha

import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}

import com.eharmony.aloha.models.Model

/**
  * Created by rdeak on 12/7/15.
  */
trait ModelSerializationTestHelper {
  def serializeDeserializeRoundTrip[M <: Model[_, _]](m: M): M = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(m)
    val bais = new ByteArrayInputStream(baos.toByteArray)
    val ois = new ObjectInputStream(bais)
    val out = ois.readObject()
    out.asInstanceOf[M]
  }
}
