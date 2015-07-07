# Constructing Datasets



## By Command Line Interface (CLI)

### Prerequisites

#### Get CLI Jar

<button>Download the CLI Jar from </button>

#### Copy the CLI script to some place your system path

### Getting Acquainted

#### Checking Out

```bash
git clone git@github.com:eHarmony/aloha.git
```

#### Building

Building takes a minute or so:

```bash
cd aloha
mvn clean package
```

Once everything is built, you should be able to run the examples that follow.

#### A Look at the CLI

```bash
aloha-cli/bin/aloha-cli
```
<pre>
usage: aloha-cli -cp /path/to/some.jar:/path/to/other.jar:... \[args to CLI\]
</pre>

So, it's clear that some jar files need to be specified on the classpath to make the CLI work.  Luckily, when 
aloha is built, the *aloha-cli* module has a jar that includes all of the necessary dependencies.  This can 
be found automatically with the following shell script magic which looks in the target directory of the 
*aloha-cli* module for a jar with dependencies.

Let's try running again with the proper jar on the classpath.

```bash
aloha-cli/bin/aloha-cli                                                        \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dependencies)
```

<pre>
No arguments supplied. Supply one of: '--dataset', '--modelrunner', '--vw'.
</pre>


Now the CLI gets a little further.  Let's choose the `--dataset` option (*since we're making a dataset*). 

```bash
aloha-cli/bin/aloha-cli                                                        \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dependencies)                 \
  --dataset
```

<pre>
Error: Missing option --spec
Error: No output dataset type provided.  Provide at least one of: vw, vw_labeled, vw_cb, libsvm, libsvm_labeled, csv
dataset \[ SOME ALOHA VERSION HERE \]
Usage: dataset \[options\]

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
</pre>


So, now we're getting somewhere.  Now that we have the lay of the land, let's create some small datasets for real.

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
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep) \
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
    { "name": "weight", "type": "int", "optional": true },
    { "name": "likes", "type": "string", "vectorized": true }
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

Let's look at the file structure and examine what happened.


<span class="label">Input</span>

```bash
# ================================================
# Create temporary input and output files
# ================================================
INFILE=$(mktemp -t example_input)
OUTFILE=$(mktemp -t example_output)

# ================================================
# File in file w/ base64-encoded UserProto data.
# ================================================
(
cat <<EOM
CAESBEFsYW4YASUAALhBKg0IARABGQAAAAAAAPA/Kg0IAhACGQAAAAAAAABA
CAESBEthdGUYAioNCAMQAxkAAAAAAAAIQA==
EOM
) >> $INFILE

# ================================================
#  Make a VW dataset.
#  Run CLI w/ an input file, output to a file.
# ================================================
aloha-cli/bin/aloha-cli                                \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep):\
$(find $PWD/aloha-core -name "*.jar" | grep test)      \
  --dataset                                            \
  -i $INFILE                                           \
  -s $(find $PWD/aloha-core/src -name 'proto_spec1.js')\
  -p com.eharmony.aloha.test.proto.Testing.UserProto   \
  --cachedir /tmp/aloha-cache                          \
  --vw $OUTFILE

# ================================================
#  Clean up temp files and print the output.
# ================================================
rm -f $INFILE
cat $OUTFILE
rm -f $OUTFILE
```

<span class="label label-success">Output</span>

<pre>
| name=Alan gender=MALE bmi:23 num_photos:2
| name=Kate gender=FEMALE bmi=UNK num_photos
</pre>


```bash
mkdir -p /tmp/aloha-cache 2>/dev/null
```

```bash
# Go to the root aloha directory. 
cd aloha

(
 echo "MALE,175,"
 echo "FEMALE,,books|films|chinese food"
) |\
aloha-cli/bin/aloha-cli                                               \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dependencies)        \
  --dataset                                                           \
  -s $(find $PWD/aloha-core/src/test/resources -name 'csv_spec2.js')  \
  -c $(find $PWD/aloha-core/src/test/resources -name 'csv_types1.js') \
  --cachedir /tmp/aloha-cache                                         \
  --csv -
```
