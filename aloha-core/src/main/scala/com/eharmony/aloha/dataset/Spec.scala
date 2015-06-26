package com.eharmony.aloha.dataset

trait Spec[-A] extends (A => (MissingAndErroneousFeatureInfo, CharSequence)) with Serializable
