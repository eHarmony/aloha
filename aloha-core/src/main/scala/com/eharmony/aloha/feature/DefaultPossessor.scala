package com.eharmony.aloha.feature

trait DefaultPossessor {

    /** The ''suggested'' default Iterable that should be emitted when data is missing or unexpected in some way.
      * This is provided to the feature generating functions but not doesn't act as a substitute for the framework's
      * responsibility for handling missing data.
      *
      * '''Note''': The keys (1st field) present in the Iterable's Tuple2s should be prefixed with "=".  This is
      * because (as the name suggests, it is mainly for regression models which have a specific format for generated
      * features.
      */
    protected[feature] val DefaultForMissingDataInReg: Iterable[(String, Double)]

    final protected[feature] val empty = Iterable(("", 1.0))
}
