//package com.eharmony.aloha.models.ensemble
//
//import com.eharmony.aloha.score.basic.ModelOutput
//
//case class EnsembleCombiner[B, C, +D](zero: ModelOutput[B] => C, seqOp: (C, (ModelOutput[B], Int)) => C, finalOp: C => ModelOutput[D])
