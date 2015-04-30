package com.eharmony.matching.featureSpecExtractor.reflect

object ReflectiveSemanticsFactory {
//    private[this] val protoPluginClass = "com.eharmony.matching.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin"
//    private[this] val pluginClass = "com.eharmony.matching.aloha.semantics.compiled.CompiledSemanticsPlugin"
//    private[this] val compilerClass = "com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler"
//    private[this] val compiledSemanticsClass = "com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics"
//    private[this] val containerReadableClass = "com.eharmony.matching.aloha.io.ContainerReadable"
//
//    private[this] def instantiatePlugin(msgClass: Class[_], dereferenceAsOptional: Boolean = true) = instantiate(
//        constructor(protoPluginClass, classOf[Class[_]], java.lang.Boolean.TYPE),
//        msgClass,
//        Boolean.box(dereferenceAsOptional)
//    )
//
//    private[this] def instantiateCompiler(classCacheDir: Option[File] = None) =
//        classCacheDir map {
//            cache => instantiate(constructor(compilerClass, classOf[File]), cache)
//        } getOrElse {
//            instantiate(constructor(compilerClass))
//        }
//
//    def instantiateSemantics[A <: GeneratedMessage](
//            msgClass: Class[A],
//            dereferenceAsOptional: Boolean = true,
//            provideSemanticsUdfException: Boolean = true,
//            classCacheDir: Option[File] = None,
//            imports: Array[String] = Array.empty): Try[Semantics[A]] = {
//        for {
//            ctor <- constructor(
//                        compiledSemanticsClass,
//                        containerReadableClass,
//                        pluginClass,
//                        classOf[Array[String]],
//                        java.lang.Boolean.TYPE,
//                        classOf[scala.concurrent.ExecutionContext]
//                    )
//            compiler <- instantiateCompiler(classCacheDir)
//            plugin <- instantiatePlugin(msgClass, dereferenceAsOptional)
//            inst <- instantiate(
//                        ctor,
//                        compiler,
//                        plugin,
//                        imports.clone(),
//                        Boolean.box(provideSemanticsUdfException),
//                        scala.concurrent.ExecutionContext.global
//                    ).map(_.asInstanceOf[Semantics[A]])
//        } yield inst
//    }
}
