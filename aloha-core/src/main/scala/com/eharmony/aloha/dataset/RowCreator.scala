package com.eharmony.aloha.dataset

trait RowCreator[-A, +B] extends (A => (MissingAndErroneousFeatureInfo, B)) with Serializable
