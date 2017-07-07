package com.eharmony.aloha.audit.impl.avro

import java.{util => ju}

import scala.annotation.tailrec
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 7/5/17.
  */
object Implicits {
  implicit class RichFlatScore(val flatScore: FlatScore) extends AnyVal {

    /**
      * Turn `flatScore` into a `Score`.
      * This function uses trampolining so it may stack overflow.  This should not happen
      * in typical cases when the depth of the score tree is relatively shallow.
      * @return
      */
    def toScore: Score = {
      new Score(
        flatScore.getModel,
        flatScore.getValue,
        getSubs(flatScore.getDescendants, flatScore.getSubvalueIndices),
        flatScore.getErrorMsgs,
        flatScore.getMissingVarNames,
        flatScore.getProb)
    }
  }

  implicit class RichScore(val score: Score) extends AnyVal {

    /**
      * Create a FlatScore in which the first element is the root node of the original `score`.
      * @return
      */
    def toFlatScore: FlatScore = {
      val subs = score.getSubvalues.asScala.toVector
      val n = subs.size
      val descendants = bfsFlatten(subs, n, new ju.ArrayList[FlatScoreDescendant](n))
      new FlatScore(
        score.getModel,
        score.getValue,
        childIndices(0 until n),
        score.getErrorMsgs,
        score.getMissingVarNames,
        score.getProb,
        descendants)
    }
  }

  /**
    * Turn index `i` of `fsds` into a `Score`.
    * Trampoline to `getSubs` to get the list of all `Score` formatted subscores.
    * @param fsds list of all flattened scores.
    * @param i index of `FlatScore` to turn into a `Score`.
    * @return
    */
  private[this] def score(fsds: ju.List[FlatScoreDescendant], i: Int): Score = {
    val fs = fsds.get(i)
    new Score(
      fs.getModel,
      fs.getValue,
      getSubs(fsds, fs.getSubvalueIndices),
      fs.getErrorMsgs,
      fs.getMissingVarNames,
      fs.getProb)
  }

  /**
    * Get Scores for `fsds` based on the indices `ind` as a java List of Score.
    * Trampoline to `score` to get the list of all `Score` formatted subscores.
    * @param fsds list of all flattened scores.
    * @param ind indices to be turned into Score instances.
    * @return
    */
  private[this] def getSubs(fsds: ju.List[FlatScoreDescendant], ind: ju.List[Integer]): ju.List[Score] = {
    val n = ind.size
    val out = new ju.ArrayList[Score](n)
    var i = 0
    while (i < n) {
      out.add(score(fsds, ind.get(i)))
      i += 1
    }
    out
  }

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
                                        out: ju.ArrayList[FlatScoreDescendant]): ju.ArrayList[FlatScoreDescendant] = {
    if (q.isEmpty)
      out
    else {
      val s = q.head
      val subs = s.getSubvalues
      val nSubs = if (null == subs) 0 else subs.size()
      val newI = i + nSubs
      val childInd = childIndices(i until newI)
      val fs = new FlatScoreDescendant(s.getModel, s.getValue, childInd, s.getErrorMsgs, s.getMissingVarNames, s.getProb)
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
