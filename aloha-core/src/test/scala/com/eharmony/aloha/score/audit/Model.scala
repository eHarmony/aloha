package com.eharmony.aloha.score.audit

trait Model[-A, +B] extends (A => B)
