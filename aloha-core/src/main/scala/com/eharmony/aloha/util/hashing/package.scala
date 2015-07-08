package com.eharmony.aloha.util

package object hashing {

    /**
     * This is copied from ''scala.util.hashing.MurmurHash3''.  This will insulate from changes
     * to the seed but not the algorithm.
     */
    private[this] final val stringSalt = 0xf7ca7fd2

    /**
     * Created by rdeak on 7/7/15.
     */
    trait HashFunction {
        def stringHash(s: String): Int

        /**
         * A string containing all applicable salts.  No specific format should be assumed for this string.  It
         * Should be used for logging and isn't intended to be human readable.
         * @return
         */
        def salts: String
    }

    class MurmurHash3(salt: Int = stringSalt) extends HashFunction {

        /**
         * This should have the same function as ''scala.util.hashing.MurmurHash3.stringHash(s)''.
         * @param s a string to hash.
         * @return the 32-bit murmurhash3 result.
         */
        override def stringHash(s: String) = scala.util.hashing.MurmurHash3.stringHash(s, salt)

        override def salts = s"${getClass.getSimpleName}: stringSalt=$salt"
    }

    object MurmurHash3 extends MurmurHash3(stringSalt)
}
