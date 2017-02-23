# Creating Semantics

---

## Scala

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol.DoubleJsonFormat

val semantics = CompiledSemantics(TwitterEvalCompiler(),
                                  CompiledSemanticsCsvPlugin(columnTypes),
                                  Seq("com.eharmony.aloha.feature.BasicFunctions._"))
```

## Java

```java

```

---
