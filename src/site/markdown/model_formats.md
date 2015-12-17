# Model Formats

## Prerequisites

### Common Required JSON Fields

* `modelType` is a string defining the type of model the JSON in the same object block represents.  These values 
  are predefined within the model definitions and won't change without a major version change since Aloha uses 
  [semantic versioning](http://semver.org).  See a model description below to determine the associated `modelType`
  field.
* `modelId` is a JSON object that acts as an identifier for models.  It is represented by an object with two fields 
  `id`, a 64-bit integer and `name`, a string.  

### Models Should be Immutable
  
> **NOTE**: While Aloha doesn't place any special importance of the uniqueness of these model IDs and doesn't 
> assign any special semantic meaning to these fields, *Aloha users* ***SHOULD*** *place special importance 
> on the uniqueness of the numeric `id`.*  

This is just a suggestion but it will pay huge dividends in terms of cleaner data collection:

> Models should for practical purposes be immutable.  This means that whenever a model is edited in such a way that 
> the model functionally changes, the model's `modelId`'s `id` field should be changed **And all ancestor models' 
> `modelId`'s `id` field should change.**


## Model Types

* [Categorical distribution model](#Categorical_distribution_model): 
  returns pseudo-random values based on a designated
  [Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution) 
* [Constant model](#Constant_model): returns a constant value
* [Decision tree model](#Decision_tree_model): a standard [decision tree](https://en.wikipedia.org/wiki/Decision_tree)
* [Double-to-Long model](#Double-to-Long_model): provides an 
  [affine transformation](https://en.wikipedia.org/wiki/Affine_transformation) from Double values to Long values
* [Error model](#Error_model): a model for returning errors rather than returning a score or throwing an exception. 
* [Error-swallowing model](#Error-swallowing_model): swallows all errors (exceptions) and returns a *no-score* if an
  error is encountered.
* [Model decision tree model](#Model_decision_tree_model): a [decision tree](https://en.wikipedia.org/wiki/Decision_tree)
  whose nodes are models that when reached are applied to the input data.
* [Regression model](#Regression_model): a [sparse](https://en.wikipedia.org/wiki/Sparse_matrix) 
  [polynomial regression](https://en.wikipedia.org/wiki/Polynomial_regression) model 
* [Segmentation model](#Segmentation_model): a vanilla [segmentation](https://en.wikipedia.org/wiki/Market_segmentation) 
  model based on linear search in an [interval](https://en.wikipedia.org/wiki/Interval_\(mathematics\)) space
* [Vowpal Wabbit model](#Vowpal_Wabbit_model): a model exposing [VW](https://github.com/JohnLangford/vowpal_wabbit/wiki) 
  through its [JNI](https://github.com/JohnLangford/vowpal_wabbit/tree/master/java) wrapper
* [H<sub>2</sub>O model](#H2O_model): a model exposing the suite of [H<sub>2</sub>O](https://h2o.ai) models


## Categorical distribution model

A Categorical distribution model returns pseudo-random values based on a designated
[Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution).  It can be used for collecting 
random data on which future models can be trained.  Its sampling is constant time, regardless of the number of 
values in the distribution.


### (CD) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aCD_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aCD_features">features</a></td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aCD_probabilities">probabilities</a></td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aCD_labels">labels</a></td>
    <td>Array</td>
    <td><b>OUTPUT</b></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aCD_missing">missingOk</a></td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>false</td>
    <td>false</td>
  </tr>
</table> 


### (CD) JSON Field Descriptions


#### (CD) modelType

`modelType` field must be `CategoricalDistribution`.


#### (CD) features 

`features` is an array of strings, where each string contains an Aloha feature specification.  Each of these strings
should just contain a variable definition.  These features are used to create a hash on which the pseudo-randomness
is based.  It can be thought of as the [statistical unit](https://en.wikipedia.org/wiki/Statistical_unit) for the 
experiment being run.

An example value might be: 

```json
[ "${profile.user_id}", "1" ] 
```

where the second element, *1*, acts as a [hash salt](https://en.wikipedia.org/wiki/Salt_\(cryptography\)).  When using
unique hash salts for every hash, one can avoid interaction in multiple hashes.  For more information on salting 
including an example of issues that can arise, see 
[Salts and Seeds](https://deaktator.github.io/presentations/prob-testing/#/10) in 
[Testing in an Unsure World](https://deaktator.github.io/presentations/prob-testing) by 
[Ryan Deak](https://deaktator.github.io).


#### (CD) probabilities

`probabilities` is an array of non-negative numbers.  These values, once 
[normalized](https://en.wikipedia.org/wiki/Normalization_\(statistics\)), will provide the probability of 
selecting one of the associated *labels* at the same index in the 
[parallel](https://en.wikipedia.org/wiki/Parallel_array) `labels` array.  While these values needn't be normalized 
in the JSON, it may help to supply normalized values if exact normalized probabilities can be provided.  If exact 
values cannot be provided, it's OK to provide approximations, so long as the ratios are the same as the final 
normalized [probability vector](https://en.wikipedia.org/wiki/Probability_vector).  For instance, the following are
both perfectly fine, but I would opt for the second: 

```json
[ 1, 1, 1 ]
```

```json
[ 0.33, 0.33, 0.33 ]
```

#### (CD) labels

`labels` are the actual values returned when a value is sampled from the distribution.  The *type of value* in 
`labels` is dependent on the model's output type.  For instance, if the model output type is a string, then the 
JSON value type in `labels` will be  strings.  If the output type is a 64-bit float, the JSON type in the `labels` 
array will be `Number`.


#### (CD) missingOk

`missingOk` determines whether the model should return a value when at least one of the values in `features` 
could not be determined.  If `false`, no score will be returned.  If `true`, then a constant will be used in place 
of the feature value and will be incorporated into the hash.


### (CD) JSON Examples

This example samples values *{ 1, 2, 3, 4 }* based on a user ID and the number of days since Jan 1, 1970.  This means
that for any day, regardless of how many times the model is called, the same user will always get the same value.  This
value can however change from day to day.

```json
{
  "modelType": "CategoricalDistribution",
  "modelId": { "id": 3, "name": "model with E[X] = 3" },
  "features": [ "${profile.id}", "${calculated_values.days_since_epoch}", "1" ],
  "probabilities": [ 0.1, 0.2, 0.3, 0.4 ] ,
  "labels": [ 1, 2, 3, 4 ],
  "missingOk": false
}
```

Imagine a situation where *Alice* and *Bob* are talking to each other and each of them is equally likely to start
a conversation with the other person.  But with a one percent probability *Manny* will intercept the message and 
will forward his own message to the person from whom the message did not originate.  This model can 
probabilistically model the sender of the message that was received by either *Alice* or *Bob*. 

```json
{
  "modelType": "CategoricalDistribution",
  "modelId": { "id": 1, "name": "man-in-the-middle-model" },
  "features": [ "${conversation.id}", "2" ],
  "probabilities": [ 0.495, 0.495, 0.01 ] ,
  "labels": [ "Bob", "Alice", "Manny" ],
  "missingOk": true
}
```


## Constant model

A constant model always returns the same value.

### (C) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aC_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aC_value">value</a></td>
    <td><b>OUTPUT</b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

### (C) JSON Field Descriptions


#### (C) modelType

`modelType` field must be `Constant`.

#### (C) value

`value` field's *type* is dependent on the model's output type.

### (C) JSON Examples

Always return *1*.  Whether *1* is a 32-bit integer, 64-bit integer, 32-bit or 64-bit float is dependent on the 
output type of the model.    

```json
{
  "modelType": "Constant",
  "modelId": { "id": 1, "name": "model that always returns 1" },
  "value": 1
}
```

Always return `"awesome"`.  This model will parse assuming the output type of the model is `String`.    

```json
{
  "modelType": "Constant",
  "modelId": { "id": 1, "name": "model that always returns 'awesome'" },
  "value": "awesome"
}
```


## Decision tree model

Decision trees are what one might expect from a [decision tree](https://en.wikipedia.org/wiki/Decision_tree): they 
encode the ability to return different values based on 
[predicates](https://en.wikipedia.org/wiki/Predicate_\(mathematical_logic\)) that hold for a given input datum.
Aloha encodes trees as a [graph](https://en.wikipedia.org/wiki/Graph_\(mathematics\))-like structure so that the
code could be extended to [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph)s in a straightforward way.
The first node in the `nodes` array is considered to be the root node.

### (DT) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aDT_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aDT_returnBest">returnBest</a></td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aDT_missingDataOk">missingDataOk</a></td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aDT_nodes">nodes</a></td>
    <td>Array</td>
    <td><a href="#aDT_nodes">Node<sup>*</sup></a></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 


### (DT) JSON Field Descriptions


#### (DT) modelType

`modelType` field must be `DecisionTree`.

#### (DT) returnBest

`returnBest` is a Boolean that when `true` let's the decision tree return a value associated with an 
[internal node](https://en.wikipedia.org/wiki/Tree_\(data_structure\)#Terminologies_used_in_Trees) when no 
further progress down the tree can be made.  

When set to false, the model returns an error rather than a score.

Usually, in a decision tree, the inability to produce a path from the tree root to a leaf is indicative of a problem.
The most common problem being that the branching logic at a given node is not
[exhaustive](https://en.wikipedia.org/wiki/Collectively_exhaustive_events).  This will happen when the tree algorithm
checks if it can proceed to any of its children and the predicates associated with each child return `false`.

#### (DT) missingDataOk

`missingDataOk` is a Boolean telling whether missing data is OK.  In the event that `missingDataOk` is set to `false`,
when a variable in a node selector is missing, the node selector should stop and report to the decision tree that it
can't make a decision on which child to traverse.  The subsequent behavior is dictated by [returnBest](#aDT_returnBest).

If `missingDataOk` is `true`, then when a node selector encounters missing data, it can still recover.  For instance,
when a `linear` node selector encounters missing data in one of the predicates, it will assume the predicate evaluates
to `false` and will continue on to the next predicate.  This behavior may vary across node selectors of different
types.

#### (DT) nodes

`nodes` contains the list of nodes in the decision tree.  As was mentioned above, trees are encoded in a tabular
way to provide future extensibility to decision [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph)s.  The 
first node in the `nodes` array is considered to be the root node. The structure of a node is as follows:

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aNode_id">id</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aNode_value">value</a></td>
    <td><b>OUTPUT</b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#Node_Selectors">selector</a></td>
    <td><a href="#Node_Selectors">Node Selector<sup>*</sup></a></td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
</table>

##### (Node) id

`id` is the node id and is used by [node selectors](#Node_Selectors) to determine which child to traverse.  Note that 
this is independent of the model's `id`.

##### (Node) value

`value` in nodes are the values potentially return by the decision tree when a given node is selected. 

##### (Node) selector

`selector` is the data type responsible for determining which child to traverse.  It is required for internal nodes
 but should not be included for leaf nodes.  For the different type of node selectors, see the 
 [Node Selectors](#Node_Selectors) section.

### Node Selectors

*Node selectors* are responsible for determining which child in the tree to traverse.  Currently, the following 
types of node selectors exist: 

* [Linear node selector](#Linear_Node_Selector)
* [Random node selector](#Random_Node_Selector)

#### Linear Node Selector

Linear node selectors work like an 
[IF-THEN-ELSE](https://en.wikipedia.org/wiki/Conditional_\(computer_programming\)#If.E2.80.93then.28.E2.80.93else.29)
statement.  Children are ordered and a predicate is associated with each child.  A 
[linear search](https://en.wikipedia.org/wiki/Linear_search) is performed to find the first predicate that yields a
value of *true*.  If no predicate yields true, then the decision tree split is said to be not 
[exhaustive](https://en.wikipedia.org/wiki/Collectively_exhaustive_events).  In this case, the 
[returnBest](#returnBest) field of the decision tree determines the subsequent behavior.  This selector algorithm acts
in *O*(*N*) time where *N* is node's the number of children.

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#alinear_ns_selectorType">selectorType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#alinear_ns_children">children</a></td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#alinear_ns_predicates">predicates</a></td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

##### (linear ns) selectorType

`selectorType` must be `linear`.

##### (linear ns) children

`children` is a list of `node` `id` values.  This array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `predicates` array.  If index *i* in `predicates` is the first predicate yielding *true*, then the value at 
index *i* in `children` will contain the *id* of the node that will be visited.

##### (linear ns) predicates 

The `predicates` array contains strings where each string is an Aloha feature specification representing a Boolean 
function.  This array is [parallel](https://en.wikipedia.org/wiki/Parallel_array) to the `children` array.  If 
index *i* in `predicates` is the first predicate yielding *true*, then the value at index *i* in `children` will 
contain the *id* of the node that will be visited.


#### Random Node Selector

A random node selector provides a way of pseudo-randomly splitting traffic between one or more outcomes.  This 
selector algorithm acts in *O*(1) time regardless of the number of children.

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#arandom_ns_selectorType">selectorType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#arandom_ns_children">children</a></td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#arandom_ns_probabilities">probabilities</a></td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#arandom_ns_features">features</a></td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table>

##### (random ns) selectorType

`selectorType` must be `random`.

##### (random ns) children

`children` is a list of `node` `id` values.  This array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `probabilities` array.  Samples drawn from the 
[Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution) backed by `probabilities` yield an
index into the `children` array.  The `id` at the chosen index in the `children` array is used to select the child
to traverse. 

**Note**: There is a special case when the length of `children` can be one less than the length of the `probabilities`
array.  In such a case, if `returnBest` is set to *true* then when the sampling from the categorical distribution 
selects last index in `probabilities`, the *current node* (and not a child node) is selected.  See JSON examples
for how this is useful.

##### (random ns) probabilities

`probabilities` is a (possibly unnormalized) [probability vector](https://en.wikipedia.org/wiki/Probability_vector).
This array is [parallel](https://en.wikipedia.org/wiki/Parallel_array) to the `children` array.  Samples drawn 
from the [Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution) backed by `probabilities` 
yield an index into the `children` array.  The `id` at the chosen index in the `children` array is used to select 
the child to traverse. 

While `probabilities` will be normalized and it's not strictly required to provide a normalized probabilities in
`probabilities`, it is most likely more readable to provide normalized probabilities as long as the ratios can be 
encoded exactly.  For instance 
 
```json
[ 0.33, 0.33, 0.33 ]
```

may be preferable to 

```json
[ 1, 1, 1 ]
```

What should not be done is to compromise the ratios for readability.  For instance, if we want to encode probabilities 
of *1/3*, we should **NOT** encode them as:

```json
[ 0.33, 0.34, 0.33]
```

##### (random ns) features

`features` is an array of strings, where each string contains an Aloha feature specification.  Each of these strings
should just contain a variable definition.  These features are used to create a hash on which the pseudo-randomness
is based.  It can be thought of as the [statistical unit](https://en.wikipedia.org/wiki/Statistical_unit) for the 
experiment being run.

An example value might be: 

```json
[ "${profile.user_id}", "1" ] 
```

where the second element, *1*, acts as a [hash salt](https://en.wikipedia.org/wiki/Salt_\(cryptography\)).  When using
unique hash salts for every hash, one can avoid interaction in multiple hashes.  For more information on salting 
including an example of issues that can arise, see 
[Salts and Seeds](https://deaktator.github.io/presentations/prob-testing/#/10) in 
[Testing in an Unsure World](https://deaktator.github.io/presentations/prob-testing) by 
[Ryan Deak](https://deaktator.github.io).


### (DT) JSON Examples


#### (DT) One Node Tree Example

The simplest tree would be a one node tree that is equivalent to a [Constant model](#Constant_model):

```json
{
  "modelType": "DecisionTree",
  "modelId": {"id": 0, "name": "one node tree"},
  "returnBest": false,
  "missingDataOk": false,
  "nodes": [ { "id": 1, "value": 1 } ]
}
```

#### (DT) Three Node Tree Example

A basic tree with one split can be encoded as follows (deeper trees can just repeat the pattern).  It will output the 
string "*short*" when the height is less than 66 (*presumably inches*).  If height is greater than or equal to 66, 
"*tall*" is returned.  Note that the `value` field is related to the model output type so this model will only 
parse when the model output type is `String`.

```json
{
  "modelType": "DecisionTree",
  "modelId": {"id": 0, "name": "height decision tree"},
  "returnBest": false,
  "missingDataOk": false,
  "nodes": [ 
    { 
      "id": 1, 
      "value": "This value won't be returned b/c returnBest and missingDataOk are both false.",
      "selector": { 
        "selectorType": "linear",
        "predicates": [ "${profile.height} < 66", "true" ],
        "children": [ 2, 3 ]
      }
    },
    { "id": 2, "value": "short" },
    { "id": 3, "value": "tall" }
  ]
}
```

#### (DT) Random Branching Example

In the following example, we have a randomized split based on user id where 90% of the time, the value 1 is returned
and 10% of the time, -1 is returned.  If the user ID is missing, the output value will be 0.  This is just for 
illustration purposes.  Usually, you'll want to add a constant salt to `features` like `[ "${profile.user_id}", "1" ]`.

```json
{
  "modelType": "DecisionTree",
  "modelId": {"id": 0, "name": "90/10 random split"},
  "returnBest": true,
  "missingDataOk": false,
  "nodes": [ 
    { 
      "id": 1, 
      "value": 0,
      "selector": { 
        "selectorType": "random",
        "features": [ "${profile.user_id}" ],
        "children": [ 2, 3 ],
        "probabilities": [ 0.9, 0.1 ]
      }
    },
    { "id": 2, "value": 1 },
    { "id": 3, "value": -1 }
  ]
}
```

#### (DT) Short-Circuit Random Branching Example

As was mentioned above, you could have a situation where you want to do something randomly with some probability
by making the `children` array 1 element shorter than the `probabilities` array.  Let's say that we want to do 
the same thing as in the previous example but are willing to give users missing a user ID a negative one.  This 
allows us to essentially reuse the root node for randomization and missing data purposes.  We can write that 
model as follows:

```json
{
  "modelType": "DecisionTree",
  "modelId": {"id": 0, "name": "90/10 random split, user missing id get -1."},
  "returnBest": true,
  "missingDataOk": false,
  "nodes": [ 
    { 
      "id": 1, 
      "value": -1,
      "selector": { 
        "selectorType": "random",
        "features": [ "${profile.user_id}" ],
        "children": [ 2 ],
        "probabilities": [ 0.9, 0.1 ]
      }
    },
    { "id": 2, "value": 1 }
  ]
}
```


## Double-to-Long model

The Double-to-Long model provides an [affine transformation](https://en.wikipedia.org/wiki/Affine_transformation) 
from Double values to Long values.  It works by applying the following transformation: 

*v* = *scale* &times; *submodel value* + *translation*  
*v*&prime; = *IF round THEN* round(*v*) *ELSE* floor(*v*)  
*output* = max(*clampUpper*, min(*v*&prime;, *clampUpper*))

This model is useful for [eHarmony](http://www.eharmony.com)-specific purposes, but others may find it useful as well. 

### (DtL) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aDtL_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aDtL_submodel">submodel</a></td>
    <td><b>Model<sup>*</sup></b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aDtL_scale">scale</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>1</td>
  </tr>
  <tr>
    <td><a href="#aDtL_translation">translation</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>0</td>
  </tr>
  <tr>
    <td><a href="#aDtL_clampLower">clampLower</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>-&infin;</td>
  </tr>
  <tr>
    <td><a href="#aDtL_clampUpper">clampUpper</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>&infin;</td>
  </tr>
  <tr>
    <td><a href="#aDtL_round">round</a></td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>false</td>
    <td>false</td>
  </tr>
</table> 


### (DtL) JSON Field Descriptions

#### (DtL) modelType

`modelType` field must be `DoubleToLong`.

#### (DtL) submodel

`submodel` must either be a JSON object containing an Aloha model or it can be a JSON object with exactly one field,
`import` whose associated value is a JSON string containing an 
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  For instance: 

```json
{ "import": "/path/to/aloha/model.json" }
```

Either way, the submodel is expected to be a model that takes the same input type as the Double-to-Long model itself
and the submodel should have an output type of Double.  The output to of the Double-to-Long model is obviously 
expected to be a 64-bit long-valued integer. 

#### (DtL) scale

`scale` is the multiplier in the affine transformation.  It can be any 64-bit float.  When not supplied, the default 
value will be 1.

#### (DtL) translation

`translation` is the additive term in the affine transformation.  It can be any 64-bit float.  When not supplied, 
the default value will be 0.

#### (DtL) clampLower

`clampLower` is the lower [clamp](https://en.wikipedia.org/wiki/Clamping_\(graphics\)) value.  It is a 64-bit long-valued
int.  When not supplied, the default value will be `-9,223,372,036,854,775,808`.

#### (DtL) clampUpper

`clampUpper` is the upper [clamp](https://en.wikipedia.org/wiki/Clamping_\(graphics\)) value.  It is a 64-bit long-valued
int.  When not supplied, the default value will be `9,223,372,036,854,775,807`.

#### (DtL) round

`round` is a Boolean that determines whether the affine-transformed value should be rounded (`true`) or 
[floored](https://en.wikipedia.org/wiki/Floor_and_ceiling_functions) (`false`).   

### (DtL) JSON Examples

#### (DtL) Minimal Example

A double-to-long model whose submodel is a constant model returning 5.5.  The outer model floors *5.5* to the 64-bit long 
value *5*.

```json
{
  "modelType": "DoubleToLong",
  "modelId": { "id": 0, "name": "" },
  "submodel": {
    "modelType": "Constant",
    "modelId": { "id": 1, "name": "Constant model returning 5.5" },
    "value": 5.5
  }
}
```

#### (DtL) Full Example

A double-to-long model whose submodel is a constant model returning `-13`.  The outer model multiplies by `-0.5`, adds 
`2`, rounds, then takes the min of `clampUpper` (`8`) and `9`, which comes out to `8`.

```json
{
  "modelType": "DoubleToLong",
  "modelId": { "id": 0, "name": "" },
  "clampLower": 6,
  "clampUpper": 8,
  "scale": -0.5,
  "translation": 2,
  "round": true,
  "submodel": {
    "modelType": "Constant",
    "modelId": { "id": 1, "name": "Constant model returning -13" },
    "value": -13
  }
}
```


## Error model

An error model returns an error.  This is not the same as throwing an exception.  Error models return values.

### (E) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aE_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aE_errors">errors</a></td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>Array[ "Error with unspecified reason." ]</td>
  </tr>
</table> 


### (E) JSON Field Descriptions

#### (E) modelType

`modelType` field must be `Error`.

#### (E) errors

`errors` is an array of error strings to provide.  This is an optional parameter.  An empty array is permitted. 

### (E) JSON Examples

```json
{
  "modelType": "Error",
  "modelId": { "id": 0, "name": "" },
}
```

```json
{
  "modelType": "Error",
  "modelId": { "id": 0, "name": "" },
  "errors": [ "error 1", "error 2" ]
}
```

## Error-swallowing model

Error swallowing model makes an attempt to trap exceptions and return them as errors instead.  While this is not 
foolproof, it was tested against 
[SchrodingerException](https://deaktator.github.io/2015/08/07/can-your-exception-handling-code-handle-the-schrodingerexception-juggernaut/),
an exception specifically designed to be as harmful as possible.  The implementation uses `scala.util.Try` which means 
errors that are caught are dictated by 
[scala.util.control.NonFatal](https://github.com/scala/scala/blob/v2.10.3/src/library/scala/util/control/NonFatal.scala).

### (ES) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aES_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aES_submodel">submodel</a></td>
    <td><b>Model<sup>*</sup></b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aES_recordErrorStackTraces">recordErrorStackTraces</a></td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>false</td>
    <td>true</td>
  </tr>
</table> 

### (ES) JSON Field Descriptions

#### (ES) modelType

`modelType` field must be `ErrorSwallowingModel`.

#### (ES) submodel

`submodel` must either be a JSON object containing an Aloha model or it can be a JSON object with exactly one field,
`import` whose associated value is a JSON string containing an 
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  For instance: 

```json
{ "import": "/path/to/aloha/model.json" }
```

Either way, the submodel is expected to be a model with the same input and output type as the Error-swallowing model 
itself.

#### (ES) recordErrorStackTraces

`recordErrorStackTraces` is a boolean indicating whether to record stack traces in the returned error.

### (ES) JSON Examples

```json
{
  "modelType": "ErrorSwallowingModel",
  "modelId": { "id": 0, "name": "0" },
  "recordErrorStackTraces": true,
  "submodel": {
    "modelType": "Regression",
    "modelId": { "id": 1, "name": "1" },
    "features": {
      "evil_feature": "throw new RuntimeException"
    },
    "weights": {}
  }
}
```

## Model decision tree model

Model decision tree models are just like [Decision tree models](#Decision_tree_model) exception that the `value` field
must either be a JSON object containing an Aloha model or it can be a JSON object representing a model import.
For instance: 

```json
{ "import": "/path/to/aloha/model.json" }
```

where `import` is a JSON string containing an
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  

The submodels, input, and output types are expected to be the same as the input and output type of the model decision 
tree model.  The model works by using the decision tree algorithm to select a node containing a model, and then it 
applies the submodel it finds in the node to the same input data that was passed to the decision tree. 

For more information, see the section on [Decision tree models](#Decision_tree_model).

## Regression model

The regression model implementation in Aloha is a [sparse](https://en.wikipedia.org/wiki/Sparse_matrix) 
[polynomial regression](https://en.wikipedia.org/wiki/Polynomial_regression) model.  This is a superset of and therefore
subsumes [linear regression](https://en.wikipedia.org/wiki/Linear_regression) models.

### (R) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aR_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_features">features</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_weights">weights</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_higherOrderFeatures">higherOrderFeatures</a></td>
    <td>Array</td>
    <td>Object</td>
    <td>false</td>
    <td>Empty Array</td>
  </tr>
  <tr>
    <td><a href="#aR_spline">spline</a></td>
    <td>Array</td>
    <td>Object</td>
    <td>false</td>
    <td>Empty Array</td>
  </tr>
  <tr>
    <td><a href="#aR_numMissingThreshold">numMissingThreshold</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>&infin;</td>
  </tr>
</table> 


### (R) JSON Field Descriptions

#### (R) modelType

`modelType` field must be `Regression`.

#### (R) features

`features` is the map of features that are included in the model.  The keys in the `features` represent the feature 
names; the values can take on one of two forms.  They can either be a String or a JSON object.  If they are a JSON
object, they must have the following format: 

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aR_feature_spec">spec</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_feature_defVal">defVal</a></td>
    <td>Array</td>
    <td>Array (2 elements. 0: (String) feature name, 1: (Number) feature value)</td>
    <td>false</td>
    <td>Empty Array</td>
  </tr>
</table>

##### (R) feature spec

`spec` contains an Aloha feature specification.  These feature specifications produce functions whose input type
is the same as the regression model's input type and the output type is an iterable collection of 
(*String*, *Double*) key-value pairs.  Specifically, the Scala type is `scala.collection.Iterable[(String, Double)]`.
An example of such a `spec` string is:

```json
"Seq((\"\", ${profile.height}))"
```

This creates one key-value pair whose *key* is the empty string and whose *value* is the height extract from a 
profile.  One might ask, "*why would the key be the empty string?*"  The answer is that the keys are prepended with the 
feature name.  So, for instance, the above *spec* string might be included in a feature as follows: 

```json
{
  "modelType": "Regression",
  ...
  "features": {
    "height": { "spec": "Seq((\"\", ${profile.height}))" }
  }
  ...
}
```

Given the above model, let's say the `profile.height` yields a value of *66*.  Then the feature key-value pairs produced 
would be: { `("height", 66)` }.

There is a lot of supporting code available to the Aloha user when importing 
`com.eharmony.aloha.feature.BasicFunctions._`.  Among these is an implicit conversion from numeric types to sequences
of key-value pairs.  This allows the user to write the previously mentioned Aloha function specification as: 
 
```json
"${profile.height}"
```

Aloha will take care of converting it to:

```json
"Seq((\"\", ${profile.height}))"
```

assuming `profile.height` is an appropriate numeric type.

##### (R) feature defVal

**NOTE**: [Ryan Deak](https://deaktator.github.io), author of Aloha has expressed a personal distaste for the use
of this feature.  It can however create better models at the cost of readability and error reporting.

`defVal` is the value that is returned if the `spec` references a variable whose presence is *optional* and 
whose value is not present for the current model input.  As with the Iterable of key-value pairs produced by the 
function created by `spec`, keys will have the *feature name* prepended in the final feature value (see below for
an example).  

`defVal` is a JSON Array of Arrays.  The inner Array should contain a String followed by a Number.  For instance, you 
may want to use something like the following: 

```json
[["=UNKNOWN", 1]]
```
  
[eHarmony](http://www.eharmony.com) uses this in a lot of models.  Let's put this into context: 

```json
{
  "modelType": "Regression",
  ...
  "features": {
    "height": { "spec": "Seq((\"\", ${profile.height}))", "defVal": [["=UNKNOWN", 1]] }
  }
  ...
}
```

and imagine `profile.height` is an optional variable that is not present in the current model input.  Then the model
can't produce the key-value for *height* feature and defaults to using `defVal` to produce: 
{ `("height=UNKNOWN", 1)` }.  The reason we use the value *1* is that this is exactly the form of an 
[indicator variable](https://en.wikipedia.org/wiki/Dummy_variable_\(statistics\)).  That way, the regression model
can have an associated weight in the **&beta;** vector.

There is a consequence of providing a `defVal`.  When the `defVal` field is provided and its value is the non-empty Array,
this will affect the reporting of the number of missing features.  For more information, see the 
[numMissingThreshold](#aR_numMissingThreshold) section. 

##### (R) spec (as a String)

As was mentioned above, features values in the regression model JSON can either be represented as a String or Object,
the Object case is shown above.  If you don't provide a `defVal` in the above object, the specification Object can 
be replaced with just the `spec` value.  For instance, the example from above: 

```json
{
  "modelType": "Regression",
  ...
  "features": {
    "height": { "spec": "Seq((\"\", ${profile.height}))" }
  }
  ...
}
```

could be rewritten as: 

```json
{
  "modelType": "Regression",
  ...
  "features": {
    "height": "Seq((\"\", ${profile.height}))"
  }
  ...
}
```

and with what we learned about imports, if `com.eharmony.aloha.feature.BasicFunctions._` is imported, the JSON can 
be simplified further to: 

```json
{
  "modelType": "Regression",
  ...
  "features": {
    "height": "${profile.height}"
  }
  ...
}
```

This is easier to read and write and more aesthetically pleasing in general.

#### (R) weights

`weights` is a representation of the *first-order* feature weights in the regression **&beta;** vector.  By
*first-order*, we mean terms of [degree](https://en.wikipedia.org/wiki/Degree_of_a_polynomial) *1* in the
[polynomial](https://en.wikipedia.org/wiki/Polynomial) expression which is being regressed in the model.

`weights` is a JSON Object where the keys are the same as the keys produced in the feature map.  The values are the
weights associated with the each feature value.  

**NOTE**: Only if the feature values (the numbers in the [feature](#aR_features) key-value pairs) are 
[standardized](https://en.wikipedia.org/wiki/Standard_score#Calculation_from_raw_score), can one infer feature
importance from the magnitude of the weight values.  This is because if the weights are not 
[standardized](https://en.wikipedia.org/wiki/Standard_score#Calculation_from_raw_score), the scales of the feature 
values will differ and the weights will adapt to these scales.

An example of weights is: 

```json
{
  "modelType": "Regression",
  ...
  "weights": {
    "height": 1.23,
    "weight": -2.34
  }
  ...
}
```

#### (R) higherOrderFeatures

`higherOrderFeatures` encodes the weights associated with terms in the 
[polynomial](https://en.wikipedia.org/wiki/Polynomial) expression with 
[degree](https://en.wikipedia.org/wiki/Degree_of_a_polynomial) greater than *1*.  This is optional and is used for
[polynomial regression](https://en.wikipedia.org/wiki/Polynomial_regression) models but is not necessary for 
[linear regression](https://en.wikipedia.org/wiki/Linear_regression) models.  It is an *optional* field.  An example
looks like: 

```json
{
  "modelType": "Regression",
  "features": {
    "m_ht_lt_63": "ind(${male.height} < 63)",
    "f_ht_gt_66": "ind(66 < ${female.height})",
  },
  ...
  "higherOrderFeatures": [
    { 
      "wt": -5.1, 
      "features": { 
        "m_ht_lt_63": ["m_ht_lt_63=true"], 
        "f_ht_gt_66": ["f_ht_gt_66=true"] 
      }
    },
    { 
      "wt": 1.2,
      "features": {
        "m_ht_lt_63": ["m_ht_lt_63=false"],
        "f_ht_gt_66": ["f_ht_gt_66=false"]
      }
    }
  ]
}
```

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aR_higherOrderFeatures_wt">wt</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_higherOrderFeatures_features">features</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table>

Given `features` with feature names "*m_ht_lt_63*" and "*f_ht_gt_66*", where "*m_ht_lt_63*" is an indicator variable
meaning male in a [dyad](https://en.wikipedia.org/wiki/Dyad_\(sociology\)) has a height of less than 63 inches and 
"*f_ht_gt_66*" means the female's height is greater than 66 inches, we see that when the male is on the shorter 
side and the female is taller, this has a disproportionally large negative effect versus when the male is either 
not that much shorter (&le; 3 inches) or is taller than the the female.

##### (R) higherOrderFeatures wt

We can tell this because the `wt` field in each higher-order features.  This is the weight associated with the 
**&beta;** for the feature.  Within the features

##### (R) higherOrderFeatures features

`features` is a map where the keys are the feature names from the top-level [features](#aR_features) map in the 
regression model.  The value associated with a key is a non-empty Array containing the **feature keys** from the 
features created by the top-level [features](#aR_features) map.  That is, when a feature function produces the 
Iterable of (*String*, *Double*) key-value pairs, the *String* in the first index of the tuple is the String 
that goes in this Array.  It is possible to have Arrays of length larger than two.  This will happen most commonly 
occurs when the term of interest in the polynomial is some variable raised to a power.  

For instance, imagine someone throwing a ball 80 mph (35.7632 *m*/*s*) at a 30&deg; angle with a release point 
8.25ft (2.5146 *m*) off the ground.  The vertical velocity component is 17.8816 *m*/*s* = 0.5 &times; 35.7632. 
Using the equation from [classical mechanics](https://en.wikipedia.org/wiki/Classical_mechanics): 

<em>h</em>(<em>t</em>) = 1/2<em>gt</em><sup>2</sup> + <em>v</em><sub>0</sub><em>t</em> + <em>h</em><sub>0</sub> &nbsp; &nbsp; Where <em>g</em> = -9.8 <em>m</em>/<em>s</em> 

the height of the ball in meters could be encoded as:

```json
{
  "modelType": "Regression",
  "modelId": { "id": 0, "name": "80mph throw at 30 degree angle" },
  "features": {
    "intercept": "intercept",
    "time": "${time}"
  },
  "weights": {
    "intercept": 2.5146,
    "time": 17.8816
  },
  "higherOrderFeatures": [
    { "wt": -4.9, "features": { "time": [ "time", "time" ] } } 
  ]
}
```

#### (R) spline

`spline` is an optional field representing a 
[spline](https://en.wikipedia.org/wiki/Spline_\(mathematics\)) function.  The spline is applied to the inner product 
of the feature vector, **X** and the **&betal** vector (**X&beta;**); and acts like an 
[inverse link function](https://en.wikipedia.org/wiki/Generalized_linear_model#Link_function) in 
[GLM](https://en.wikipedia.org/wiki/Generalized_linear_model)s.  The reason a spline is provided rather than an 
explicit link is that oftentimes we want to [calibrate](https://en.wikipedia.org/wiki/Calibration_\(statistics\)) 
our classifiers to ensure that the class probabilities yield accurate estimates.  For more information see
[Charles Elkan](http://cseweb.ucsd.edu/~elkan/)'s and [Bianca Zadrozny](http://www2.ic.uff.br/~bianca/)'s papers on 
model calibration or checkout Jan Hendrik Metzen's 
[blog post on calibration](https://jmetzen.github.io/2015-04-14/calibration.html) for a nice explanation.


<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aR_spline_min">min</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aR_spline_max">max</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
    <tr>
      <td><a href="#aR_spline_knots">knots</a></td>
      <td>Array</td>
      <td>Number</td>
      <td>true</td>
      <td>N / A</td>
    </tr>
</table>

##### (R) spline min

`min` is the minimum value in the [domain](https://en.wikipedia.org/wiki/Domain_of_a_function) of the spline.  Values
less than `min` will be [clamped](https://en.wikipedia.org/wiki/Clamping_\(graphics\)).

##### (R) spline max

`max` is the minimum value in the [domain](https://en.wikipedia.org/wiki/Domain_of_a_function) of the spline.  Values
greater than `max` will be [clamped](https://en.wikipedia.org/wiki/Clamping_\(graphics\)).

##### (R) spline knots

`knots` defines the [piecewise linear](https://en.wikipedia.org/wiki/Piecewise_linear_function) 
[spline](https://en.wikipedia.org/wiki/Spline_\(mathematics\)).  The knots are the values of the 
[codmain](https://en.wikipedia.org/wiki/Codomain).  The associated 
[domain](https://en.wikipedia.org/wiki/Domain_of_a_function) values can be calculated from the `min`, `max`, and size
of the `knots` Array.  The size of `knots` must be at least two, unless in the special case `min` equals `max`, in
which case knots is expected to contain *exactly* one element.

#### (R) numMissingThreshold

`numMissingThreshold` is an optional integral value.  When supplied, if the number of [features](#aR_features) that
yield empty Iterables (*size zero*) **exceeds** `numMissingThreshold`, then an error rather than a score will be 
returned.  

### (R) JSON Examples

#### (R) Basic Example

This is the most basic regression model there is, just an intercept term:

> 0.5 = **&beta;X** = [0.5]<sup>T</sup>[1]<sup>T</sup>

```json
{
  "modelType": "Regression",
  "modelId": { "id": 0, "name": "basic example" },
  "features": { "intercept": "intercept" },
  "weights": { "intercept": 0.5 }
}
```

There is a little bit going on here behind the scenes.  The `intercept` function that appears as the sole *value* in 
the `features` map is a special function that is included when importing `com.eharmony.aloha.feature.BasicFuncions._`. 
It is in the `com.eharmony.aloha.feature.Intercept` trait that is mixed into `BasicFuncions`.  What this function does
is create one key-value pair: `("", 1.0)`.  Then, as has been mentioned before, the feature name is prepended to the 
key, so the final key-value pair is `("intercept", 1.0)`.  The `1.0` represents the 
[bias term](https://en.wikipedia.org/wiki/Artificial_neuron#Basic_structure) and the associated `intercept` in the 
`weights` map represents the model's output when all [covariate](https://en.wikipedia.org/wiki/Covariate) data is 
omitted.
             

#### (R) higherOrderFeatures Example

This is repeated from the [higherOrderFeatures features](#aR_higherOrderFeatures_features) section.  It illusrates 
modeling the equation
<em>h</em>(<em>t</em>) = 1/2<em>gt</em><sup>2</sup> + <em>v</em><sub>0</sub><em>t</em> + <em>h</em><sub>0</sub>.  See
that section for more information.

```json
{
  "modelType": "Regression",
  "modelId": { "id": 0, "name": "80mph throw at 30 degree angle" },
  "features": {
    "intercept": "intercept",
    "time": "${time}"
  },
  "weights": {
    "intercept": 2.5146,
    "time": 17.8816
  },
  "higherOrderFeatures": [
    { "wt": -4.9, "features": { "time": [ "time", "time" ] } } 
  ]
}
```

## Segmentation model

Segmentation models take a submodel that returns a value with an imposed 
[total ordering](https://en.wikipedia.org/wiki/Total_order) and then create a
[partition](https://en.wikipedia.org/wiki/Partition_of_a_set) on the set of values the submodel produces.  The 
segmentation model maps elements in the same induced 
[equivalence class](https://en.wikipedia.org/wiki/Equivalence_class) to a label unique to each class.


### (S) JSON Fields


<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aS_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_subModel"><b>subModel</b></a></td>
    <td><b>Model<sup>*</sup></b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_subModelOutputType">subModelOutputType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_thresholds">thresholds</a></td>
    <td>subModel Output</td>
    <td><b>subModel Output</b></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_labels">labels</a></td>
    <td>Array</td>
    <td><b>Output</b></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

### (S) JSON Field Descriptions

#### (S) modelType

`modelType` field must be `Segmentation`.

#### (S) subModel

`submodel` must either be a JSON object containing an Aloha model or it can be a JSON object that represents the 
import of an external model.  The `import` field has an associated value which is a JSON string containing an 
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  For instance: 

```json
{ "import": "/path/to/aloha/model.json" }
```

Either way, the submodel is expected to be a model that takes the same input type as the Segmentation model itself
and the submodel should have an output type dictated by the [subModelOutputType](#aS_subModelOutputType) value.


#### (S) subModelOutputType

`subModelOutputType` determines the output type of the submodel.  `subModelOutputType` must be one of:

* Byte
* Short
* Int
* Long
* Float
* Double
* String

#### (S) thresholds

`thresholds` is the set of values that partitions the space into intervals.  It should be sorted according to the
natural [total ordering](https://en.wikipedia.org/wiki/Total_order) associated with the output type of the submodel.  
This Array must have one less element than the `labels` Array.  If, for instance, the submodel produces a value 
"less than" the first value in `thresholds`, then the first element in `labels` is returned.  If that's not the case, 
the search continues until an element in `thresholds` is greater than the submodel output.  If there is no such value, 
the last element in `labels` is returned.

#### (S) labels

`labels` contains the labels associated with each [equivalence class](https://en.wikipedia.org/wiki/Equivalence_class) 
induced by `thresholds`.  The type of each label must be the same as the Segmentation model output type.  `labels`
must have one more element that `thresholds`.  See [thresholds](#aS_thresholds) section for more details.

### (S) JSON Examples

The following shows a model whose output type is String, with a submodel that returns a value in { 1, 4, 5, 7 }.  
The mapping from submodel value to Segmentation model value is: 

<table>
  <tr>
    <th>subModel</th>
    <th>Segmentation</th>
  </tr>
  <tr>
    <td>1</td>
    <td>smallest</td>
  </tr>
  <tr>
    <td>4</td>
    <td>second smallest</td>
  </tr>
  <tr>
    <td>5</td>
    <td>second largest</td>
  </tr>
  <tr>
    <td>7</td>
    <td>largest</td>
  </tr>
</table> 


```json
{
  "modelType": "Segmentation",
  "modelId": { "id": 0, "name": "" },
  "thresholds": [ 2, 5, 6 ],
  "labels": [ "smallest", "second smallest", "second largest", "largest" ],
  "subModelOutputType": "Int",
  "subModel": {
    "modelType": "CategoricalDistribution",
    "modelId": { "id": 1, "name": "" },
    "features": [ "${profile.id}", "1" ],
    "probabilities": [ 0.25, 0.25, 0.25, 0.25 ] ,
    "labels": [ 1, 4, 5, 7 ],
    "missingOk": true
  }
}
```


## Vowpal Wabbit model

The [Vowpal Wabbit](https://github.com/JohnLangford/vowpal_wabbit/wiki) model provides the feature extraction 
capabilities of an Aloha native [Regression model](#Regression_model), but delegates to VW's 
[JNI layer](https://github.com/JohnLangford/vowpal_wabbit/tree/master/java) for prediction, which is much more 
powerful.  Note that this shares a fair amount of code with [Regression model](#Regression_model) so be sure to
look at the docs there.

**NOTE**: This model is in module **aloha-vw-jni**, not **aloha-core**.  To use, be sure to include the proper 
maven dependency: 

```xml
<dependency>
  <groupId>com.eharmony</groupId>
  <artifactId>aloha-vw-jni</artifactId>
  <version>...</version>
</dependency>
```
 
VW does not need to be installed on the computer where the Aloha VW model is used.  The VW JNI library contains
operating system-specific versions of the VW library, so it will be included transitively when the `aloha-vw-jni`
maven dependency is pulled in.  Since VW is **not binary compatible across versions**, the trained VW binary model
needs to be trained on the same VW version that is included in the `aloha-vw-jni`
[POM](https://github.com/eHarmony/aloha/blob/master/aloha-vw-jni/pom.xml).  Make sure to look for this dependency.

```xml
<dependency>
 <groupId>com.github.johnlangford</groupId>
 <artifactId>vw-jni</artifactId>
 <version>[THIS VERSION NEEDS TO MATCH THE VW USED TO TRAIN THE MODEL]</version>
</dependency>
```

### (VW) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aVW_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_features">features</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_vw">vw</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_namespaces">namespaces</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_numMissingThreshold">numMissingThreshold</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_notes">notes</a></td>
    <td>Array</td>
    <td>String</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_spline">spline</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
</table> 

### (VW) JSON Field Descriptions

#### (VW) modelType

`modelType` field must be `VwJNI`.

#### (VW) features

See [Regression model features](#aR_features) for details.

#### (VW) vw

The `vw` Object has two main components.  The first component is a representation of the binary VW model, or 
"*initial regressor*" in VW terminology.  This is the model parameterization.  The second component is the set of 
parameters used by VW.  This includes things like the link function, prediction ranges, skip grams, namespace 
interactions, regularization, etc.  To see descriptions of these parameters, see the 
[VW wiki](https://github.com/JohnLangford/vowpal_wabbit/wiki) or run: 
  
```bash
vw -h
```

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aVW_vw_creationTime">creationTime</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>number of milliseconds since <a href="https://en.wikipedia.org/wiki/Unix_time">epoch</a> for current time.</td>
  </tr>
  <tr>
    <td><a href="#aVW_vw_model">model</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_vw_modelUrl">modelUrl</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_vw_via">via</a></td>
    <td>String with one of: "<em>file</em>", "<em>vfs1</em>", "<em>vfs2</em>"</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aVW_vw_params">params</a></td>
    <td>String or Array of String</td>
    <td>N / A</td>
    <td>false</td>
    <td>Empty String</td>
  </tr>
</table>

##### (VW) vw creationTime

`creationTime` is optional and is used when copying the content of the binary VW model to the local disk on the 
computer where the Aloha model is run.  When no value is supplied, the value will be set to
`java.lang.System.currentTimeMillis()`.

##### (VW) vw model

Exactly one of `model` or `modelUrl` is required.  When `model` is provided, it should be a string 
with the base64-encoded content of the binary VW model.  For instance, this may be something like the string: 

"`BgAAADguMC4wAG0AAAAAAACAPxIAAAAAAAAAAAAAAAAAAAABAAAAAAA=`"

which might come from: 

```bash
echo "" | vw -f model.vw 2>/dev/null && cat model.vw | base64 && rm -f model.vw
```

##### (VW) vw modelUrl

Exactly one of `model` or `modelUrl` is required.  When `modelUrl` is provided, it should be a string contain an
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  This is the location from which the
content will be copied to the local disk.

##### (VW) vw via

`via` is optional.  It is a string and can be one of: `file`, `vfs1`, `vfs2`.  It provides a way for Aloha 
users to use different versions of [Apache VFS](https://commons.apache.org/proper/commons-vfs/).  This is useful 
because VFS provides a common interface to different file systems.  Since VFS provides a plugin architecture, 
different plugins might be available to different versions of VFS.  For instance, the HDFS plugin eHarmony uses 
(at the time of this writing) is a VFS 1 plugin.

`via` is only to be used in conjunction with the `modelUrl` field.  When supplied with the `model` field,
the value associated with the `via` field is ignored.  File operations used to copy to the local disk
the contents associated with the `model` field are `java.io.File` based and don't make use of Apache VFS.

##### (VW) vw params

`params` is not required.  It can either be specified as a string or an Array of strings (which are joined with a 
space in between each item).  This is how VW parameters are specified.   


#### (VW) namespaces

A JSON object representing a map from [namespace](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format) 
names to an Array of feature names to be placed in that 
[namespace](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format).  For example:

```json
  "features": {
    "height_mm": "${profile.height_cm} * 10"
  },
  ...
  "namespaces": {
    "personal_features": [ "height_mm" ]
  }
```

#### (VW) numMissingThreshold

See [Regression model numMissingThreshold](#aR_numMissingThreshold) for details.

#### (VW) notes

This is just an optional Array of Strings for documentation purposes.

#### (VW) spline

See [Regression model spline](#aR_spline) for details.

### (VW) JSON Examples

#### (VW) Base64 Encoded Model JSON Example

```json
{
  "modelType": "VwJNI",
  "modelId": { "id": 0, "name": "" },
  "features": {
    "height_mm": "Seq((\"1800\", 1.0))"
  },
  "namespaces": {
    "personal_features": [ "height_mm" ]
  },
  "vw": {
    "params": "--quiet -t",
    "model": "[base64 encoded model string here]"
  }
}
```

#### (VW) Model URL JSON Example


```json
{
  "modelType": "VwJNI",
  "modelId": { "id": 0, "name": "model name" },
  "features": {
    "height_mm": "Seq((\"1800\", 1.0))"
  },
  "notes": [
    "This is a note"
  ],
  "namespaces": {
    "personal_features": [ "height_mm" ]
  },
  "vw": {
    "params": ["--quiet", "-t"],
    "model": "file:///Users/xyz/model.vw"
  }
}
```

#### (VW) Model URL via 'vfs1' JSON Example


```json
{
  "modelType": "VwJNI",
  "modelId": { "id": 0, "name": "model name" },
  "features": {
    "height_mm": "Seq((\"1800\", 1.0))"
  },
  "notes": [
    "This is a note"
  ],
  "namespaces": {
    "personal_features": [ "height_mm" ]
  },
  "vw": {
    "params": ["--quiet", "-t"],
    "model": "hdfs://some/path/to/data",
    "via": "vfs1"
  }
}
```

## H<sub>2</sub>O model

[H<sub>2</sub>O](http://h2o.ai) model is a wrapper around the [H<sub>2</sub>O](http://h2o.ai) library that provides
feature extraction and delegates to [H<sub>2</sub>O](http://h2o.ai) for predictions.  [H<sub>2</sub>O](http://h2o.ai)
provides many types of models.  Users will most likely find the biggest benefit in using the non-linear model
offerings.

**NOTE**: This model is in module **aloha-h2o**, not **aloha-core**.  To use, be sure to include the proper
maven dependency:

```xml
<dependency>
  <groupId>com.eharmony</groupId>
  <artifactId>aloha-h2o</artifactId>
  <version>...</version>
</dependency>
```

[H<sub>2</sub>O](http://h2o.ai) does not need to be installed on the computer where the Aloha
[H<sub>2</sub>O](http://h2o.ai) model is used.  Since [H<sub>2</sub>O](http://h2o.ai) is a java library and the model
is embedded in an Aloha model and is compiled on the fly, ensure that you are producing a model with an
[H<sub>2</sub>O](http://h2o.ai) version compatible with the one found in the
[aloha-h2o/pom.xml](https://github.com/eHarmony/aloha/blob/master/aloha-h2o/pom.xml).  Search for `h2o.version` in
the pom.


### (H) JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><a href="#aH_modelType">modelType</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aH_features">features</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aH_numMissingThreshold">numMissingThreshold</a></td>
    <td>Number</td>
    <td>N / A</td>
    <td>false</td>
    <td>&infin;</td>
  </tr>
  <tr>
    <td><a href="#aH_model">model</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aH_modelUrl">modelUrl</a></td>
    <td>String</td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aH_via">via</a></td>
    <td>String with one of: "<em>file</em>", "<em>vfs1</em>", "<em>vfs2</em>"</td>
    <td>N / A</td>
    <td>false</td>
    <td>"<em>vfs2</em>"</td>
  </tr>
</table>

### (H) JSON Field Descriptions

#### (H) modelType

`modelType` field must be `H2o`.

#### (H) features

`features` is similar to [features](#aR_features) found in the Regression model. Like in the Regression
model, the `features` JSON object represents a map from feature name to a extraction function.  There are however
differences between the two.  Since the variable types in [H<sub>2</sub>O](http://h2o.ai) can be either `Double`
or `String`, the JSON has to allow for this. So the JSON for a feature can take on one of two forms.  It can
either be a JSON string or an object.

1. If the feature is encoded as a string, it is assumed that the feature is a Double-based feature and that
no default will be supplied to the [H<sub>2</sub>O](http://h2o.ai) if any of the data used to compute the
feature is missing.
1. If the feature is encoded as an object, it must supply a `type` field with a value of either `string` or
`double`.  It must also have a `spec` field whose value is the feature specification.  Additionally, an
optional `defVal` field may be provided that determines the feature's return value in the case that data
required in the feature computation is missing.  The default must be the same type as the one indicated in
the `type` field.  For instance, in the following `features` map, `weight_1` and `weight_2` have an
equivalent meaning.

```json
{
  "weight_1": "${u.wt}",
  "weight_2": { "type": "double",
                "spec": "${u.wt}" },
  "wt_w_def": { "type": "double",
                "spec": "${u.wt}",
                "defVal": 0 },
  "height":   { "type": "string",
                "spec": "if (${u.ht} < 84) \"UNDER_7\" else \"OVER_7\"" },
  "ht_w_def": { "type": "string",
                "spec": "if (${u.ht} < 84) \"UNDER_7\" else \"OVER_7\"",
                "defVal": "UNDER_7" }
}
```

#### (H) model

Exactly one of `model` or `modelUrl` is required.  When `model` is provided, it should be a string
with the base64-encoded content a generated [H<sub>2</sub>O](http://h2o.ai) model.

#### (H) modelUrl

Exactly one of `model` or `modelUrl` is required.  When `modelUrl` is provided, it should be a string contain an
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  This is the location from which the
content will be copied to the local disk.

#### (H) via

`via` is optional.  It is a string and can be one of: `file`, `vfs1`, `vfs2`.  It provides a way for Aloha
users to use different versions of [Apache VFS](https://commons.apache.org/proper/commons-vfs/).  This is useful
because VFS provides a common interface to different file systems.  Since VFS provides a plugin architecture,
different plugins might be available to different versions of VFS.  For instance, the HDFS plugin eHarmony uses
(at the time of this writing) is a VFS 1 plugin.

`via` is only to be used in conjunction with the `modelUrl` field.  When supplied with the `model` field,
the value associated with the `via` field is ignored.  File operations used to copy to the local disk
the contents associated with the `model` field are `java.io.File` based and don't make use of Apache VFS.

#### (H) numMissingThreshold

numMissingThreshold is an optional integral value. When supplied, if the number of features no value
exceeds numMissingThreshold, then an error rather than a score will be returned.  Note that when a feature
default is provided and that default is returned, it is not counted as a missing feature.

### (H) JSON Examples

```json
{
  "modelType": "H2o",
  "modelId": { "id": 0, "name": "proto model" },
  "notes": [
    "The features are all calculated in different varieties of an identity function to show off some of the power of Aloha"
  ],
  "features": {
    "Sex":            { "type": "string", "spec": "${sex}.name.substring(0,1)" },
    "Length":         "1d + ${length} - 1L",
    "Diameter":       "${diameter} * 1f",
    "Height":         "identity(${height})",
    "Whole weight":   "${weight.whole} * ${height} / ${height}",
    "Shucked weight": "pow(${weight.shucked}, 1)",
    "Viscera weight": "${weight.viscera} * (pow(sin(${diameter}), 2) + pow(cos(${diameter}), 2))",
    "Shell weight":   "${weight.shell} + log((${length} + ${height}) / (${height} + ${length}))",
    "Circumference (unused)":  "Pi * ${diameter}"
  },
  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
}
```