package com.eharmony.matching.aloha.factory

import scala.util.Try

import spray.json.{JsonReader, pimpString}

import com.eharmony.matching.aloha.io._
import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.RefInfo
import grizzled.slf4j.Logging
import com.eharmony.matching.aloha.io.sources.ReadableSource
import com.eharmony.matching.aloha.io.multiple.{SequenceMultipleReadable, MultipleAlohaReadable}

/**
  *
  * Note that there are essentially three factory interfaces associated with this factory.  Those are calling:
  *
  1.   methods from [[com.eharmony.matching.aloha.io.AlohaReadable]]: This gives back a scala.util.Try[ [[com.eharmony.matching.aloha.models.Model]] ] instance.
  1.   methods in the model value.  This gives back a scala.util.Try of an instance of M[A, B].
  1.   methods in the modelAndInfo value.  This gives back a scala.util.Try of an instance of [[com.eharmony.matching.aloha.factory.ModelInfo]][ M[A, B] ].
  *
  * From Scala, it is recommended to use function from methods 2 or 3 above as more type information is retained.
  *
  * From Java, the easiest method of instantiation is to use Spring to instantiate the factory. Then
  * use the factory to instantiate models via the first set of methods mentioned above.  Note: the factory should
  * be of the interface type AlohaReadable like in the first example to follow; otherwise, a compile time error
  * may result due to type erasure.  For more information, see some of the Scala bugs:
  *
  *   - [[https://issues.scala-lang.org/browse/SI-6414 SI-6414]]
  *   - [[https://issues.scala-lang.org/browse/SI-7374 SI-7374]]
  *   - [[https://issues.scala-lang.org/browse/SI-3452 SI-3452]]
  *
  * {{{
  * // ===  JAVA CODE  ========================================================
  * // Set up Spring to inject class ...
  * public class X {
  *     // Injected from Spring
  *     @Resource
  *     private AlohaReadable<Try<Model<TestProto, Double>>> modelFactory = null;
  *
  *     public Model<TestProto, Double> getModelFromClasspath(String path) {
  *         return modelFactory.fromClasspath(path).get();
  *     }
  * }
  * }}}
  *
  * Rather than:
  *
  * {{{
  * // ===  JAVA CODE  ========================================================
  * // Set up Spring to inject class ...
  * public class X {
  *     // Injected from Spring
  *     @Resource
  *     private TypedModelFactory< TestProto, Double, Model< TestProto, Double > > modelFactory = null;
  *
  *     public Model< TestProto, Double> getModelFromClasspath(String path) {
  *         // This line causes a compile time error stating:
  *         //
  *         //    /z/y/X.java cannot find symbol
  *         //    symbol  : method get()
  *         //    location: class java.lang.Object
  *         //
  *         // because the higher kind M[A, B] erases to Object.
  *         return modelFactory.fromClasspath(path).get();
  *
  *         // This would work (but the cast is annoying) and the coder has to know about the required casting.
  *         // return ((Try< Model< TestProto, Double > >) modelFactory.fromClasspath(path)).get();
  *     }
  * }
  * }}}
  *
  * @param factory an untyped factory the produces models of kind M
  * @param semantics semantics used to instantiate the models
  * @param evidence$1 reflection information about type parameter A
  * @param evidence$2 reflection information about type parameter B
  * @param evidence$3 a way to read a variable of type B from JSON
  * @param evidence$4 a way to convert an instance of type B into a [[com.eharmony.matching.aloha.score.Scores.Score]]
  * @tparam A the input type of models created by this factory
  * @tparam B the output type of models created by this factory
  * @tparam M the kind (as in higher kind) of models created.  This represents the greatest lower bound of all of the
  * model types whose parsers were passed to the factory parameter.  In other words, it is the most specific type of
  * model algorithm interface that can be produced, given the parsers that were provided to the untyped factory
  * parameter that was passed to this factory instance.
  */
case class TypedModelFactory[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]] = None)
    extends ReadableByString[Try[Model[A, B]]]
    with GZippedReadable[Try[Model[A, B]]]
    with LocationLoggingReadable[Try[Model[A, B]]]
    with MultipleAlohaReadable[Try[Model[A, B]]]
    with SequenceMultipleReadable[ReadableSource, Try, Model[A, B]]
    with Logging {

    /** Most information provided by any of the three APIs.  One of the two recommended interfaces for Scala code.
      */
    val modelAndInfo =
        new ReadableByString[Try[ModelInfo[Model[A, B]]]]
            with GZippedReadable[Try[ModelInfo[Model[A, B]]]]
            with MultipleAlohaReadable[Try[ModelInfo[Model[A, B]]]]
            with SequenceMultipleReadable[ReadableSource, Try, ModelInfo[Model[A, B]]]
            with LocationLoggingReadable[Try[ModelInfo[Model[A, B]]]]
            with Logging {
        def fromString(s: String): Try[ModelInfo[Model[A, B]]] = Try { throw new UnsupportedOperationException("modelAndInfo unsupported") }
    }

    /** The recommended API method from Java.  It's also equally valid to use any of the other methods provided by
      * [[com.eharmony.matching.aloha.io.AlohaReadable]] trait.  It throws away the kind information (encoded in the
      * type parameter M) that is retained the in Scala environment.
      * @param s String representation of the model.
      * @return [[http://www.scala-lang.org/api/current/index.html#scala.util.Try scala.util.Try]] of a
      *         [[com.eharmony.matching.aloha.models.Model]][A, B].
      */
    def fromString(s: String): Try[Model[A, B]] = factory.getModel[A, B](s.parseJson, semantics)
}
