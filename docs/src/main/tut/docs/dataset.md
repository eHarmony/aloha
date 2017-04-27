---
layout: docs
title: Documentation
---

<!--
doxia-markdown-plugin syntax highlight support is broken.  Changing output to 
&lt;pre&gt;&lt;code&gt;...&lt;/code&gt;&lt;/pre&gt; fix all highlighting.  See:
  https://github.com/andriusvelykis/reflow-maven-skin/issues/11
  https://issues.apache.org/jira/browse/DOXIA-507
-->

# Constructing Datasets via CLI


## Prerequisites

**A quick note**: *All examples will assume you've check out Aloha from GitHub and built it from source.*  This is so
that you can reuse data in the testing code.  You can download and build via: 

```bash
git clone git@github.com:eHarmony/aloha.git
cd aloha

sbt +clean +publishLocal
sbt ++2.10.5 clean publishLocal
sbt ++2.11.8 clean publishLocal

sbt +clean +publishM2
sbt ++2.10.5 clean publishM2
sbt ++2.11.8 clean publishM2
```

### Get CLI Jar and *aloha-cli* script

In reality, you'll probably not want to download the source and build from scratch to use Aloha to create a dataset.
You can get a few different ways.  For instance: 

1. If you have the Aloha source code, just do an `mvn clean install` and look for the 
   `aloha-cli-[X.Y.Z]-jar-with-dependencies.jar` in 
   `$HOME/.m2/repository/com/eharmony/aloha-cli/[X.Y.Z]/aloha-cli-[X.Y.Z]-jar-with-dependencies.jar`
1. If you are using a project that has an Aloha Maven dependency, `aloha-cli-[X.Y.Z]-jar-with-dependencies.jar`
   may already be present at 
   `$HOME/.m2/repository/com/eharmony/aloha-cli/[X.Y.Z]/aloha-cli-[X.Y.Z]-jar-with-dependencies.jar`
1. If you are not currently using Aloha in a project but just want the CLI functionality, you can download the 
   `aloha-cli-[X.Y.Z]-jar-with-dependencies.jar` from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ccom.eharmony.aloha-cli).
   Just click [here](http://search.maven.org/#search%7Cga%7C1%7Ccom.eharmony.aloha-cli), then click the 
   `jar-with-dependencies.jar` link to download the latest Jar file with all of the dependencies included.

### Copy the CLI script to some place your system path

```bash
curl -fsSL https://raw.githubusercontent.com/eHarmony/aloha/master/aloha-cli/bin/aloha-cli
```

## Getting Acquainted

### A Look at the CLI

<span class="label">Input</span>

```bash
aloha-cli/bin/aloha-cli
```

<span class="label label-success">Output</span>

<pre><code>usage: aloha-cli -cp /path/to/some.jar:/path/to/other.jar:... [args to CLI]</code></pre>



So, it's clear that some jar files need to be specified on the classpath to make the CLI work.  Luckily, when 
aloha is built, the *aloha-cli* module has a jar that includes all of the necessary dependencies.  This can 
be found automatically with the following shell script magic which looks in the target directory of the 
*aloha-cli* module for a jar with dependencies.

Let's try running again with the proper jar on the classpath.

<span class="label">Input</span>

```bash
aloha-cli/bin/aloha-cli                                    \
  -cp $(find aloha-cli -name "*jar-with-dependencies.jar")
```

<span class="label label-success">Output</span>

<pre><code>No arguments supplied. Supply one of: '--dataset', '--modelrunner', '--vw'.</code></pre>

Now the CLI gets a little further.  Let's choose the `--dataset` option (*since we're making a dataset*). 

<span class="label">Input</span>

```bash
aloha-cli/bin/aloha-cli                                    \
  -cp $(find aloha-cli -name "*jar-with-dependencies.jar") \
  --dataset
```

<span class="label label-success">Output</span>

<pre><code>Error: Missing option --spec
Error: No output dataset type provided.  Provide at least one of: vw, vw_labeled, vw_cb, libsvm, libsvm_labeled, csv
dataset [ SOME ALOHA VERSION HERE ]
Usage: dataset [options]

  --cachedir &lt;value&gt;
        a cache directory
  --parallel &lt;value&gt;
        a list of Apache VFS URLs additional jars to be included on the classpath
  -s &lt;value&gt; | --spec &lt;value&gt;
        Apache VFS URL to a JSON specification file containing attributes of the dataset being created.
  -p &lt;value&gt; | --proto-input &lt;value&gt;
        canonical class name of the protocol buffer type to use.
  -c &lt;value&gt; | --csv-input &lt;value&gt;
        Apache VFS URL to JSON file specifying the structure of the CSV input.
  -i &lt;value&gt; | --in &lt;value&gt;
        Apache VFS URL to the input file.  If not supplied, STDIN will be used.
  --vw &lt;value&gt;
        produce an unlabeled VW dataset and place the output in the specified location.
  --vw_labeled &lt;value&gt;
        produce a labeled VW dataset and place the output in the specified location.
  --vw_cb &lt;value&gt;
        produce a contextual bandit VW dataset and place the output in the specified location.
  --libsvm &lt;value&gt;
        produce an unlabeled LIBSVM dataset and place the output in the specified location.
  --libsvm_labeled &lt;value&gt;
        produce a labeled LIBSVM dataset and place the output in the specified location.
  --csv &lt;value&gt;
        produce a CSV dataset and place the output in the specified location.
  --csv-headers
        Produce headers in CSV output.
  --csv-header-file &lt;value&gt;
        Write CSV headers to the designated file.
