package com.eharmony.aloha.semantics.compiled.compiler

/** The type of threading model to use for compilation within the
  * [[com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler]].
  */
sealed trait EvalThreadingModel

/** Creates a single instance of com.twitter.util.Eval within the
  * [[com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler]] instance.
  */
case object SingleThreadedEval extends EvalThreadingModel

/** Creates an instance of com.twitter.util.Eval per calling thread to the
  * [[com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler]] instance.
  */
case object ThreadLocalEval extends EvalThreadingModel
// TODO: Implement multi-threaded evaluation case class MultiThreadedEval(numThreads: Int) extends EvalThreadingModel
