package com.eharmony.matching.featureSpecExtractor.reflect

/**
 * Helper method for getting constructors and instantiating them.
 * @author R M Deak
 */
object Instantiation {
//
//    /**
//     * Get a constructor with a specific parameter list.  For convenience, consider importing
//     * `com.eharmony.matching.featureSpecExtractor.reflect.{string2ParamType, class2ParamType}`.  Then you can attempt
//     * to get a constructor as follows:
//     *
//     * {{{
//     * import com.eharmony.matching.featureSpecExtractor.reflect.{string2ParamType, class2ParamType}
//     * import com.eharmony.matching.featureSpecExtractor.reflect.Instantiation._
//     *
//     * val className: String = ...
//     * val param1DownStreamClassName: String = ...
//     * val param2UpstreamClass: Class[String] = classOf[String]
//     * val ctor: Try[Constructor[_]] = constructor(className, param1DownStreamClassName, param2UpstreamClass)
//     * }}}
//     *
//     * @param className name of the class to be instantiated.
//     * @param params list of parameters to pass as parameters to the constructor.
//     * @return
//     */
//    def constructor(className: String, params: ParamType*): Try[Constructor[_]] =
//        for {
//            clazz        <- Try { Class.forName(className) }
//            paramClasses <- params.sequenceOfClasses
//            ctor         <- Try { clazz.getConstructor(paramClasses:_*) } recoverWith {
//                case _ => Failure(new NoSuchMethodException(s"$className(${params.paramListString}.  Found: ${clazz.getConstructors.mkString(", ")}"))            }
//        } yield ctor
//
//    /**
//     * Invoke a constructor with a specific parameter list.  For convenience, consider importing
//     * `com.eharmony.matching.featureSpecExtractor.reflect.{string2ParamType, class2ParamType}`.  Then you can attempt
//     * to get a constructor as follows:
//     *
//     * {{{
//     * import com.eharmony.matching.featureSpecExtractor.reflect.{string2ParamType, class2ParamType}
//     * import com.eharmony.matching.featureSpecExtractor.reflect.Instantiation._
//     *
//     * val className: String = ...
//     * val param1DownStreamClassName: String = ...
//     * val param2UpstreamClass: Class[String] = classOf[String]
//     * val ctor: Try[Constructor[_]] = constructor(className, param1DownStreamClassName, param2UpstreamClass)
//     *
//     * instantiate(ctor, param1, param2).
//     * }}}
//     *
//     * @param constructor name of the class to be instantiated.
//     * @param params list of parameters to pass as parameters to the constructor.
//     * @return
//     */
//    def instantiate(constructor: Try[Constructor[_]], params: AnyRef*): Try[AnyRef] =
//        for {
//            ctor <- constructor
//            inst <- instantiate(ctor, params:_*)
//        } yield inst
//
//    /**
//     * Like the above other instantiate function but with a constructor instead of a possible constructor.
//     * @param constructor a constructor to invoke
//     * @param params the parameters with which to invoke the constructor.
//     * @return
//     */
//    def instantiate(constructor: Constructor[_], params: AnyRef*): Try[AnyRef] =
//        Try { constructor.newInstance(params:_*).asInstanceOf[AnyRef] }
}
