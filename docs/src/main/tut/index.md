---
layout: home
title:  "Home"
section: "home"
---


Motivation
----------

Data science is filled with many mundane tasks that take up a majority of the data scientist's time.  Much of this
revolves around formatting and transforming data to a form more amenable to learning or inference.  Aloha attempts
to alleviate this burden by providing a few things:

- a DSL for feature specification based on familiar syntax
- generic models that make use of this DSL
- a pipeline for dataset generation using the same DSL


How does Aloha help?
--------------------

Oftentimes machine learning libraries and models employ linear-algebraic data structure as their input type.  For instance:

- [Weka](http://www.cs.waikato.ac.nz/ml/weka/) &#8611; [Instance](http://weka.sourceforge.net/doc.dev/weka/core/Instance.html)
- [Torch](http://www.torch.ch/) &#8611; [Tensor](https://github.com/torch/torch7/blob/master/doc/tensor.md)
- [Mahout](http://mahout.apache.org/) &#8611; [DataModel](https://builds.apache.org/job/Mahout-Quality/javadoc/org/apache/mahout/cf/taste/model/DataModel.html)
- ***Aloha*** &#8611; \[ ***Your KV Store's Type*** \]

In Aloha, models are written generically, and different
[semantics](http://en.wikipedia.org/wiki/Formal_semantics_of_programming_languages) implementations are provided to
give meaning to the features extracted from the arbitrary input types on which the models operate.


Maven Setup
-----------



```xml
<dependency>
  <groupId>com.eharmony</groupId>
  <artifactId>aloha-core_2.11</artifactId>
  <version>4.1.3</version>
</dependency>
```

