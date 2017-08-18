---
layout: docs
title: Documentation
---

# Getting Started (Data Engineers)

This is for data engineers that might be tasked with integrating Aloha into a scoring or prediction process
such as a prediction web service, etc.

For the purpose of constructing prediction pipelines, there are four main components:

- **Semantics** give meaning to features that appear in models.
- **Auditors** translate raw model predictions to an output type easily consumable by the model caller.
- **Factories** produce models.  Factories are parametrized by *semantics* and an *auditor*.
- **Models** make predictions.

## Semantics

Models can be created directly in Aloha but the preferred way to create a model is to specify model definitions
in JSON and use model factories to interpret the JSON to produce a model.  A
[BMI](https://en.wikipedia.org/wiki/Body_mass_index#History_and_usage_in_obesity_studies) feature in the JSON model
definition might look like the following:

```json
"703 * ${weight} / pow(${height}, 2)"
```

### CompiledSemantics

To interpret this feature, a `Semantics` instance is needed.  In Aloha, semantics implementations are parametrized
on the model input type, typically indicated by the type parameter `A` in the Aloha code.  The most common&mdash;and
presumably the most powerful&mdash;type of semantics is the `CompiledSemantics`, which performs code generation and
on-the-fly compilation at model creation time.  The compiled semantics is responsible parsing and emitting code
for the entire feature definition (like the one for BMI above).

Notice in the feature above that there are two *variables* on which the BMI calculation is based:
**height** and **weight**.  The compiled semantics in Aloha is modularized so that most of the machinery can
be reused but the interpretation of variables in a feature varies by domain.  Therefore, there are
`CompiledSemanticsPlugin`s for various input classes including CSV data, Protocol Buffer data, Avro data, etc.

> **Note**: *It is a rather heavyweight process to compile the features into functions.  It is time and memory
> intensive.  Once the models are all created by the model factory, it is recommended to not hold a reference to
> a model factory.  This disclaimer is noted here because it is the CompiledSemantics that embeds the Scala
compiler so it is the most expensive component.*

#### Creating CompiledSemanticsPlugin for Avro

Compiling Avro definitions to Java classes via Avro's `avro-tools` generates classes that implement both
[SpecificRecord](https://avro.apache.org/docs/current/api/java/org/apache/avro/specific/SpecificRecord.html) and
[GenericRecord](https://avro.apache.org/docs/current/api/java/org/apache/avro/generic/GenericRecord.html).  Aloha's
`CompiledSemanticsAvroPlugin` is based on `GenericRecord`.  While `GenericRecord`'s interface is untyped, Aloha uses
a provided Avro `Schema` to provide a typed interface.  So as long as the data adheres to the schema, Aloha will have
type information it can use to ensure features are well-typed.

```tut:invisible

def getSchema(): org.apache.avro.Schema = {
  import java.io.File
  val alohaRoot = new File(new File(".").getAbsolutePath.replaceFirst("/aloha/.*$", "/aloha/"))
  val protocolFile = new File(alohaRoot, "aloha-io-avro/src/test/resources/avro/test.avpr")
  val protocol = org.apache.avro.Protocol.parse(protocolFile)
  protocol.getType("Test")
}

def getCacheDir(): Option[java.io.File] = None
```

```tut:silent
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import com.eharmony.aloha.semantics.compiled.plugin.avro.CompiledSemanticsAvroPlugin

// Get an Avro Schema from somewhere.
val schema: Schema = getSchema()

// Here we use the GenericRecord but a more specific class can be used if desired.
val plugin = CompiledSemanticsAvroPlugin[GenericRecord](schema)
```

#### Creating CompiledSemanticsPlugin for Protocol Buffers

Compiling Protocol Buffers via `protoc` (2.4.1) yields Java classes that extend `GeneratedMessage`.
Aloha accepts types extending `GeneratedMessage` and can extract the type information without needing an
external schema.  This can be accomplished via:

```tut:silent
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.test.proto.TestProtoBuffs.TestProto // Some protobuf class

val plugin = CompiledSemanticsProtoPlugin[TestProto]
```


#### Creating CompiledSemanticsPlugin for CSV Data

To define a compiled semantics plugin for CSV Data, we need to specify a field name to type mapping.  Field order
isn't defined here as it's unnecessary at this point.  That defined by the data itself.  For more information, see
the [In-Depth Walkthrough](/aloha/docs/in_depth_walkthrough.html).

```tut:silent
import com.eharmony.aloha.semantics.compiled.plugin.csv.CompiledSemanticsCsvPlugin
import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvTypes.{IntType, FloatOptionType}

val plugin = CompiledSemanticsCsvPlugin(
  "height" -> IntType,          // Required integer field
  "weight" -> FloatOptionType   // Optional float field
)
```

#### Creating CompiledSemantics from a CompiledSemanticsPlugin

Once a compiled semantics plugin is created, we need to pass it to `CompiledSemantics`.  This is rather easy.  There
are a few additional things needed to do so:

- *a class cache directory* (optional, but suggested)
- *a compiler instance*
- *imports to be injected into features*: These will typically be determined by data scientists.


```tut:silent
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler

// If provided, needs to be an existing directory.
val cacheDir: Option[java.io.File] = getCacheDir()

val compiler = TwitterEvalCompiler(classCacheDir = cacheDir)

// BasicFunctions is not necessary but it's like Aloha's Predef.
// It is advisable to import this.  Additional import statements
// can be included to provide additional functionality, UDFs, etc.
val imports = Seq(
  "com.eharmony.aloha.feature.BasicFunctions._"
)

// An implicit execution context is needs by the semantics.
import concurrent.ExecutionContext.Implicits.global
val semantics = CompiledSemantics(compiler, plugin, imports)
```

## Auditor

Models are designed to return predictions in a data structure easy for the calling environment to consume.
Internally, models may produce predictions in some primitive format like 32-bit floats, but when the predictions
are returned by the model, they are boxed into some kind of container.  This is because models may have failures,
in which case a prediction may be impossible.  To differentiate between valid predictions and failures, Aloha
**DOES NOT** use sentinel or default values for primitive types.

Auditors are the component responsible for boxing scores into some kind of container.  Since models in Aloha can be
nested to form hierarchical models, the auditors need to handle this recursive structure.  Therefore, auditors are
deal with trees of data including the prediction itself as well as additional information about missing features,
errors messages and other diagnostic information.

The three main auditors are

- [OptionAuditor](/aloha/docs/api/index.html#com.eharmony.aloha.audit.impl.OptionAuditor)
- [proto.ScoreAuditor](/aloha/docs/api/index.html#com.eharmony.aloha.audit.impl.proto.ScoreAuditor)
- [AvroScoreAuditor](/aloha/docs/api/index.html#com.eharmony.aloha.audit.impl.avro.AvroScoreAuditor)

Many more are possible, for instance, there's a `TreeAuditor` in `aloha-core`'s `src/test/scala` directory
that encodes scores in trees.

### Creating an AvroScoreAuditor

```tut:silent
import com.eharmony.aloha.audit.impl.avro.AvroScoreAuditor

// Specify the type parameter.  Could be something else beside Double
val optAuditor: Option[AvroScoreAuditor[Double]] = AvroScoreAuditor[Double]
val auditor = optAuditor.get
```

### Creating a Protocol Buffer ScoreAuditor

```tut:silent
import com.eharmony.aloha.audit.impl.proto.ScoreAuditor

// Specify the type parameter.  Could be something else beside Double
val optAuditor: Option[ScoreAuditor[Double]] = ScoreAuditor[Double]
val auditor = optAuditor.get
```

### Creating an OptionAuditor

```tut:silent
import com.eharmony.aloha.audit.impl.OptionAuditor

// Specify the type parameter.  Could be something else beside Float
val auditor = OptionAuditor[Double]()
```

## ModelFactory

Model factories are provide the mechanism to parse an external JSON model definition.  To create, we need a
semantics and auditor as explained above.  Once we have these, we can create model factory very easily.
`ModelFactory.defaultFactory` finds all model definitions on the class path (via class path scanning)
and allows these types of models to be parsed.

```tut:silent
import com.eharmony.aloha.factory.ModelFactory

val modelFactory = ModelFactory.defaultFactory(semantics, auditor)
```

## Models

Models are the components that make predictions.  They are easy to create, given a model factory:

```tut:silent
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import concurrent.ExecutionContext.Implicits.global
import com.eharmony.aloha.semantics.compiled.plugin.csv.CompiledSemanticsCsvPlugin
import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvTypes.IntType
import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLines

val modelFile = {
  import java.io.File
  val alohaRoot = new File(new File(".").getAbsolutePath.replaceFirst("/aloha/.*$", "/aloha/"))
  new File(alohaRoot, "aloha-core/src/test/resources/fizzbuzz.json")
}

val compiler = TwitterEvalCompiler(classCacheDir = cacheDir)
val imports = Seq("com.eharmony.aloha.feature.BasicFunctions._", "scala.math._")
val plugin = CompiledSemanticsCsvPlugin("profile.user_id" -> IntType)
val semantics = CompiledSemantics(compiler, plugin, imports)
val auditor = OptionAuditor[Double]()
val modelFactory = ModelFactory.defaultFactory(semantics, auditor)
```

### Get Model from ModelFactory

```tut
// There are many other methods that retrieve models from InputStreams, Apache VFS, etc.
val modelTry = modelFactory.fromFile(modelFile)  // wrapped in a scala.util.Try
val model = modelTry.get
```

### Predict with Model

Models extend Scala's `Function1` trait so they have an `apply` method.  Since `.apply` can
be dropped in Scala code, we can call the predict function very easily:

```tut:silent
val Seq(x1, x2) = CsvLines(Map("profile.user_id" -> 0))(Seq("1", "2"))
```
```tut
val y1 = model(x1)
val y2 = model(x2)
```
