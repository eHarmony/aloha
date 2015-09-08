//scala -feature -deprecation \
//  -cp $HOME/.m2/repository/com/google/protobuf/protobuf-java/2.4.1/protobuf-java-2.4.1.jar:\
//$HOME/.m2/repository/commons-codec/commons-codec/1.3/commons-codec-1.3.jar:\
//$PWD/aloha-core/target/test-classes/

import scala.language.implicitConversions
import scala.util.Random

import java.io.{BufferedWriter, FileWriter}
import java.net.URL

import org.apache.commons.codec.binary.Base64
import com.google.protobuf.GeneratedMessage
import com.eharmony.aloha.test.proto.AbaloneProtos.Abalone
import Abalone.Sex.{MALE, FEMALE, INFANT}

object DatasetCreator {
  val MaxSentenceLength = 10
  val MinSentenceLength = 4

  val numberWords = Vector("zero", "one", "two", "three", "four", 
                           "five", "six", "seven", "eight", "nine",
                           "ten", "eleven", "twelve", "thirteen", "fourteen", 
                           "fifteen", "sixteen", "seventeen", "eightteen", "nineteen",
                           "twenty", "twenty-one", "twenty-two", "twenty-three", "twenty-four", 
                           "twenty-five", "twenty-six", "twenty-seven", "twenty-eight", "twenty-nine",
                           "thirty")
                           
  val numberWordsCaesar = (new Random(0)).shuffle(numberWords)

  lazy val words = scala.io.Source.fromFile("1000_words.txt").getLines.drop(1).toVector
  lazy val numRandWords = words.size

  implicit def string2float(s: String): Float = augmentString(s).toFloat
  implicit def string2int(s: String): Int = augmentString(s).toInt

  def sex(s: String) = s match {
    case "M" => MALE
    case "F" => FEMALE
    case "I" => INFANT
  }

  def getSomeText(rings: Int, i: Int, caesarCipher: Boolean = false) = {
    val r = new Random(i)
    val numWords = MinSentenceLength - 1 + r.nextInt(MaxSentenceLength - MinSentenceLength + 1)
    val n = if (caesarCipher) numberWordsCaesar(rings) else numberWords(rings)
    val w = r.shuffle(n +: Vector.fill(numWords)(words(r.nextInt(numRandWords))))
    w.mkString(" ")
  }

  def createAbaloneProto(caesarCipher: Boolean = false) = ((s: String, i: Int) => {
    val f = s.split(",")
    val w = Abalone.Weight.newBuilder
    w.setWhole(f(4)).
      setShucked(f(5)).
      setViscera(f(6)).
      setShell(f(7))
      
    val a = Abalone.newBuilder
    a.setSex(sex(f(0))).
      setLength(f(1)).
      setDiameter(f(2)).
      setHeight(f(3)).
      setWeight(w).
      setRings(f(8)).
      setSomeText(getSomeText(f(8), i, caesarCipher)).
      build()  
  }).tupled
  
  def encode(m: GeneratedMessage) = new String(Base64.encodeBase64(m.toByteArray))
  
  def createDataset(fileName: String, caesarCipher: Boolean = false) = {
    val w = new BufferedWriter(new FileWriter(fileName))
    scala.io.Source.fromURL(new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/abalone/abalone.data")).getLines.zipWithIndex.
      map { createAbaloneProto(caesarCipher) }.
      map { encode }.
      foreach { d => w.append(d).append("\n").flush() }
    w.close
  }
  
  def readDataset(fileName: String): Iterator[Abalone] = 
    scala.io.Source.fromFile(fileName).getLines().map(l => Abalone.parseFrom(Base64.decodeBase64(l.getBytes)))
}

