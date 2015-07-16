#Aloha#

[![Build Status](https://travis-ci.org/eHarmony/aloha.svg?branch=master)](https://travis-ci.org/eHarmony/aloha)

The Aloha libraries provide implementations of machine learning models used at eHarmony.  The distinction between Aloha and other machine learning libraries such as [Weka](http://www.cs.waikato.ac.nz/ml/weka/), [Torch](http://www.torch.ch/), [Mahout](http://mahout.apache.org/), etc, is that Aloha models:

1. do not have predefined input and output types
2. can be parsed by a single factory
3. have external specifications (JSON)

So, Aloha models are are not written in terms of [Instance](http://weka.sourceforge.net/doc.dev/weka/core/Instance.html)s, [Tensor](https://github.com/torch/torch7/blob/master/doc/tensor.md)s, or [DataModel](https://builds.apache.org/job/Mahout-Quality/javadoc/org/apache/mahout/cf/taste/model/DataModel.html)s.  Instead, models are written generically, and different [semantics](http://en.wikipedia.org/wiki/Formal_semantics_of_programming_languages) implementations are provided to give meaning to the features extracted from the arbitrary input types on which the models operate.


While these differences may not sound extremely useful, together they produce a number of advantages.  The most notable is probably the way input features make their way to the models.  Typically, when interacting with APIs, data is translated into a format that can be understood by the objects being called.  By tying a model interface to an input type specified inside the library, we require the caller to convert the data to the input type before the model can use the data to make a prediction.  There are some ways to ease the woes that are involved in the [ETL](http://en.wikipedia.org/wiki/Extract,_transform,_load) process, but as we've seen many times, transforming data can be slow, error-prone, and ultimately, unnecessary altogether.  It's almost always the case that data is in an alternate format than the one required for learning or prediction.  Because data, in its natural form, typically has a [graph](http://en.wikipedia.org/wiki/Graph_\(mathematics\))-like structure and many machine learning algorithms operate on [vector spaces](http://en.wikipedia.org/wiki/Vector_space), we often have to perform such a transformation.  The question is who should do the data transformation.


Rather than requiring the calling code to transform data, Aloha takes the stance that it's better for the models to transform data in a lazy way when needed.  From the [Lazy Load](http://www.martinfowler.com/eaaCatalog/lazyLoad.html) page of [Martin Fowler](http://en.wikipedia.org/wiki/Martin_Fowler)'s [Catalog of Patterns of Enterprise Application Architecture](http://www.martinfowler.com/eaaCatalog/index.html), we know that

> \[*it*\] makes loading easier on the developer using the object, who otherwise has to load all the objects he needs explicitly.

and

> if you're lazy about doing things you'll win when it turns out you don't need to do them at all.

Aloha considers both of these properties very important.  The calling code should be as simple as possible and *SHOULD NOT* have to do any unnecessary work.  The problem usually is that the calling code doesn't know what is unnecessary.  This could be ameliorated in the traditional setting by adding a *what-features-the-model-needs* method to the API but this too can be a problem in situations when the model has different needs conditional on the data passed to the model.  This is especially true with decision trees.  Many times, the branches require different features.  To provide all features ahead of time is often wasteful.

Since models are specified in JSON and can be parsed by a single factory, we can pass around model definitions as messages and expose parsers as service interfaces.  This means that we can train models in mini batches and atomically hot swap out models in prod without server restarts, if desired.

##So, how does it all work?##


## Models ##

### Regression Model ###

#### Regression Model JSON Sections ####

##### Features #####

###### Defaults ######

There are two kinds of defaults:

* Variable-Level Defaults
* Feature-Level Defaults

*Variable-Level Defaults* occur inside variable specifications and have the same syntax as one might find in bash.  See the [Advanced Bash-Scripting Guide, Chapter 10](http://tldp.org/LDP/abs/html/parameter-substitution.html) for syntax on
variable substitution and defaulting.

As an example instance in Aloha, take

    "${user.profile.height:-160}",

This seemingly tries to extract the user height (in centimeters) and if the value does not exist, it will insert the value of 160 (*NOT -160*).  Note that while not specified, default type matters.  Hopefully, it should be obvious that the default should have the same type as the variable.  To specify different literal types, the normal java rules apply:

* For 32-bit integer (i.e. int), just specify the integer literal `"${x:-1}"`
* For 64-bit integer (i.e. long), append an L `"${x:-1L}"`
* For IEEE 754 32-bit floats, append an f.  `"${x:-1f}"`
* For IEEE 754 64-bit floats (i.e. double), specify at least a single decimal place with no suffix `"${x:-1.0}"`
* For 1-byte integer, specify like: `"${x:-1.toByte}"`
* For 2-byte integer (i.e. short), specify like: `"${x:-1.toShort}"`
* For Boolean values, specify using *true* or *false*, all lowercase: `"${x:-false}"`
* For Strings, specify the String literal.  Be sure to properly escape the string: `"${x:-\"[default string]\"}"`


*Feature-Level Defaults* specification syntax varies by model because it is tied to the [codomain](http://en.wikipedia.org/wiki/Codomain) of the functions that the semantics produces for, and injects into, the model.  For instance, in the regression model, the semantics produces functions that have a codomain of unordered sequences of key-value pairs.  Therefore, the feature-level default default of regression models is the same type and is specified as the value associated with the "*default*" key:

    {
        "spec": "sos2U(log2(${pairing.distance}), 0, 10, 1)",
        "default": [["=UNK", 1]]
    }

The `[["=UNK", 1]]` represent a sequence of one key-value pair whose key is *=UNK* and whose value is 1.