</code></pre>


So, now we're getting somewhere.  Now that we have the lay of the land, let's create some small datasets for real.


## Examples

### CSV Input, Transformed CSV output

---

#### Command and Results

<span class="label">Input</span>

```bash
# Creating an Aloha cache directory provides a speed up 
# when running multiple times.  
#
# The cache directory must exist if it's specified.
# 
mkdir -p /tmp/aloha-cache 2>/dev/null
```

```bash
# Read 2 rows of data from STDIN.
(
cat <<EOM
MALE,175,
FEMALE,,books|films|chinese food
EOM
) |\
aloha-cli/bin/aloha-cli                               \
  -cp $(find aloha-cli -name "*.jar" | grep dep)      \
  --dataset                                           \
  --cachedir /tmp/aloha-cache                         \
  -c $(find $PWD/aloha-core/src -name 'csv_types1.js')\
  -s $(find $PWD/aloha-core/src -name 'csv_spec2.js') \
  --csv -
```

<span class="label label-success">Output</span>

<pre>
MALE,170,0
FEMALE,NULL,3
</pre>

#### Files

<span class="label label-info text-right">csv_types1.js</span>
  
```json

{
  "fs": ",",
  "ifs": "|",
  "missingData": "NULL",
  "errorOnOptMissingField": false,
  "errorOnOptMissingEnum": false,
  "columns": [
    { "name": "gender", "type": "enum",
      "className": "a.b.Gender", 
      "values": [ "MALE", "FEMALE" ] },
    { "name": "weight", "type": "int", 
      "optional": true },
    { "name": "likes", "type": "string", 
      "vectorized": true }
  ]
}
```

<span class="label label-info text-right">csv_spec2.js</span>

```json

{
  "separator": ",",
  "nullValue": "NULL",
  "encoding": "regular",
  "imports":[],
  "features": [
    {"name": "gender", "spec":"${gender}" },
    {"name": "weight", "spec":"${weight} / 10 * 10" },
    {"name": "num_likes", "spec":"${likes}.size" }
  ]
}
```

---

Let's look at the CLI arguments and the file structure of the auxiliary we provied to examine what happened.  We 
supplied: 
   
* `--cachedir /tmp/aloha-cache`: This is useful for avoiding recompilation of Aloha features across calls to the 
  CLI.  Try to specify a cache directory when possible.  The directory must exist.
