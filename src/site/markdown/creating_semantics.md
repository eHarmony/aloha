# Creating Semantics

---

## Scala

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol.DoubleJsonFormat
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter

val semantics = CompiledSemantics(TwitterEvalCompiler(), 
                                  CompiledSemanticsCsvPlugin(columnTypes), 
                                  Seq("com.eharmony.aloha.feature.BasicFunctions._"))
val factory = ModelFactory(RegressionModel.parser).toTypedFactory[CsvLine, Double](semantics)
```

## Java

```java

```

---
