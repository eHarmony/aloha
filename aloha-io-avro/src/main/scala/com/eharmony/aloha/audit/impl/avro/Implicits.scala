package com.eharmony.aloha.audit.impl.avro

import java.{util => ju}

import scala.collection.{immutable => sci}
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.annotation.tailrec

/**
  * Created by ryan.deak on 7/5/17.
  */
object Implicits {
  implicit class RichFlatScoreList(val flatScoreList: FlatScoreList) extends AnyVal {

    /**
      * Turn `flatScoreList` into a `Score`.
      * This function uses trampolining so it may stack overflow.  This should not happen in typical cases
      * when the depth of the score tree is relatively shallow.
      * @return
      */
    def toScore: Score = score(flatScoreList.getScores, 0)

    /**
      * Turn index `i` of `fss` into a `Score`.
      * Trampoline to `getSubs` to get the list of all `Score` formatted subscores.
      * @param fss list of all flattened scores.
      * @param i index of `FlatScore` to turn into a `Score`.
      * @return
      */
    private[this] def score(fss: ju.List[FlatScore], i: Int): Score = {
      val fs = fss.get(i)
      new Score(fs.getModel, fs.getValue, getSubs(fss, fs), fs.getErrorMsgs, fs.getErrorMsgs, fs.getProb)
    }

    /**
      * Get sub scores of `fs` as a java List of Score.
      * Trampoline to `score` to get the list of all `Score` formatted subscores.
      * @param fss list of all flattened scores.
      * @param fs current `FlatScore` to whose children should be turned into Score instances.
      * @return
      */
    private[this] def getSubs(fss: ju.List[FlatScore], fs: FlatScore): ju.List[Score] = {
      val subvals = fs.getSubvalues
      val n = subvals.size
      val out = new ju.ArrayList[Score](n)
      subvals.asScala.foreach { i =>
        val s = score(fss, i)
        out.add(s)
      }
      out
    }
  }

  implicit class RichScore(val score: Score) extends AnyVal {

    /**
      * Create a FlatScoreList in which the first element is the root node of the original `score`.
      * @return
      */
    def toFlatScoreList: FlatScoreList = bfsFlatten(Vector(score), 0, new ju.ArrayList[FlatScore])

    /**
      * Do a breadth-first (BFS) flattening so that children have consecutive indices in the final list.
      * @param q a queue for the BFS.
      * @param i current index in output array.  i + 1 indicates first available slot in which to insert children in
      *        the `out` list.
      * @param out list into which flattened score data is placed.
      * @return
      */
    @tailrec private[this] def bfsFlatten(q: Vector[Score],
                                          i: Int,
                                          out: ju.ArrayList[FlatScore]): FlatScoreList = {
      if (q.isEmpty)
        new FlatScoreList(out)
      else {
        val s = q.head
        val subs = s.getSubvalues
        val nSubs = if (null == subs) 0 else subs.size()
        val newI = i + nSubs
        val childInd = childIndices(i + 1 to newI)
        val fs = new FlatScore(s.getModel, s.getValue, childInd, s.getErrorMsgs, s.getMissingVarNames, s.getProb)
        out.add(fs)
        bfsFlatten(q.tail ++ subs.asScala, newI, out)
      }
    }

    private[this] def childIndices(indices: sci.IndexedSeq[Int]): ju.ArrayList[Integer] = {
      var i = 0
      val n = indices.size
      val out = new ju.ArrayList[Integer](n)
      while (i < n) {
        out.add(indices(i))
        i += 1
      }
      out
    }
  }
}
