package com.eharmony.aloha.util.io

import java.io.{ByteArrayOutputStream, PrintStream}

import scala.util.Try

/**
 *
 * @author R M Deak
 */
trait TestWithIoCapture {

    /**
      * A PrintStream that allows the contents to be interrogated.
      * @param baos a backing ByteArrayOutputStream whose contents are exposed in the
      *             `contents` method.
      */
    protected[this] case class InterrogatablePrintStream(baos: ByteArrayOutputStream) extends PrintStream(baos) {
        def contents: String = {
            baos.flush()
            new String(baos.toByteArray)
        }
    }

    protected[this] implicit def interrogatablePrintStream: InterrogatablePrintStream =
        InterrogatablePrintStream(new ByteArrayOutputStream)

    /**
      * The result of a [[IoTrappedRun]].
      * @param result result wrapped in a Try.
      * @param out the output stream.
      * @param err the error stream.
      * @tparam A the result type.
      * @tparam B the stream type.
      */
    protected[this] case class RunResult[A, B <: PrintStream](result: Try[A], out: B, err: B)

    /**
      * @param f a function to be run where the output is desired.
      * @tparam A the function output type.
      */
    protected[this] case class IoTrappedRun[A](f: () => A) {
        /**
          * Run the function `f` with the output and error stream in Console replaced with
          * the ones provided.
          * @param out the output stream.
          * @param err the error stream.
          * @tparam B the type of PrintStram.
          * @return a [[RunResult]] is the desired type of PrintStream instances that
          *         were used during the running of `f`.
          */
        def withOutput[B <: PrintStream](implicit out: B, err: B): RunResult[A, B] = {
            // Ensure that the Console gets reset properly by wrapping the call to f in a Try.
            Console.withOut(out) {
                Console.withErr(err) {
                    val t = Try { f() }
                    RunResult(t, out, err)
                }
            }
        }
    }

    /**
      * Evaluate a ''call-by-name'' parameter with the output and error streams returned.
      * This allows the caller to interrogate the contents of the streams.  For instance:
      *
      * {{{
      * val main: Array[String] => Unit = ???
      * val args: Array[String}] = ???
      * val res = evaluate(main(args)).withOutput[InterrogatablePrintStream]
      * println(res.out.contents)
      * }}}
      * @param r the value to evaluate.
      * @tparam A output type `r`.
      * @return a [[IoTrappedRun]] based on the value `r`.
      */
    protected[this] def evaluate[A](r: => A): IoTrappedRun[A] = IoTrappedRun(() => r)

    /**
      * Call a function on the provided input with the output and error streams returned.
      * This allows the caller to interrogate the contents of the streams.  For instance:
      *
      * {{{
      * val main: Array[String] => Unit = ???
      * val args: Array[String}] = ???
      * val res = run(main)(args)
      * println(res.out.contents)
      * }}}
      * @param f a function to run.
      * @tparam A input type of the function
      * @tparam B output type of the function
      * @return a function that when given an `A` returns a [[RunResult]].
      */
    protected[this] def run[A, B](f: A => B): (A) => RunResult[B, InterrogatablePrintStream] =
        (a: A) => evaluate(f(a)).withOutput[InterrogatablePrintStream]
}
