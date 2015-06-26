package com.eharmony.aloha.models.tree.decision

import scala.language.implicitConversions

trait DecisionTreeBoolToOptBoolConversions {

    /** Allows conversion from a Boolean to an option of a Boolean.  It is used to lift the boolean value into the
      * computational context of an Option.  This function has many names in many  different contexts (''point'',
      * ''return'', etc).  For more info, see [[http://en.wikipedia.org/wiki/Monad_(functional_programming)]]
      * @param b a Boolean to be converted to an Option[Boolean].
      * @return the lifted boolean.
      */
    implicit def booleanToOptionBoolean(b: Boolean) = Option(b)
}

object DecisionTreeBoolToOptBoolConversions extends DecisionTreeBoolToOptBoolConversions
