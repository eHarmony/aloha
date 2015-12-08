package com.eharmony.aloha

import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}

/**
  * Created by rdeak on 12/7/15.
  */
trait ModelSerializationTestHelper {
  def serializeDeserializeRoundTrip[A <: java.io.Serializable](a: A): A = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(a)
    val bais = new ByteArrayInputStream(baos.toByteArray)
    val ois = new ObjectInputStream(bais)
    val out = ois.readObject()
    out.asInstanceOf[A]
  }
}
