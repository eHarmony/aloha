package com.eharmony.aloha.feature

import scala.language.higherKinds
import scala.collection.generic.{FilterMonadic, CanBuildFrom}

/** Provides ways to transform items from between dense and sparse domains.
  * @author R. M. Deak
  */
trait SparsityTransforms {

    /** Given a sparse mapping represented as key-value pairs in parallel iterables, convert to a dense format of the
      * appropriate container type.  This is done by mapping over the ''denseDomain'', determining whether a key
      * exists in ''sparseKeys''. If a key exists, the associated value is substituted; otherwise, substitute with the
      * ''whenMissing'' value.
      *
      * In the event of duplicate keys, for each pair of duplicate keys, the key-value pair associated with the second
      * encountered key will be used.
      *
      * An example:
      *
      * {{{
      * val denseDomain = 3 to 6
      * val sparseKeys = Array(4, 6)
      * val sparseVals = Iterable(1, 2)
      * val sparseValOptions = sparseVals.map(Option.apply)
      * val whenMissing = None
      *
      * val result = densify(denseDomain, sparseKeys, sparseValOptions, whenMissing)
      * val expected = Vector(None, Some(1), None, Some(2))
      *
      * // NOTE: The resulting container type is a Vector because of the type of denseDomain.
      * assert(result.getClass.getCanonicalName == "scala.collection.immutable.Vector")
      * assert(result == expected)
      * }}}
      *
      * Notice in the example above that when all of the keys are contained in denseDomain, then when sparsifying the
      * results of densify, we get back the original sparseVals:
      *
      * {{{
      * assert(result.flatten == sparseVals)
      * }}}
      *
      * @param denseDomain the domain of dense values provided as the preimage to the sparse mapping specified by
      *                    the parallel iterables.
      * @param sparseKeys the keys in the sparse mapping (NOTE: (sparseKeys(i), sparseVals(i)) represents a key-value
      *                   pair)
      * @param sparseVals the values in the sparse mapping
      * @param whenMissing the resulting value when an item from denseDomain isn't contained in sparseKeys.
      * @param cbf a CanBuildFrom object
      * @tparam A type of the dense domain
      * @tparam B type of the dense range
      * @tparam F the container type of the input.  An attempt is made to make the output container type as close as
      *           possible to the input container type.  While this is a FilterMonadic, it really only needs to be a
      *           functor (because we only care about the map function.  Flatmap doesn't matter).
      * @tparam That the resulting type implementation.
      * @return the dense image of the mapping from the dense domain, using the mapping created by
      *         sparseKeys, sparseVals and whenMissing.
      */
    def densifyPI[A, B, F[C] <: FilterMonadic[C, F[C]], That](denseDomain: F[A], sparseKeys: Iterable[A], sparseVals: Iterable[B], whenMissing: B)(implicit cbf: CanBuildFrom[F[A], B, That]): That =
        densifyMap(denseDomain, (sparseKeys zip sparseVals).toMap, whenMissing)

    /** Given a sparse mapping represented as a function (''sparseMapping'') from the input domain ''A'' to a range of
      * optional values (''Option[B]''), convert to a dense format of the appropriate container type.  This is done by
      * composing ''f = g ∘ sparseMapping'', where ''g'' is defined as:
      *
      * <pre>
      *        ⎧ y            if x = Some(y)
      * g(x) = ⎨
      *        ⎩ whenMissing  otherwise
      * </pre>
      *
      * and mapping ''f'' over ''denseDomain''.
      *
      * An example:
      *
      * {{{
      * val denseDomain = 3 to 6
      * val sparseKeys = Array(4, 6)
      * val sparseVals = Iterable(1, 2)
      * val sparseMapping = (sparseKeys zip sparseVals).toMap.get _   // Int => Option[Int]
      * val whenMissing = 0
      *
      * val result = densify(denseDomain, sparseMapping, whenMissing)
      * val expected = Vector(0, 1, 0, 2)
      *
      * // NOTE: The resulting container type is a Vector because of the type of denseDomain.
      * assert(result.getClass.getCanonicalName == "scala.collection.immutable.Vector")
      * assert(result == expected)
      * }}}
      *
      * @param denseDomain the domain of dense values provided as the preimage to the sparse mapping specified by
      *                    the parallel iterables.
      * @param sparseMapping a mapping from the input domain to an option of the output domain.  Once composed with
      *                      ''whenMissing'' this map all values of the domain an appropriate value in the range of the
      *                      function.
      * @param whenMissing the resulting value when an item from denseDomain isn't contained in sparseKeys.
      * @param cbf a CanBuildFrom object
      * @tparam A type of the dense domain
      * @tparam B type of the dense range
      * @tparam F the container type of the input.  An attempt is made to make the output container type as close as
      *           possible to the input container type.  While this is a FilterMonadic, it really only needs to be a
      *           functor (because we only care about the map function.  Flatmap doesn't matter).
      * @tparam That the resulting type implementation.
      * @return the dense image of the mapping from the dense domain, using sparseMapping and whenMissing.
      */
    def densifyFn[A, B, F[C] <: FilterMonadic[C, F[C]], That](denseDomain: F[A], sparseMapping: A => Option[B], whenMissing: B)(implicit cbf: CanBuildFrom[F[A], B, That]): That = {
        val m = sparseMapping andThen { _ getOrElse whenMissing }
        denseDomain map { m.apply }
    }

    /** Given a sparse mapping represented as a map (''sparseFeatures''), convert to a dense format of the appropriate
      * container type.  This is done by creating a map based on ''sparseFeatures'' with a default value specified by
      * ''whenMissing'' and mapping the new map's apply function over ''denseDomain''.
      *
      * An example:
      *
      * {{{
      * val denseDomain = 3 to 6
      * val sparseKeys = Array(4, 6)
      * val sparseVals = Iterable(1, 2)
      * val sparseFeatures = (sparseKeys zip sparseVals).toMap
      * val whenMissing = 0
      *
      * val result = densify(denseDomain, sparseFeatures, whenMissing)
      * val expected = Vector(0, 1, 0, 2)
      *
      * // NOTE: The resulting container type is a Vector because of the type of denseDomain.
      * assert(result.getClass.getCanonicalName == "scala.collection.immutable.Vector")
      * assert(result == expected)
      * }}}
      *
      *
      * @param denseDomain the domain of dense values provided as the preimage to the sparse mapping specified by
      *                    the parallel iterables.
      * @param sparseFeatures a map from the domain to range
      * @param whenMissing the resulting value when an item from denseDomain isn't contained in sparseKeys.
      * @param cbf a CanBuildFrom object
      * @tparam A type of the dense domain
      * @tparam B type of the dense range
      * @tparam F the container type of the input.  An attempt is made to make the output container type as close as
      *           possible to the input container type.  While this is a FilterMonadic, it really only needs to be a
      *           functor (because we only care about the map function.  Flatmap doesn't matter).
      * @tparam That the resulting type implementation.
      * @return the dense image of the mapping from the dense domain, using sparseMapping and whenMissing.
      */
    def densifyMap[A, B, F[C] <: FilterMonadic[C, F[C]], That](denseDomain: F[A], sparseFeatures: Map[A, B], whenMissing: B)(implicit cbf: CanBuildFrom[F[A], B, That]): That = {
        val m = sparseFeatures withDefaultValue whenMissing
        denseDomain map { m.apply }
    }
}

object SparsityTransforms extends SparsityTransforms
