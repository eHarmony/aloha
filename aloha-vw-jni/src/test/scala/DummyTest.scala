import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan.deak on 8/18/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class DummyTest {
  @Test def outputJavaLibraryPath(): Unit = {
    println("===JAVA LIBRARY PATH===\n" + System.getProperty("java.library.path"))
  }
}
