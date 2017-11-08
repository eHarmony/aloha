package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.File

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.dataset.vw.multilabel.VwDownsampledMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.json.VwDownsampledMultilabeledJson
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.semantics.compiled.CompiledSemanticsInstances
import org.apache.commons.vfs2.VFS
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Assert.{assertEquals, fail}
import spray.json.pimpString
import spray.json.DefaultJsonProtocol.IntJsonFormat
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.collection.breakOut
import scala.util.Random

/**
  * Created by ryan.deak on 11/6/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelDownsampledModelTest {

  import VwMultilabelDownsampledModelTest.EndToEndDatasetSpec

  @Test def test(): Unit = {

    // ------------------------------------------------------------------------------------
    //  Preliminaries
    // ------------------------------------------------------------------------------------

    type Lab = Int
    type Dom = Vector[Lab]  // Not an abuse of notation.  Domain is indeed a vector of labels.

    val semantics = CompiledSemanticsInstances.anyNameIdentitySemantics[Dom]
    val optAud = OptionAuditor[Map[Lab, Double]]()

    // 10 passes over the data, sampling the negatives.
    val repetitions = 8


    // ------------------------------------------------------------------------------------
    //  Dataset and test example set up.
    // ------------------------------------------------------------------------------------

    // Marginal Distribution:          Pr[8] = 0.80 = 20 / 25 = (12 + 8) / 25
    //                                 Pr[4] = 0.40 = 10 / 25 = ( 2 + 8) / 25
    //
    val unshuffledTrainingSet: Seq[Dom] = Seq(
      Vector(    )  -> 3,   //            pr = 0.12 =  3 / 25   <-- JPD
      Vector(   8)  -> 12,  //            pr = 0.48 = 12 / 25
      Vector(4   )  -> 2,   //            pr = 0.08 =  2 / 25
      Vector(4, 8)  -> 8    //            pr = 0.32 =  8 / 25
    ) flatMap {
      case (k, n) => Vector.fill(n * repetitions)(k)
    }

    val trainingSet = new Random(0x0912e40f395bL).shuffle(unshuffledTrainingSet)

    val labelsInTrainingSet = trainingSet.flatten.toSet.toVector.sorted

    val testExample: Dom = Vector.empty

    val marginalDist = labelsInTrainingSet.map { label =>
      val z = trainingSet.size.toDouble
      label -> trainingSet.count(row => row contains label) / z
    }.toMap

    // ------------------------------------------------------------------------------------
    //  Prepare dataset specification and read training dataset.
    // ------------------------------------------------------------------------------------

    val datasetSpec = VFS.getManager.resolveFile(EndToEndDatasetSpec)
    val datasetJson =
      StringReadable
        .fromVfs2(datasetSpec)
        .parseJson
        .convertTo[VwDownsampledMultilabeledJson]

    val rc =
      new VwDownsampledMultilabelRowCreator.Producer[Dom, Lab](labelsInTrainingSet, () => 0L).
        getRowCreator(semantics, datasetJson).get


    // ------------------------------------------------------------------------------------
    //  Prepare parameters for VW model that will be trained.
    // ------------------------------------------------------------------------------------

    val binaryVwModel = File.createTempFile("vw_", ".bin.model")
    binaryVwModel.deleteOnExit()

    val cacheFile = File.createTempFile("vw_", ".cache")
    cacheFile.deleteOnExit()

    val origParams =
      s"""
         | --quiet
         | --cache_file ${cacheFile.getCanonicalPath}
         | --holdout_off
         | --passes 10
         | --learning_rate 5
         | --decay_learning_rate 0.9
         | --csoaa_ldf mc
         | --loss_function logistic
         | -f ${binaryVwModel.getCanonicalPath}
       """.stripMargin.trim.replaceAll("\n", " ")

    // Get the namespace names from the row creator.
    val nsNames = rc.namespaces.map(_._1)(breakOut): Set[String]

    // Take the parameters and augment with additional parameters to make
    // multilabel w/ probabilities work correctly.
    val vwParams = VwMultilabelModel.updatedVwParams(origParams, nsNames, 2) fold (
      e => throw new Exception(e.errorMessage),
      ps => ps
    )

    // ------------------------------------------------------------------------------------
    //  Train VW model
    // ------------------------------------------------------------------------------------

    // Get the iterator of the examples produced.  This is similar to what one may do
    // within a `mapPartitions` in Spark.
    val examples = rc.statefulMap(trainingSet.iterator, rc.initialState) collect {
      case ((_, Some(x)), _) => x
    }

    val vwLearner = VWLearners.create[VWActionScoresLearner](vwParams)

    examples foreach { yx =>
      vwLearner.learn(yx)
    }

    vwLearner.close()


    // ------------------------------------------------------------------------------------
    //  Create Aloha model JSON
    // ------------------------------------------------------------------------------------

    val modelJson = VwMultilabelModel.json(
      datasetSpec = Vfs.apacheVfs2ToAloha(datasetSpec),
      binaryVwModel = Vfs.javaFileToAloha(binaryVwModel),
      id = ModelId(1, "NONE"),
      labelsInTrainingSet = labelsInTrainingSet,
      labelsOfInterest = Option.empty[String],
      externalModel = false,
      numMissingThreshold = Option(0)
    )

    // ------------------------------------------------------------------------------------
    //  Instantiate Aloha Model
    // ------------------------------------------------------------------------------------

    val factory = ModelFactory.defaultFactory(semantics, optAud)
    val modelTry = factory.fromString(modelJson.prettyPrint) // Use `.compactPrint` in prod.
    val model = modelTry.get


    // ------------------------------------------------------------------------------------
    //  Test Aloha Model
    // ------------------------------------------------------------------------------------

    val output = model(testExample)
    model.close()

    output match {
      case None => fail()
      case Some(m) =>
        assertEquals(marginalDist.keySet, m.keySet)
        marginalDist foreach { case (k, v) =>
          assertEquals(s"For key '$k':", v, m(k), 0.01)
        }
    }
  }
}

object VwMultilabelDownsampledModelTest {
  private val EndToEndDatasetSpec =
    "res:com/eharmony/aloha/models/vw/jni/multilabel/downsampled_neg_dataset_spec.json"
}
