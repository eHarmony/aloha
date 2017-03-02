package com.eharmony.aloha.reflect

import com.eharmony.aloha.reflect.RefInfoOps.{isImmutableIterableButNotMap => test}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.{immutable => sci}

/**
  * Created by deak on 3/1/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class RefInfoOpsTest {

  // Tests for RefInfoOps.isImmutableIterableButNotMap

  // Note sci.Stream.Empty and sci.Nil are not considered because the are Nothing
  @Test def sciEmpty_false(): Unit = assertFalse(test[Int, sci.Stream.Empty.type])
  @Test def sciNil_false(): Unit = assertFalse(test[Int, sci.Nil.type])

  @Test def sciDefaultMap_false(): Unit = assertFalse(test[Int, sci.DefaultMap[Int, Int]])
  @Test def sciHashMap_false(): Unit = assertFalse(test[Int, sci.HashMap[Int, Int]])
  @Test def sciHashMap1_false(): Unit = assertFalse(test[Int, sci.HashMap.HashMap1[Int, Int]])
  @Test def sciHashTrieMap_false(): Unit = assertFalse(test[Int, sci.HashMap.HashTrieMap[Int, Int]])
  @Test def sciIntMap_false(): Unit = assertFalse(test[Int, sci.IntMap[Int]])
  @Test def sciListMap_false(): Unit = assertFalse(test[Int, sci.ListMap[Int, Int]])
  @Test def sciLongMap_false(): Unit = assertFalse(test[Int, sci.LongMap[Int]])
  @Test def sciMap_false(): Unit = assertFalse(test[Int, sci.Map[Int, Int]])
  @Test def sciMap1_false(): Unit = assertFalse(test[Int, sci.Map.Map1[Int, Int]])
  @Test def sciMap2_false(): Unit = assertFalse(test[Int, sci.Map.Map2[Int, Int]])
  @Test def sciMap3_false(): Unit = assertFalse(test[Int, sci.Map.Map3[Int, Int]])
  @Test def sciMap4_false(): Unit = assertFalse(test[Int, sci.Map.Map4[Int, Int]])
  @Test def sciMapProxy_false(): Unit = assertFalse(test[Int, sci.MapProxy[Int, Int]])

  @Test def sciBitSet_true(): Unit = assertTrue(test[Int, sci.BitSet])
  @Test def sciBitSet1_true(): Unit = assertTrue(test[Int, sci.BitSet.BitSet1])
  @Test def sciBitSet2_true(): Unit = assertTrue(test[Int, sci.BitSet.BitSet2])
  @Test def sciBitSetN_true(): Unit = assertTrue(test[Int, sci.BitSet.BitSetN])
  @Test def sciRange_true(): Unit = assertTrue(test[Int, sci.Range])
  @Test def sciSortedMap_false(): Unit = assertFalse(test[Int, sci.SortedMap[Int, Int]])
  @Test def sciTreeMap_false(): Unit = assertFalse(test[Int, sci.TreeMap[Int, Int]])
  @Test def sciWithDefault_false(): Unit = assertFalse(test[Int, sci.Map.WithDefault[Int, Int]])
  @Test def sciWrappedString_false(): Unit = assertFalse(test[String, sci.WrappedString])
  @Test def sciNumericRange_true(): Unit = assertTrue(test[Int, sci.NumericRange[Int]])
  @Test def sciLinearSeq_true(): Unit = assertTrue(test[Int, sci.LinearSeq[Int]])
  @Test def sciColonColon_true(): Unit = assertTrue(test[Int, sci.::[Int]])
  @Test def sciCons_true(): Unit = assertTrue(test[Int, sci.Stream.Cons[Int]])
  @Test def sciExclusive_true(): Unit = assertTrue(test[Int, sci.NumericRange.Exclusive[Int]])
  @Test def sciHashSet_true(): Unit = assertTrue(test[Int, sci.HashSet[Int]])
  @Test def sciHashTrieSet_true(): Unit = assertTrue(test[Int, sci.HashSet.HashTrieSet[Int]])
  @Test def sciImpl_true(): Unit = assertTrue(test[Int, sci.IndexedSeq.Impl[Int]])
  @Test def sciInclusive_true(): Unit = assertTrue(test[Int, sci.NumericRange.Inclusive[Int]])
  @Test def sciIndexedSeq_true(): Unit = assertTrue(test[Int, sci.IndexedSeq[Int]])
  @Test def sciList_true(): Unit = assertTrue(test[Int, sci.List[Int]])
  @Test def sciListSet_true(): Unit = assertTrue(test[Int, sci.ListSet[Int]])
  @Test def sciQueue_true(): Unit = assertTrue(test[Int, sci.Queue[Int]])
  @Test def sciSeq_true(): Unit = assertTrue(test[Int, sci.Seq[Int]])
  @Test def sciSet_true(): Unit = assertTrue(test[Int, sci.Set[Int]])
  @Test def sciSet1_true(): Unit = assertTrue(test[Int, sci.Set.Set1[Int]])
  @Test def sciSet2_true(): Unit = assertTrue(test[Int, sci.Set.Set2[Int]])
  @Test def sciSet3_true(): Unit = assertTrue(test[Int, sci.Set.Set3[Int]])
  @Test def sciSet4_true(): Unit = assertTrue(test[Int, sci.Set.Set4[Int]])
  @Test def sciSetProxy_true(): Unit = assertTrue(test[Int, sci.SetProxy[Int]])
  @Test def sciSortedSet_true(): Unit = assertTrue(test[Int, sci.SortedSet[Int]])
  @Test def sciStack_true(): Unit = assertTrue(test[Int, sci.Stack[Int]])
  @Test def sciStream_true(): Unit = assertTrue(test[Int, sci.Stream[Int]])
  @Test def sciTreeSet_true(): Unit = assertTrue(test[Int, sci.TreeSet[Int]])
  @Test def sciVector_true(): Unit = assertTrue(test[Int, sci.Vector[Int]])
}
