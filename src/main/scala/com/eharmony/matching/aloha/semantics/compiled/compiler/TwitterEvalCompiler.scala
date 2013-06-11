package com.eharmony.matching.aloha.semantics.compiled.compiler

import java.io.File
import com.eharmony.matching.aloha.io.ContainerReadableByString
import scala.util.Try
import com.twitter.util.Eval

/** Compiles code using Twitter's Eval class from util-eval, wrapping the attempt in a Try.
  *
  * '''NOTE''': while thread local is a good idea for multi-threading purposes (creates one compiler per thread), one
  * needs to be careful about the number of threads interacting with the compiler.  This is true for two reasons:
      1 Creation time for compiler instance takes a while.
      1 ''compiler instances are big'' (translation, many threads + thread local == java.lang.OutOfMemoryError).
  *
  * Because the critical sections of the compile function of the StringCompiler.apply(*,*,*) function called by
  * Eval.applyProcessed(*,*,*) are synchronized, we shouldn't need to worry about thread-safety.  For more information,
  * see
  * [[https://github.com/twitter/util/blob/master/util-eval/src/main/scala/com/twitter/util/Eval.scala Eval on github]].
  * @param threadingModel a threading model used for compilation.
  * @param classCacheDir Directory where generated class files should be stored.
  * @param imports import statements.
  */
@throws[IllegalArgumentException]("When a class cache directory is specified but doesn't exist or is not RWX-able.")
case class TwitterEvalCompiler(threadingModel: EvalThreadingModel = SingleThreadedEval, classCacheDir: Option[File] = None, imports: Seq[String] = Nil) extends ContainerReadableByString[Try] {

    /** Java 0-param constructor.
      * @return
      */
    def this() = this(SingleThreadedEval, None, Nil)

    /** Java 1-param constructor specifying a class file caching directory.
      * @param classCacheDir Location to cache generated class files.
      * @return
      */
    def this(classCacheDir: File) = this(SingleThreadedEval, Option(classCacheDir), Nil)


    validateDirectory()

    private[this] val eval: Evaluator = threadingModel match {
        case SingleThreadedEval =>   new StandardEvaluator
        case ThreadLocalEval =>      new ThreadLocalEvaluator
        //TODO: case MultiThreadedEval(n) => new ActorEvaluator(n)
    }

    /** If we can cache the generated classes, then we should not reset the compiler state on every
      */
    private[this] val shouldReset = false

    /** Compile s into an instance of A.  If classCacheDir
      * @param code a string representing the to compile.
      * @tparam A the type of object to be generated
      * @return
      */
    def fromString[A](code: String) = eval[A](code)

    @throws[IllegalArgumentException]("When a class cache directory is specified but doesn't exist or is not RWX-able.")
    private[this] def validateDirectory() {
        classCacheDir.foreach(f =>
            (for {
                e <- f.exists      or s"Directory ${f} does not exist."
                d <- f.isDirectory or s"${f} is not a directory."
                r <- f.canRead     or s"Directory ${f} is not readable."
                w <- f.canWrite    or s"Directory ${f} is not writable."
                e <- f.canExecute  or s"Directory ${f} is not executable."
            } yield e).left.foreach(throw _)
        )
    }

    private[this] sealed trait Evaluator {
        def apply[A](code: String): Try[A]

        final protected[this] def className(code: String, idOpt: Option[Long] = None) = {
            "AlohaGeneratedFunction__" + uniqueId(code, idOpt)
        }

        /** This is a slightly modified version of the uniqueId in the com.twitter.util.Eval class. I would consider
          * that function to contain a bug as of 6.3.4 because the pattern matched statement doesn't seem right.
          * @param code
          * @param idOpt optional class suffix.  Long, instead of Int in Twitter's version, to allow unixtime-based dates.
          * @return `hex(SHA1(code)) + idOpt.map("_" + _).getOrElse("")`
          */
        final protected[this] def uniqueId(code: String, idOpt: Option[Long] = None): String = {
            val digest = java.security.MessageDigest.getInstance("SHA-1").digest(code.getBytes)
            val sha = BigInt(1, digest).toString(16)
            val id = sha + idOpt.map("_" + _).getOrElse("")
            id
        }
    }

    private[this] sealed trait EvaluatorContainer extends Evaluator {
        def get(): Eval
        final def apply[A](code: String) = Try { get.applyProcessed[A](className(code), code, shouldReset) }
    }

    private[this] class StandardEvaluator extends EvaluatorContainer {
        private[this] val e = new Eval(classCacheDir)
        def get() = e
    }

    /** The caller of the TwitterEvalWrapper implicitly dictates the number of Eval instances.  This happens because
      * one instance is created per calling thread.
      */
    private[this] class ThreadLocalEvaluator extends EvaluatorContainer {
        private[this] val tlE = new ThreadLocal[Eval] { override final protected def initialValue = new Eval(classCacheDir) }
        def get() = tlE.get
    }

    /** Placeholder for actor-based Evaluator.  This is beneficial because it would allow the instantiator of the
      * TwitterEvalWrapper instance to have control over exact number of compiler instances.
      * @param numActors
      */
    private[this] class ActorEvaluator(numActors: Int) extends Evaluator {
        throw new UnsupportedOperationException("ActorEvaluator not yet implemented.  Specify")

        // Internal pool of actors.

        /** Call to internal pool of actors that block on each call and return a result.
          * Maybe use a typed actor here...
          * @param code
          * @tparam A
          * @return
          */
        def apply[A](code: String) = throw new UnsupportedOperationException("not yet implemented")
    }
}
