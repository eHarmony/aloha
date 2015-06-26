package com.eharmony.aloha

import concurrent.{ExecutionContext, Promise, promise, Future, future}
import collection.concurrent.TrieMap
import com.eharmony.aloha.reflect.{RefInfoOps, RefInfo}

/** A cache that is a hybrid of the Memoizer in Listing 5.19 in Java Concurrency in Practice and
  * https://www.bionicspirit.com/blog/2012/07/02/love-scala.html.  Makes sure to put a Promise
  * of the memoized value into the cache only if it's not already there.  Then only compute the
  * actual value (in a Future) and complete the Promise with the Future only if the promise was
  * successfully inserted atomically.  Otherwise, another promise was snuck in so use the Future
  * associated with that Promise.
  *
  * {{{
  * val cache = new NoEvictionCache
  * val slow = () => {Thread.sleep(5000); println("executing slow()"); 1}
  * val t1 = System.nanoTime                                      // Start the clock
  * val futures = Seq(                                            // Returns immediately (non-blocking)
  *       cache("one"){slow()},                                   // Only execute slow() once.
  *       cache("one"){slow()})
  * val a = Future.fold(futures)(0)(_+_)                          // Returns immediately (non-blocking)
  * val t2 = System.nanoTime
  * println("Runtime for setup: " + (1.0e-9*(t2-t1)).toFloat)     // Prints: "Runtime for setup: 0.001041"
  * a.onSuccess{ case i =>
  *   val t3 = System.nanoTime                                    // Determine elapsed time.
  *   println(i + ", runtime: " + (1.0e-9*(t3-t1)).toFloat)       // Prints: "2, runtime: 5.002082"
  * }
  * }}}
  * @param ec an execution context in which to run the futures that are created.
  */
class NoEvictionCache(implicit ec: ExecutionContext) {

    /**
     * The first string in the key is the "key" variable passed to apply.  The second is the string
     * representation of the TypeTag.
     */
    private[this] val cache = TrieMap.empty[(String, String), Promise[_]]

    /** Cache a value.
      * @param key string identifier
      * @param a call-by-name value to be cached or retrieved from the cache.
      * @tparam A type of value to be memoized
      * @return a Future containing the result of a
      */
    def apply[A: RefInfo](key: String)(a: => A): Future[A] = {
        val k = typedKey(key)
        val f = cache.getOrElse(k, createAndCache(k, a)).asInstanceOf[Future[A]]
        f
    }

    /**
      * @param k the key in the cache.  This is a combination of the string ID and the TypeTag string
      * representation.
      * @param a call-by-name value to be executed and cached or to be retrieved from the cache.
      * @tparam A type of value to be memoized
      * @return the promise in the cache associated with k
      */
    @inline private[this] def createAndCache[A](k: (String, String), a: => A): Promise[_] = {
        val p = promise[A]
        val op = cache.putIfAbsent(k, p)                // Only insert p if another promise didn't race in.
        val p1 = op.getOrElse(p completeWith future(a)) // Only execute a if another promise didn't race in.
        logging(k, op.isEmpty)                          // Can be overridden for testing if necessary.
        p1
    }

    /** Generate a cache key.
      * @param s string key
      * @tparam A type of value to be memoized
      * @return
      */
    @inline private[this] def typedKey[A: RefInfo](s: String) = (s, RefInfoOps.toString[A])

    /** Made protected so that it can be overridden in a test class to test caching.
      * @param k
      * @param inserting
      * @tparam A
      */
    protected[this] def logging[A](k: (String, String), inserting: Boolean) {
        // TODO: Logging // println("caching: " + k)
    }
}
