
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
speed ups when building Aloha.  Zinc is an [Scala](http://scala-lang.org) incremental compiler produced by 
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
aloha-cli/bin/aloha-cli                                  \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep):\
$(find $PWD/aloha-core -name "*.jar" | grep test)        \
  --dataset                                              \
  -s $(find $PWD/aloha-core/src -name 'proto_spec2.js')  \
  -p com.eharmony.aloha.test.proto.Testing.UserProto     \
  -i $(find $PWD/aloha-core/src -name 'fizz_buzzs.proto')\
  --vw_labeled /tmp/dataset.vw
```

<span class="label label-success">/tmp/dataset.vw</span>

<pre>
1 1| name=Alan gender=MALE bmi:23 |photos num_photos:2 avg_photo_height
1 1| name=Kate gender=FEMALE bmi=UNK |photos num_photos avg_photo_height:3
</pre>

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

```bash
aloha-cli/bin/aloha-cli                                       \
  -cp $(find $PWD/aloha-cli -name "*.jar" | grep dep)         \
  --vw                                                        \
  --spec $(find $PWD/aloha-core/src -name 'proto_spec2.json') \
  --model file://tmp/model.vw                                 \
  --name "test-model"                                         \
  --id 101
```

This prints to *STDOUT* JSON similar to the following.  The following has rearranged key-value pairs and added 
whitespace.  Under the [JSON specification](http://json.org), this is equivalent to the JSON printed by the 
above command.

<span class="label label-success">output</span>

```json
{
  "modelType":"VwJNI",
  "modelId": { "id": 101, "name": "test-model" },
  "namespaces": {
    "photos": [ "num_photos", "avg_photo_height" ] 
  },
  "features": {
    "name":             { "spec": "ind(${name})",   "defVal": [ ["=UNK",1.0] ] },
    "gender":           { "spec": "ind(${gender})", "defVal": [ ["=UNK",1.0] ] },
    "bmi":              { "spec": "${bmi}",         "defVal": [ ["=UNK",1.0] ] },
    "num_photos":       "${photos}.size",
    "avg_photo_height": "(${photos.height}.flatten : Seq[Int]).sum / ${photos}.size"
  },
  "vw":{
    "model": "BwAAADcuMTAuMABtAABIwgAASEIS [More base64-encoded data ...]"
  }
}
```
