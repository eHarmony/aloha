package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/18/17.
  */
case class Subvalue[+B, +N](audited: B, natural: Option[N])
