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

This is just a suggestion but it will pay huge dividends in terms cleaner data collection:

> Models should for practical purposes by immutable.  This means that whenever a model is edited in such a way that 
> the model functionally changes, the model's `modelId`'s `id` field should be changed **And all ancestor models' 
> `modelId`'s `id` field should change.**


## Model Types

* [Categorical distribution model](#Categorical_distribution_model): 
  returns psuedo-random values based on a designated
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

   

## Categorical distribution model

A Categorical distribution model returns psuedo-random values based on a designated
[Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution).  It can be used for collecting 
random data on which future models can be trained.  It's sampling is constant time, regardless of the number of 
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

`features` is an array of strings, where each string contains an aloha feature specification.  Each of these strings
should just contain a variable definition.  These features are used to create a hash on which the psuedo-randomness
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
selecting on of the associated *labels* in the at the same index in the 
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
array will be number.


#### (CD) missingOk

`missingOk` determines whether the model should return a value when at least one of the values in `features` 
could not be determined.  If `false`, no score will be returned.  If `true`, then  


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

A constant model always returns the same score value.

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

`returnBest` is a Boolean that when *true* let's the decision tree return a value associated with an 
[internal node](https://en.wikipedia.org/wiki/Tree_\(data_structure\)#Terminologies_used_in_Trees) when no 
further progress down the tree can be made.  

When set to false, the model returns an error rather than a score.

Usually, in a decision tree, when given a datum, a path to a leaf cannot be traversed, it indicates a problem.  
The most common being that the branching logic at a given node is not 
[exhaustive](https://en.wikipedia.org/wiki/Collectively_exhaustive_events).  When this happens, the tree algorithm 
checks if it can proceed to any of its children and the predicates associated with each child returns false.

#### (DT) missingDataOk

`missingDataOk` is a Boolean telling whether missing data is OK.  In the event that `missingDataOk` is set to *false*,
when a variable in a node selector is missing, the node selector should stop and report to the decision tree that it
can't make a decision on which child to traverse.  The subsequent behavior is dictated by [returnBest](#returnBest).

If `missingDataOk` is *true*, then when a node selector encounters missing data, it can still recover.  For instance, 
when a `linear` node selector encounters missing data in one of the predicate, it will assume the predicate is *false* 
and continue on the next the predicate.  This behavior may vary across node selectors of different types.

#### (DT) nodes

`nodes` contains the list of nodes in the decision tree.  As was mentioned above, trees are encoded in a tabular 
way to provide future extensibility to [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph)s.  The first node 
in the `nodes` array is considered to be the root node. The structure of a node is as follows: 

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

`id` is the node id and is used by [node selectors](#Node_Selectors) to determine which child to traverse.

##### (Node) value

`value` is the value to return when the decision tree algorithm selects a node whose value should be returned. 

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

`children` is a list of `node` `id` values.  This is array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `predicates` array.  If index *i* in `predicates` is the first predicate yielding *true*, then the value at 
index *i* in `children` will contain the *id* of the node that will be visited.

##### (linear ns) predicates 

The `predicates` array contains strings where each string is an aloha feature specification representing a Boolean 
function.  This is array is [parallel](https://en.wikipedia.org/wiki/Parallel_array) to the `children` array.  If 
index *i* in `predicates` is the first predicate yielding *true*, then the value at index *i* in `children` will 
contain the *id* of the node that will be visited.


#### Random Node Selector

A random node selector provides a way of psueudo-randomly splitting traffic between one or more outcomes.  This 
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

`children` is a list of `node` `id` values.  This is array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `probabilities` array.  Samples drawn from the 
[Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution) backed by `probabilities` yield an
index into the `children` array.  The `id` at the chosen index in the `children` array is used to select the child
to traverse. 

**Note**: There is a special case when the length of `children` can be one less than the length of the `probabilties`
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

What should not be done is compromise the ratios for readability.  For instance, if we want to encode probabilities 
of *1/3*, we should **NOT** encode them as:

```json
[ 0.33, 0.34, 0.33]
```

##### (random ns) features

`features` is an array of strings, where each string contains an aloha feature specification.  Each of these strings
should just contain a variable definition.  These features are used to create a hash on which the psuedo-randomness
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
string "*short*" when the height is less than 66 (*presumably inches*).  If height is greater then or equal 66, 
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
      "value": 1,
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
from Double values to Long values.  It works by applying the following tranformation: 

*v* = *scale* &times; *submodel value* + *translation*  
*v*&prime; = *IF round THEN* round(*v*) *ELSE* floor(*v*)  
*output* = max(*clampUpper*, min(*v*&prime;, *clampUpper*))

This model is useful for [eHarmony](http://www.eharmony.com)-specific purposes, but others may find it useful. 

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

`clampLower` is lower [clamp](https://en.wikipedia.org/wiki/Clamping_\(graphics\)) value.  It is a 64-bit long-valued
int.  When not supplied, the default value will be `-9,223,372,036,854,775,808`.

#### (DtL) clampUpper

`clampUpper` is upper [clamp](https://en.wikipedia.org/wiki/Clamping_\(graphics\)) value.  It is a 64-bit long-valued
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

A double-to-long model whose submodel is a constant model returning -13.  The outer model multiplies by *0.5*, adds *2*,
rounds, then takes the min of `clampUpper` (*8*) and *9*, which comes out to *8*.

```json
{
  "modelType": "DoubleToLong",
  "modelId": { "id": 0, "name": "" },
  "clampLower": 6,
  "clampUpper": 8,
  "scale": -0.5,
  "translation": 2,
  "rouind": true,
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
[SchrodingerException](https://github.com/eHarmony/aloha/blob/master/aloha-core/src/main/scala/com/eharmony/aloha/ex/SchrodingerException.scala),
an exception specifically designed to be as harmful as possible.  The implementation uses `scala.util.Try` which means
errors errors that are caught are dictated by 
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

Either way, the submodel is expected to be a model with the same input and output type as the Error-swalling model 
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
must either be a JSON object containing an Aloha model or it can be a JSON object with exactly one field,
`import` whose associated value is a JSON string containing an 
[Apache VFS](https://commons.apache.org/proper/commons-vfs/filesystems.html) URL.  For instance: 

```json
{ "import": "/path/to/aloha/model.json" }
```

The submodels input and output types are expected to be the same as the input and output type of the model decision 
tree model.  The model works by using the decision tree algorithm to select a node containing a model, and then it 
applies the same input data to the model it finds in the node. 

For more information, see the section on [Decision tree models](#Decision_tree_model).

## Regression model

The regression model inplementation in aloha is a [sparse](https://en.wikipedia.org/wiki/Sparse_matrix) 
[polynomial regression](https://en.wikipedia.org/wiki/Polynomial_regression) model.  This is a superset and therefore
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
#### (R) features
#### (R) weights
#### (R) higherOrderFeatures
#### (R) spline
#### (R) numMissingThreshold


### (R) JSON Examples


## Segmentation model

Description.

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
    <td><a href="#aS_subModel">subModel</a></td>
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
    <td><b>Output</b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_labels">labels</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

### (S) JSON Field Descriptions

#### (S) modelType
#### (S) subModel
#### (S) subModelOutputType
#### (S) thresholds
#### (S) labels

### (S) JSON Examples


## Vowpal Wabbit model

Description.

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
    <td><a href="#aS_features">features</a></td>
    <td>Object</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td><a href="#aS_vw">vw</a></td>
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
#### (VW) features
#### (VW) vw
#### (VW) namespaces
#### (VW) numMissingThreshold
#### (VW) notes
#### (VW) spline

### (VW) JSON Examples

