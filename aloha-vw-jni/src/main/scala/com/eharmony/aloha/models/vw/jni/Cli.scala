package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha
import com.eharmony.aloha.annotate.CLI
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.reg.ConstantDeltaSpline
import org.apache.commons.vfs2.{FileObject, VFS}

/**
 * Created by rdeak on 6/15/15.
 */
@CLI(flag = "--vw")
object Cli {

    private[this] val CommandName = "vw"

    /**
     * '''NOTE''' null default values is only OK because both parameters are required
     * @param spec
     * @param model
     */
    case class Config(
        spec: FileObject = null,
        model: FileObject = null,
        id: Long = 0,
        name: String = "",
        vwArgs: Option[String] = None,
        numMissingThreshold: Option[Int] = None,
        notes: Vector[String] = Vector.empty,
        splineMin: Option[Double] = None,
        splineMax: Option[Double] = None,
        splineKnots: Option[Vector[Double]] = None)

    def main(args: Array[String]) {
        cliParser.parse(args, Config()) match {
            case Some(Config(spec, model, id, name, vwArgs, numMissingThresh, notesList, min, max, knots)) =>
                val spline = for (n <- min; x <- max; k <- knots) yield ConstantDeltaSpline(n, x, k)
                val notes = Option(notesList) filter {_.nonEmpty}
                val jsonAst = VwJniModel.json(spec, model, ModelId(id, name), vwArgs, numMissingThresh, notes, spline)
                println(jsonAst.compactPrint)
            case None => // Will be taken care of by scopt.
        }
    }

    private[this] def cliParser = {
        new scopt.OptionParser[Config](CommandName) {
            head(CommandName, aloha.version)
            opt[String]('s', "spec") action { (x, c) =>
                c.copy(spec = file(x))
            } text "spec is an Apache VFS URL to an aloha spec file." required()
            opt[String]('m', "model") action { (x, c) =>
                c.copy(model = file(x))
            } text "model is an Apache VFS URL to a VW binary model." required()
            opt[String]('n', "name") action { (x, c) =>
                c.copy(name = x)
            } text "name of the model." optional()
            opt[Long]('i', "id") action { (x, c) =>
                c.copy(id = x)
            } text "numeric id of the model." optional()
            opt[String]("vw-args") action { (x, c) =>
                c.copy(vwArgs = Some(x))
            } text "arguments to vw" optional()
            opt[Int]("num-missing-thresh") action { (x, c) =>
                c.copy(numMissingThreshold = Option(x))
            } text "number of missing features to allow before returning a 'no-prediction'." optional()
            opt[String]("note") action { (x, c) =>
                c.copy(notes = c.notes :+ x)
            } text "notes to add to the model. Can provide this many parameter times." unbounded() optional()
            opt[Double]("spline-min") action { (x, c) =>
                c.copy(splineMin = Option(x))
            } text "min value for spline domain. (must additional provide spline-max and spline-knots)." optional() maxOccurs(1)
            opt[Double]("spline-max") action { (x, c) =>
                c.copy(splineMax = Option(x))
            } text "max value for spline domain. (must additional provide spline-min and spline-knots)." optional() maxOccurs(1)
            opt[Seq[Double]]("spline-knots") action { (x, c) =>
                c.copy(splineKnots = Option(x.toVector))
            } validate { knots =>
                if (2 <= knots.size) Right(()) else Left("spline-knots must contain at least 2 knots.")
            } text "max value for spline domain. (must additional provide spline-min, spline-delta, and spline-knots)." optional() maxOccurs(1)
            checkConfig { c =>
                val splineProps = Seq(c.splineMin, c.splineMax, c.splineKnots).map(_.isDefined)
                if (!Seq(0, splineProps.size).contains(splineProps.count(identity)))
                    failure("All or no spline props should be supplied: spline-min, spline-max, spline-knots.")
                else if (c.splineMin.isDefined && c.splineMax.isDefined && !(c.splineMin.get < c.splineMax.get))
                    failure("spline-min must be strictly less than spline-max.")
                else success
            }
        }
    }

    private[this] def file(path: String) = VFS.getManager.resolveFile(path)
}
