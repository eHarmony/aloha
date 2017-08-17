package com.eharmony.aloha.audit.impl.tree

import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import org.junit.Assert._
import org.junit.Test
import scala.collection.{immutable => sci}

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
  def tree[U](modelId: ModelIdentity,
              errorMsgs: sci.Seq[String] = Nil,
              missingVarNames: Set[String] = Set.empty,
              value: Option[U] = None,
              subvalues: sci.Seq[Tree[U]] = Nil,
              prob: Option[Float] = None) =
  TreeImpl(modelId, errorMsgs, missingVarNames, value, subvalues, prob)

  private val FullBinaryTree =
    tree(ModelId(1), value = Some(1), subvalues = Vector(
      tree(ModelId(2), value = Some(2), subvalues = Vector(
        tree(ModelId(3), value = Some(3)),
        tree(ModelId(4), value = Some(4))
      )),
      tree(ModelId(5), value = Some(5), subvalues = Vector(
        tree(ModelId(6), value = Some(6)),
        tree(ModelId(7), value = Some(7))
      ))
    ))
}