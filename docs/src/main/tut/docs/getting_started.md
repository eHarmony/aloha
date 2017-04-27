---
layout: docs
title: Documentation
---

# Getting Started

This section will walk through downloading and building Aloha as well as doing some simple tasks in the command
line interface (CLI) like generating datasets, creating models and using models to make predictions.

## Build Prerequisites

Aloha uses [SBT](http://www.scala-sbt.org) for building.


### Installing SBT on a *Mac*

The preferred way to install SBT on a *Mac* is by using the [Homebrew](http://http://brew.sh) package manager.  If
you have Homebrew installed, just install via:

```bash
brew install sbt
```

### Installing Homebrew on a *Mac*

If you don't have Homebrew installed, follow the instructions at [http://brew.sh](http://brew.sh).


### Installing SBT Manually

[Download it here](http://www.scala-sbt.org/download.html).


## Get Aloha from Source

```bash
git clone git@github.com:eHarmony/aloha.git
cd aloha
```


### Installing to Ivy Local Cache

This can be accomplished via one of the following.  Choose the most appropriate for you.

```bash
sbt ++2.11.8 clean publishLocal  # For Scala 2.11 version
sbt ++2.10.5 clean publishLocal  # For Scala 2.10 version
sbt +clean +publishLocal         # For all Scala versions
```

### Installing to Maven Local Repository

Similarly, you can publish to you local Maven repository via one of the following.

```bash
sbt ++2.11.8 clean publishM2  # For Scala 2.11 version
sbt ++2.10.5 clean publishM2  # For Scala 2.10 version
sbt +clean +publishM2         # For all Scala versions
```

## Generating a Local Copy of the Documentation

### Prerequisites

Aloha uses [sbt-microsites](https://47deg.github.io/sbt-microsites/) to generate site documentation.  To create
locally, you'll need [Jekyll](https://jekyllrb.com/).  There are a bunch of ways to get Jekyll, but the easiest
is with one of the following:

```bash
gem install jekyll        # Via ruby gems installer
yum install jekyll        # via Yum, typically on RedHat and variants
apt-get install jekyll    # via apt-get, typically on Debian and variants
```

### Site Generation

```bash
sbt makeMicrosite && jekyll serve -s docs/target/site -d docs/target/site/_site
```

Then you should be able to open a browser to [http://localhost:4000/aloha/](http://localhost:4000/aloha/) to see the
documentation.  To stop the webserver provided by Jekyll, Just press `ctrl-c`.

## Create a VW Dataset

> **NOTE**: *CLI* examples require running `sbt test:compile` prior to execution.  This is necessary because Aloha
> no longer publishes a *CLI* uberjar.  Therefore, we want the Aloha classfiles to be on the classpath.


*[eHarmony](http://www.eharmony.com)* uses [Vowpal Wabbit](https://github.com/JohnLangford/vowpal_wabbit/wiki) for many
of its predictive tasks so Aloha provides support for VW including dataset creation and native VW model creation on the 
[JVM](https://en.wikipedia.org/wiki/Java_virtual_machine).

<span class="label">bash script</span>

```bash
# The classpath argument contains the protocol buffer definitions.
#
aloha-cli/bin/aloha-cli                                  \
  -cp 'aloha-io-proto/target/scala-2.11/test-classes'    \
  --dataset                                              \
  -s $(find $PWD/aloha-cli/src -name 'proto_spec2.json') \
  -p 'com.eharmony.aloha.test.proto.Testing.UserProto'   \
  -i $(find $PWD/aloha-core/src -name 'fizz_buzzs.proto')\
  --vw_labeled /tmp/dataset.vw
```

<span class="label label-success">/tmp/dataset.vw</span>

<pre>
1 1| name=Alan gender=MALE bmi:23 |photos num_photos:2 avg_photo_height
1 1| name=Kate gender=FEMALE bmi=UNK |photos num_photos avg_photo_height:3
</pre>

Let's break down the preceding example.  `-cp` was specified to provide additional classpath elements.  This argument
is currently required can be `''` if no additional classpath elements are required.  In this example, we included the
directory that contains the necessary protocol buffer classfiles for `com.eharmony.aloha.test.proto.Testing.UserProto`.

After the `-cp` flag always comes the subtask.  Here the subtask is creating a dataset, denoted by the `--dataset`
flag.  To see additional subtasks available, you can omit the subtask flag and the subsequent parameters and the
Aloha CLI will provide sensible context-sensitive error messages.

`-s` tells where to get the Aloha feature specification file that will be used to extract data from the protocol
buffer instances provided as the raw data.  This file can be local or remote and is an
[Apache VFS](https://commons.apache.org/proper/commons-vfs/) URL.

`-p` along with the [canonical class name](https://docs.oracle.com/javase/7/docs/api/java/lang/Class.html#getCanonicalName\(\))
tells that the input type of the raw data is line-separated base64-encoded protocol buffer input.

`-i` tells where the raw input data resides.  If the `-i` argument is omitted, the CLI reads from standard
input.  This is nice when you want to pipe input to the Aloha CLI from another process.  Again, if supplied, Aloha
expects the argument to be and Apache VFS URL.

Finally, `--vw_labeled` tells Aloha to generated a labeled VW dataset and output it to `tmp/dataset.vw`.  Like with
other *\*nix* commands,  you can provide `-` as a value and the CLI will output to standard out.


### Create a VW dataset programmatically

```tut:silent
import java.io.File
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import com.google.protobuf.GeneratedMessage
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.dataset.{RowCreator, RowCreatorProducer, RowCreatorBuilder}
import com.eharmony.aloha.dataset.vw.labeled.VwLabelRowCreator
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.test.proto.Testing.UserProto
import com.eharmony.aloha.dataset.MissingAndErroneousFeatureInfo

def getRowCreator[T <: GeneratedMessage : RefInfo, S <: RowCreator[T]](
    producers: List[RowCreatorProducer[T, S]], 
    alohaJsonSpecFile: File,
    alohaCacheDir: Option[File] = None): Try[S] = {
  val plugin = CompiledSemanticsProtoPlugin[T]
  val compiler = TwitterEvalCompiler(classCacheDir = alohaCacheDir)
  val imports: Seq[String] = Nil // Imports for UDFs to use in extraction functions.
  val semantics = CompiledSemantics(compiler, plugin, imports)
  val specBuilder = RowCreatorBuilder(semantics, producers)

  // There are many other factory methods for various input types: VFS, Strings, etc.
  specBuilder.fromFile(alohaJsonSpecFile)
}

val alohaRoot = new File(new File(".").getAbsolutePath.replaceFirst("/aloha/.*$", "/aloha/"))
val myAlohaJsonSpecFile =
  new File(alohaRoot, "aloha-cli/src/test/resources/com/eharmony/aloha/cli/dataset/proto_spec2.json")

// No cache dir in this example.
val alohaCacheDir: Option[File] = None

// Try[VwLabelRowCreator[UserProto]]
val creatorTry = getRowCreator(List(new VwLabelRowCreator.Producer[UserProto]),
                               myAlohaJsonSpecFile,
                               alohaCacheDir)

// Throws if the creator wasn't produced.  This might be desirable for
// short-circuiting in initialization code.
val creator = creatorTry.get

// Create a dataset row with an instance of UserProto.
// In real usage, get a real sequence.
val users: Seq[UserProto] = Nil

val results: Seq[(MissingAndErroneousFeatureInfo, CharSequence)] =
  users.map(u => creator(u))
```



### Dataset types

Aloha currently supports creating the following types of datasets:

vw, vw_labeled, vw_cb, libsvm, libsvm_labeled, csv
1. Unlabeled [VW](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format) datasets using the `--vw` flag
1. Labeled [VW](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format) datasets using the `--vw_labeled` flag
1. Contextual Bandit [VW](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format) datasets using the `--vw_cb` flag
1. Unlabeled [LIBSVM](http://www.quora.com/What-is-this-data-format-in-LIBSVM-training-dataset) using the `--libsvm` flag
1. Labeled [LIBSVM](http://www.quora.com/What-is-this-data-format-in-LIBSVM-training-dataset) using the `--libsvm_labeled` flag
1. [CSV](https://en.wikipedia.org/wiki/Comma-separated_values) datasets using the `--csv` flag

One can generate compatible dataset types simultaneously for the same dataset by including the dataset *type* flag 
along with the file to which the dataset should be output.  To output to standard out, use the filename `-`.  Note
that only one dataset can be output to a given file.  Outputting two or more datasets to the same output file has 
undefined behaviour.


## Create a VW Model

Given our dataset in `/tmp/dataset.vw`, we can create a VW model with the normal procedure.  For instance, to create 
a simple logistic regression model with all default parameters and one pass over the data, do: 

```bash
vw -d /tmp/dataset.vw --link logistic --loss_function logistic --readable_model /tmp/model_readable.vw -f /tmp/model.vw
```

This creates the binary model `/tmp/model.vw` and a human-readable model `/tmp/model_readable.vw`.

### Verifying the Model

```bash
vw -d /tmp/dataset.vw --loss_function logistic -i /tmp/model.vw -t 
```

<span class="label label-success">output</span>

<pre>
only testing
Num weight bits = 18
learning rate = 0.5
initial_t = 0
power_t = 0.5
using no cache
Reading datafile = /tmp/dataset.vw
num sources = 1
average  since         example        example  current  current  current
loss     last          counter         weight    label  predict features
0.310707 0.310707            1            1.0   1.0000   0.7329        6
0.296281 0.281855            2            2.0   1.0000   0.7544        6

finished run
number of examples per pass = 2
passes used = 1
weighted example sum = 2.000000
weighted label sum = 2.000000
average loss = 0.296281
best constant = 1.000000
best constant's loss = 0.313262
total feature number = 12
</pre>


## Creating an Aloha Model

To create an Aloha model, we need two things.  The first is the specification file used to create the dataset.  The 
second is the binary VW model.  To build the model, we just use the CLI again.

<span class="label">bash script</span>

```bash
aloha-cli/bin/aloha-cli                                       \
  -cp ''                                                      \
  --vw                                                        \
  --vw-args "--quiet -t"                                      \
  --spec $(find $PWD/aloha-cli/src -name 'proto_spec2.json')  \
  --model /tmp/model.vw                                       \
  --name "test-model"                                         \
  --id 101                                                    \
| tee /tmp/aloha-vw-model.json
```

This prints to *STDOUT* JSON similar to the following.  The following has rearranged key-value pairs and added 
whitespace.  Under the [JSON specification](http://json.org), this is equivalent to the JSON printed by the 
above command.

<span class="label label-success">/tmp/aloha-vw-model.json</span>

```json
{
  "modelType": "VwJNI",
  "modelId": { "id": 101, "name":"test-model" },
  "features": {
    "name":   { "spec": "ind(${name})",   "defVal": [["=UNK",1.0 },
    "gender": { "spec": "ind(${gender})", "defVal": [["=UNK",1.0]] },
    "bmi":    { "spec":"${bmi}",          "defVal":[["=UNK",1.0]]},
    "num_photos": "${photos}.size",
    "avg_photo_height": "{ val hs = ${photos.height};  hs.flatten.sum / hs.filter(_.nonEmpty).size }"
  },
  "namespaces": {
    "photos": [ "num_photos", "avg_photo_height" ]
  },
  "vw": {
    "model": "[ base64-encoded data ]",
    "params": "--quiet -t"
  }
}
```

## Aloha Model Prediction via CLI

To perform predictions using the command line interface, one needs to use the `--modelrunner` flag.  There are
many options for this.  The most import here are:
 
* `-A` which adds the input after the predictions.  Since no separator was provided, *TAB* is used to separate 
  the predictions and the input data.  It's easy to change the separator using the `--outsep` flag.  
* `--output-type` is another important flag which determines the output type of the model.  This is important to get
  right because types may be coerced and this could render weird results.  For instance, if using a model to predict
  probabilities and the output type is an integral type, the values returned will likely all be 0.  This is because
  coercion from real-valued to integrally valued numbers in done by dropping the decimal places.  If the value is in
  the interval [0, 1), then it will be truncated to 0.
* `--imports` adapts the DSL by importing JVM code.
* `-p` flag says that base64-encoded protocol buffers will be used as the input format.

<span class="label">bash script</span>

```bash
cat $(find $PWD/aloha-core/src -name 'fizz_buzzs.proto')               \
| aloha-cli/bin/aloha-cli                                              \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep):\
$(find $PWD/aloha-core -name "*.jar" | grep test)                      \
  --modelrunner                                                        \
  --output-type Double                                                 \
  -A                                                                   \
  --imports "scala.math._,com.eharmony.aloha.feature.BasicFunctions._" \
  -p com.eharmony.aloha.test.proto.Testing.UserProto                   \
  /tmp/aloha-vw-model.json
```

<span class="label label-success">output</span>

<pre>
0.732928454875946	CAESBEFsYW4YASUAALhBKg0IARABGQAAAAAAAPA/Kg0IAhACGQAAAAAAAABA
0.7543833255767822	CAESBEthdGUYAioNCAMQAxkAAAAAAAAIQA==
</pre>

If you don't want the input, just omit the `-A` flag or pipe and process the data elsewhere. 

### Sanity checking the Aloha model

Notice that the outputs here are:

* 0.732928454875946
* 0.7543833255767822

We saw in the section [Verifying the Model](#Verifying_the_Model), the that predictions were:

* 0.7329
* 0.7544

Which lines up.  *It appears our model is working!*

## Programatic Aloha Model Prediction

See the separate page [programatic Aloha model usage](prog_model_usage.html) for more details.

## Future plans

In the near future, we will start on integrating Aloha with [H<sub>2</sub>O](http://h2o.ai).

## Ways to extend to ML libraries not natively supported

One can think of this as being analogous to hadoop streaming.  Aloha can be integrated with other platforms
by using it for feature transformation and dataset production.  This is an easy path for the data scientist
as it can alleviate the burden on extracting and transforming features, especially when extract values from
Protocol buffers.
