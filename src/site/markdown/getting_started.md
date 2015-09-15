# Getting Started

## Build Prerequisites

Aloha uses [Apache Maven](http://maven.apache.org) for building.


### Installing Maven on a *Mac*

The preferred way to install Maven on a *Mac* is by using the [Homebrew](http://http://brew.sh) package manager.  If 
you have Homebrew installed, just install via:

```bash
brew install maven
```


### Installing Homebrew on a *Mac*

If you don't have Homebrew installed, follow the instructions at [http://brew.sh](http://brew.sh).


### Installing Maven Manually

[Download it here](http://maven.apache.org/download.cgi).  Aloha has be tested on *2.2.1* and *3.x.x*.



### Optionally Install and Run Zinc

While not necessary, [Zinc](https://www.typesafe.com/blog/zinc-and-incremental-compilation) with Maven 3 can provide 
speed ups when building Aloha.  Zinc is an incremental [Scala](http://scala-lang.org) compiler produced by 
[Typesafe](https://www.typesafe.com), the company providing commercial support for Scala.  


#### On a *Mac* via Homebrew

```bash
brew install zinc
```

Or to manually install go to Zinc's [github page](https://github.com/typesafehub/zinc) and look for 
the ** *latest stable version* ** link.


#### Starting Zinc
 
```bash
zinc -start -nailed
```


## Build Aloha from Source

```bash
git clone git@github.com:eHarmony/aloha.git
cd aloha
mvn clean install
```

If you have Zinc installed and running, and are using Maven 3, change:

```bash
mvn clean install
```

*to* 

```bash
mvn -Dcompile=incremental clean install
```

## Create a VW Dataset 

*[eHarmony](http://www.eharmony.com)* uses [Vowpal Wabbit](https://github.com/JohnLangford/vowpal_wabbit/wiki) for many
of its predictive tasks so Aloha provides support for VW from creating datasets, to creating native VW models on the 
[JVM](https://en.wikipedia.org/wiki/Java_virtual_machine).

<span class="label">bash script</span>

```bash
# Note that the first jar is to bring in the command line tools.  The second jar is for the 
# protocol buffer generated classes.
#
aloha-cli/bin/aloha-cli                                  \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep):\
$(find $PWD/aloha-core -name "*.jar" | grep test)        \
  --dataset                                              \
  -s $(find $PWD/aloha-core/src -name 'proto_spec2.json')\
  -p com.eharmony.aloha.test.proto.Testing.UserProto     \
  -i $(find $PWD/aloha-core/src -name 'fizz_buzzs.proto')\
  --vw_labeled /tmp/dataset.vw
```

<span class="label label-success">/tmp/dataset.vw</span>

<pre>
1 1| name=Alan gender=MALE bmi:23 |photos num_photos:2 avg_photo_height
1 1| name=Kate gender=FEMALE bmi=UNK |photos num_photos avg_photo_height:3
</pre>

### Create a VW dataset programmatically

```scala
// Scala code

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

def getRowCreator[T <: GeneratedMessage : RefInfo, S <: RowCreator[T]](
    producers: List[RowCreatorProducer[T, S]], 
    alohaJsonSpecFile: File,
    alohaCacheDir: Option[File] = None): Try[S] = {
  val plugin = CompiledSemanticsProtoPlugin[T]
  val compiler = TwitterEvalCompiler(classCacheDir = alohaCacheDir)
  val semantics = CompiledSemantics(compiler, plugin, Nil)
  val specBuilder = RowCreatorBuilder(semantics, producers)
  specBuilder.fromFile(alohaJsonSpecFile)
}

val myAlohaJsonSpecFile: File = ... 
val alohaCacheDir: File = ... 

val creatorTry: Try[VwLabelRowCreator[MyProto]] = 
  getRowCreator[MyProto, VwLabelRowCreator[MyProto]](
    List(new VwLabelRowCreator.Producer[MyProto]), 
    myAlohaJsonSpecFile,
    Option(alohaCacheDir))

// Throws if the creator wasn't produced.
val creator = creatorTry.get


// Creating a row of a dataset with an instance of MyProto.
val myProto: MyProto = ...

// missingAndErrors: com.eharmony.aloha.dataset.MissingAndErroneousFeatureInfo(missingFeatures
// output: CharSequence
val (missingAndErrors, output) = creator(myProto)
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
cat /tmp/dataset.vw                          \
| vw --link logistic                         \
     --loss_function logistic                \
     --readable_model /tmp/model_readable.vw \
     -f /tmp/model.vw
```

This creates binary model `/tmp/model.vw` and a human-readable model `/tmp/model_readable.vw`.

### Verifying the Model

```bash
cat /tmp/dataset.vw | vw --loss_function logistic -i /tmp/model.vw -t 
```

<span class="label label-success">output</span>

<pre>
only testing
Num weight bits = 18
learning rate = 10
initial_t = 1
power_t = 0.5
using no cache
Reading datafile = 
num sources = 1
average    since         example     example  current  current  current
loss       last          counter      weight    label  predict features
0.310707   0.310707          1      1.0     1.0000   0.7329        6
0.296281   0.281855          2      2.0     1.0000   0.7544        6

finished run
number of examples per pass = 2
passes used = 1
weighted example sum = 2
weighted label sum = 2
average loss = 0.296281
best constant = 1
best constant's loss = 0.313262
total feature number = 12
</pre>


## Creating an Aloha Model

To create an Aloha model, we need two things.  The first is the specification file used to create the dataset.  The 
second is the binary VW model.  To build the model, we just use the CLI again.

<span class="label">bash script</span>

```bash
aloha-cli/bin/aloha-cli                                       \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep)         \
  --vw                                                        \
  --vw-args "--quiet -t"                                      \
  --spec $(find $PWD/aloha-core/src -name 'proto_spec2.json') \
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
    "name":       { "spec": "ind(${name})",   "defVal": [["=UNK",1.0]] },
    "gender":     { "spec": "ind(${gender})", "defVal": [["=UNK",1.0]] },
    "bmi":        { "spec": "${bmi}",         "defVal": [["=UNK",1.0]] },
    "num_photos": "${photos}.size",
    "avg_photo_height": "{ val hs = ${photos.height};  hs.flatten.sum / hs.filter(_.nonEmpty).size }"
  },
  "namespaces": {
    "photos": [ "num_photos", "avg_photo_height" ]
  },
  "vw": {
    "model":"[ base64-encoded data ]",
    "params":"--quiet -t",
    "creationDate":1441752810543
  }
}
```

## Aloha Model Prediction via CLI

To preform predictions using the command line interface, one needs to use the `--modelrunner` flag.  There are
many options for this.  The most import here are:
 
* `-A` which adds the input after the predictions.  Since no separator was provided, *TAB* is used to separate 
  the predictions and the input data.  It's easy to change the separator using the `--outsep` flag.  
* `--output-type` is another import flag which determines the output type of the model.  This is important to get
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
