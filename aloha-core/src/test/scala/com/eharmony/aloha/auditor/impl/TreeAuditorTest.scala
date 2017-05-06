package com.eharmony.aloha.auditor.impl

import com.eharmony.aloha.audit.impl.TreeAuditor.Tree
import com.eharmony.aloha.id.ModelId
import org.junit.Test
import org.junit.Assert._

/**
  * Created by ryan.deak on 5/5/17.
  */
class TreeAuditorTest {
  import TreeAuditorTest._

  @Test def testTreePathsOnFullBinTree(): Unit = {
    val paths = FullBinaryTree.paths.map(_.map(_.modelId.getId()))
    assertEquals(Vector(Vector(1,2,3), Vector(1,2,4), Vector(1,5,6), Vector(1,5,7)), paths)
  }

  @Test def testTreeNodesOnFullBinTree(): Unit = {
    val nodes = FullBinaryTree.nodes.map(_.modelId.getId())
    assertEquals(1 to 7, nodes)
  }
}

object TreeAuditorTest {
  private val FullBinaryTree =
    Tree(ModelId(1), value = Some(1), subvalues = Vector(
      Tree(ModelId(2), value = Some(2), subvalues = Vector(
        Tree(ModelId(3), value = Some(3)),
        Tree(ModelId(4), value = Some(4))
      )),
      Tree(ModelId(5), value = Some(5), subvalues = Vector(
        Tree(ModelId(6), value = Some(6)),
        Tree(ModelId(7), value = Some(7))
      ))
    ))
}