* `-c $(find $PWD/aloha-core/src -name 'csv_types1.js')` describes the structure of the CSV **INPUT** data 
  used to construct the dataset.  This isn't necessary when using 
  [Protocol Buffer](https://developers.google.com/protocol-buffers/?hl=en) input (`-p` flag).
* `-s $(find $PWD/aloha-core/src -name 'csv_spec2.js')` describes the **OUTPUT** format of the dataset.
* `--csv -` This option says that we want to create a *CSV* dataset.  The `-` at the end of the option indicates that
  the output be directed to standard output (*STDOUT*).

#### External CSV input format description

`csv_types1.js` is a JSON file describing the CSV input structure of the original data we are transforming.  These 
files have 6 fields.
 
* `fs`: The associated value is a string describing the *field separator*.  For instance, with comma-delimited data, 
  the value would be `","`; for tab-separated data, it would be `"\t"`.  
* `ifs`: The *intra-field separator*.  This is used when fields are `vectorized` to separate the values in a 
  vectorized data within a column.
* `missingData`: This is the string which represents a missing value.  For instance, `NULL` was used in the above 
  example.  If we set the second line in the input in the above example to `FEMALE,NULL,books|films|chinese food`, 
  everything still works.
* `errorOnOptMissingField`: a Boolean telling whether to produce an error for the row when an optional field 
  is missing.  Currently, an error in an output line causes the line not to included in the resulting dataset, 
  but the dataset creation process is not halted.  `false` is recommended since it's more forgiving; it allows 
  rows with a missing value to be included in the dataset.        
* `errorOnOptMissingEnum`: Similar to `errorOnOptMissingField`, this field tells whether to produce an error when an
  unknown value for an enumerated type is provided in the input data.
* `columns`: An array of column definitions.  The order in which the columns appear in the array defined the expected
  column order in the CSV input.

In the `columns` field, each associated value in the array is a JSON object.  
  
* `name`: This is the name of the field.  This is important not just for documentation but for use in the output 
  specification as well.
* `type`: one of { `boolean`, `double`, `enum`, `float`, `int`, `long`, `string` }
* `optional`: a Boolean value. `true` if the column's value might *NOT* appear in the input data.  The default when 
  not provided is `false`.   
* `vectorized`: a Boolean value. `true` if the column's contains *zero or more* values.  In case this is set to 
  `true`, the `ifs` value is used to split the values in the column into a vector of values.  When `vectorized` is not 
  provided, its default value is `false`.

### Protocol Buffer input, Vowpal Wabbit output

This example will show off a bunch of features not covered in the
[CSV Input, Transformed CSV output](#CSV_Input_Transformed_CSV_output) example.  We'll see how to used Protocol 
Buffer input data and output to a different output type, namely Vowpal Wabbit.  We'll also see how to specify an 
input and output file, rather relying on STDIN and STDOUT.

Let's start by looking at the fields in the data that we'll be working with in this example.  To do so, just cat the 
User.proto file: 

<span class="label">Input</span>

```bash
cat aloha-core/src/test/proto/User.proto
```

<span class="label label-success">Output</span>

<pre>
package com.eharmony.aloha.test.proto;

option java_outer_classname="Testing";

enum GenderProto {
    MALE   = 1;
    FEMALE = 2;
}

message PhotoProto {
    required int64  id = 1;
    optional int32 height = 2;
    optional double aspect_ratio = 3;
}

message UserProto {
    required int64 id = 1;
    optional string name = 2;
    optional GenderProto gender = 3;
    optional float bmi = 4;
    repeated PhotoProto photos = 5;
}
</pre>


Now let's create some data and then process it.

<span class="label">Input</span>

Create temporary input and output files:

```bash
INFILE=$(mktemp -t example_input)
OUTFILE=$(mktemp -t example_output)
```

Fill the input file with base64-encoded UserProto data.  This is the data that will be provided to the data 
scientist for analysis.  Aloha is used to decode and transform with serialized data.

```bash
(
cat <<EOM
CAESBEFsYW4YASUAALhBKg0IARABGQAAAAAAAPA/Kg0IAhACGQAAAAAAAABA
CAESBEthdGUYAioNCAMQAxkAAAAAAAAIQA==
EOM
) >> $INFILE
```

Run the CLI.  Note we need the second entry in the `-cp` (classpath) flag because it is the jar that contains 
the protocol buffer definitions.

```bash
aloha-cli/bin/aloha-cli                              \
  -cp $(find aloha-cli -name "*.jar" | grep dep):\
$(find aloha-core -name "*.jar" | grep test)         \
  --dataset                                          \
  -i $INFILE                                         \
  -s $(find $PWD/aloha-core/src -name 'proto_spec1.js')   \
  -p com.eharmony.aloha.test.proto.Testing.UserProto \
  --cachedir /tmp/aloha-cache                        \
  --vw $OUTFILE
```

Clean up temp files and print the output.

```bash 
rm -f $INFILE
cat $OUTFILE
rm -f $OUTFILE
```

<span class="label label-success">Output</span>

<pre>
| name=Alan gender=MALE bmi:23 num_photos:2
| name=Kate gender=FEMALE bmi=UNK num_photos
</pre>

Again, let's look at the CLI arguments.  We'll only describe the parameters that differ from the CSV input example:

* `-i $INFILE` This is an input file containing the data.
* `-p com.eharmony.aloha.test.proto.Testing.UserProto` Tells the CLI that the input will be protocol buffer 
  data that will have the structure described by the `com.eharmony.aloha.test.proto.Testing.UserProto` class. 
* `--vw $OUTFILE` output an unlabeled vowpal wabbit dataset and put it in the file pointed to by the $OUTFILE shell 
  variable.  Obviously, there's no need for a variable here because we are just executing a shell command.  The key
  is that the value containing the output file location is an Apache VFS URL.
  
## (Output) specification files

All CLI-based dataset creation involves a specification file for the output type.  In the 
[CSV example](#CSV_Input_Transformed_CSV_output), we used 
[csv_spec2.js](https://github.com/eHarmony/aloha/blob/master/aloha-core/src/test/resources/com/eharmony/aloha/dataset/cli/csv_spec2.js).
In the [Protocol Buffer example](#Protocol_Buffer_input_Vowpal_Wabbit_output), we used
[proto_spec1.js](https://github.com/eHarmony/aloha/blob/master/aloha-core/src/test/resources/com/eharmony/aloha/dataset/cli/proto_spec1.js).
Note that these files have the same `features` array format, but some of the other fields vary.  This is because there
are innate differences in the *output type* of columns in each row emitted by the dataset creator.  For instance, each 
column of a CSV dataset is a scalar value.  This means the vector of columns is a dense vector format.  In the case of 
vowpal wabbit output, the covariate data is inherently sparse in nature so key-value pairs are outputted.

### Common fields

* `imports` an array of strings.  These imports that will be imported into scope for eeach feature definition. 
  Wildcard imports are specified with an underscore.  For instance, a common import one should consider is 
  `"com.eharmony.aloha.feature.BasicFunctions._"`
* `features` an array containing JSON objects which describe the features to be produced.  More on this later.

### CSV-specific fields

* `separator` the column delimiter. `","` for comma-delimited data, `"\t"` for tab-delimited.
* `nullValue` the string value to assign to a column when its data is missing. `""` or `"NULL"`, for instance.  
* `encoding` how categorical variables are encoded.  Currently, this can be `regular` or `hotOne`.  Regular encoding
  just creates a one-dimensional string representation of the value.
  [Hot-one encoding](https://en.wikipedia.org/wiki/One-hot) is a binarized vector representation of the feature.

### features 

Each feature has three values:
* `name` the name of the feature
* `spec` how to compute the feature (scala code with the imports in the `imports` array in scope) 
* `defVal` a default value.  This field is optional.  If omitted, the `nullValue` value will be used if necessary in 
  the CSV case and an empty sequence of key-value pairs will be used in the case of sparse formats like VW and LIBSVM.

### A word of sparse formats

Each feature in a sparse dataset type outputs a sequence of key-value pairs.  This can be *zero or more*.  If the 
sequence sizes for each feature are exactly *zero* or *one*, the same specification file can be used for both dense 
(CSV) or sparse (VW, LIBSVM) dataset creation.  By importing `com.eharmony.aloha.feature.BasicFunctions._`, a 
conversion mechanism is brought into scope.  So when specifying a feature like: 

```json
{ 
  "name": "some_feature_name", 
  "spec": "${some.extracted.scalar.number}"
}
```

and assuming a datum passed into the dataset CLI has a value of `7` associated with the field 
`some.extracted.scalar.number`, Aloha can automatically generate a sequence of one key-value pair: 
`[ "some_feature_name" -> 7) ]`.


### Multiple dataset formats

Additionally, the dataset creator ignores JSON fields in the specification file that aren't used by the desired dataset
type.  This means that if we provide a union of fields used by multiple dataset creators, we can those to generate two
or more datasets of different types at once.  We just need to make sure the destination files differ.  For instance:


Create the files again (like above):

```bash
INFILE=$(mktemp -t example_input)
OUTFILE_CSV=$(mktemp -t example_output)
OUTFILE_VW=$(mktemp -t example_output)
```

Create the protocol buffer input again (like above):


```bash
(
cat <<EOM
CAESBEFsYW4YASUAALhBKg0IARABGQAAAAAAAPA/Kg0IAhACGQAAAAAAAABA
CAESBEthdGUYAioNCAMQAxkAAAAAAAAIQA==
EOM
) >> $INFILE
```

Run the CLI.  Here we create two datasets simultaneously from the same protocol buffer input data and the same 
specification file,
[csv_AND_vw_spec.js](https://github.com/eHarmony/aloha/blob/master/aloha-core/src/test/resources/com/eharmony/aloha/dataset/cli/csv_AND_vw_spec.js).
We give headers to the CSV file.  Everything else is the same.

```bash
aloha-cli/bin/aloha-cli                                     \
  -cp $(find aloha-cli -name "*.jar" | grep dep):\
$(find aloha-core -name "*.jar" | grep test)                \
  --dataset                                                 \
  -i $INFILE                                                \
  -s $(find $PWD/aloha-core/src -name 'csv_AND_vw_spec.js') \
  -p com.eharmony.aloha.test.proto.Testing.UserProto        \
  --cachedir /tmp/aloha-cache                               \
  --vw_labeled  $OUTFILE_VW                                 \
  --csv $OUTFILE_CSV                                        \
  --csv-headers
```

```bash
cat $OUTFILE_CSV
```

<span class="label label-success">Output</span>

<pre>
csv_label,gender,bmi,num_photos,avg_photo_height
1,0,23.0,2,1
1,1,NULL,1,3
</pre>

```bash
cat $OUTFILE_VW
```

<span class="label label-success">Output</span>

<pre>
1 1|ignored csv_label |personal bmi:23 |photos num_photos:2 avg_photo_height
1 1|ignored csv_label |personal gender |photos num_photos avg_photo_height:3
</pre>

