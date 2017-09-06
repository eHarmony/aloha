package com.eharmony.aloha.models.multilabel

/**
  * A type class used to indicate a parameter has a type that can be serialized in
  * a larger Serializable object.
  */
sealed trait SerializabilityEvidence[A]

object SerializabilityEvidence {

  implicit def anyValEvidence[A <: AnyVal]: SerializabilityEvidence[A] =
    new SerializabilityEvidence[A]{}

  implicit def serializableEvidence[A <: Serializable]: SerializabilityEvidence[A] =
    new SerializabilityEvidence[A]{}
}

