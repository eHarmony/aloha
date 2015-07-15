package com.eharmony.aloha.dataset

trait RowCreator[-A] extends (A => (MissingAndErroneousFeatureInfo, CharSequence)) with Serializable
