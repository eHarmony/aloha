<div class="jumbotron">
  <div class="container">
    <h1>Aloha</h1>
    <p>A scala-based feature generation and modeling framework</p>
    <a href="getting_started.html" class="btn btn-primary btn-large" role="button">Get Started &#187;</a>
  </div>
</div>

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
- ***<span class="color-highlight">Aloha</span>*** &#8611; \[ ***Your KV Store's Type*** \]

In Aloha, models are written generically, and different 
[semantics](http://en.wikipedia.org/wiki/Formal_semantics_of_programming_languages) implementations are provided to 
give meaning to the features extracted from the arbitrary input types on which the models operate.


Getting Started
---------------

See the [Getting Started Guide](getting_started.html).

<a href="eg/dataset/index.html" class="btn btn-primary btn-large btn-block">Learn to Create a Dataset</a>

<a href="model_formats.html" class="btn btn-primary btn-large btn-block">Learn to Create a Model</a>


Maven Setup
-----------

<!--
<dependency>
  <groupId>com.eharmony</groupId>
  <artifactId>aloha-core</artifactId>
  <version>2.0.0</version>
</dependency>
-->

<div class="source"> 
 <div class="source"> 
  <pre class="hljs xml"><span class="hljs-tag">&lt;<span class="hljs-title">dependency</span>&gt;</span>
  <span class="hljs-tag">&lt;<span class="hljs-title">groupId</span>&gt;</span>com.eharmony<span class="hljs-tag">&lt;/<span class="hljs-title">groupId</span>&gt;</span>
  <span class="hljs-tag">&lt;<span class="hljs-title">artifactId</span>&gt;</span>aloha-core<span class="hljs-tag">&lt;/<span class="hljs-title">artifactId</span>&gt;</span>
  <span class="hljs-tag">&lt;<span class="hljs-title">version</span>&gt;</span><span id="version"></span><span class="hljs-tag">&lt;/<span class="hljs-title">version</span>&gt;</span>
<span class="hljs-tag">&lt;/<span class="hljs-title">dependency</span>&gt;</span>
</pre> 
 </div> 
</div>
