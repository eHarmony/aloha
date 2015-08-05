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


### JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
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
    <td>features</td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>probabilities</td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>labels</td>
    <td>Array</td>
    <td><b>OUTPUT</b></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>missingOk</td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>false</td>
    <td>false</td>
  </tr>
</table> 


### JSON Field Descriptions


#### modelType

`modelType` field must be `CategoricalDistribution`.


#### features 

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


####  probabilities

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

#### labels

`labels` are the actual values returned when a value is sampled from the distribution.  The *type of value* in 
`labels` is dependent on the model's output type.  For instance, if the model output type is a string, then the 
JSON value type in `labels` will be  strings.  If the output type is a 64-bit float, the JSON type in the `labels` 
array will be number.


#### missingOk

`missingOk` determines whether the model should return a value when at least one of the values in `features` 
could not be determined.  If `false`, no score will be returned.  If `true`, then  


### JSON Examples

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

### JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
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
    <td>value</td>
    <td><b>OUTPUT</b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

### JSON Field Descriptions


#### modelType

`modelType` field must be `Constant`.

#### value

`value` field's *type* is dependent on the model's output type.

### JSON Examples

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

### JSON Fields

<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>JSON Subtype</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
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
    <td>returnBest</td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>missingDataOk</td>
    <td>Boolean</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>nodes</td>
    <td>Array</td>
    <td><b>Node<sup>*</sup></b></td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 


### JSON Field Descriptions


#### modelType

`modelType` field must be `DecisionTree`.

#### returnBest

`returnBest` is a Boolean that when *true* let's the decision tree return a value associated with an 
[internal node](https://en.wikipedia.org/wiki/Tree_\(data_structure\)#Terminologies_used_in_Trees) when no 
further progress down the tree can be made.  

When set to false, the model returns an error rather than a score.

Usually, in a decision tree, when given a datum, a path to a leaf cannot be traversed, it indicates a problem.  
The most common being that the branching logic at a given node is not 
[exhaustive](https://en.wikipedia.org/wiki/Collectively_exhaustive_events).  When this happens, the tree algorithm 
checks if it can proceed to any of its children and the predicates associated with each child returns false.

#### missingDataOk

`missingDataOk` is a Boolean telling whether missing data is OK.  In the event that `missingDataOk` is set to *false*,
when a variable in a node selector is missing, the node selector should stop and report to the decision tree that it
can't make a decision on which child to traverse.  The subsequent behavior is dictated by [returnBest](#returnBest).

If `missingDataOk` is *true*, then when a node selector encounters missing data, it can still recover.  For instance, 
when a `linear` node selector encounters missing data in one of the predicate, it will assume the predicate is *false* 
and continue on the next the predicate.  This behavior may vary across node selectors of different types.

#### nodes

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
    <td>id</td>
    <td>Number</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>value</td>
    <td><b>OUTPUT</b></td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>selector</td>
    <td><b>Node Selector<sup>*</sup></b></td>
    <td>N / A</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
</table>

##### id

`id` is the node id and is used by [node selectors](#Node_Selectors) to determine which child to traverse.

##### value

`value` is the value to return when the decision tree algorithm selects a node whose value should be returned. 

##### selector

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
    <td>selectorType</td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>children</td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>predicates</td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 

##### selectorType

`selectorType` must be `linear`.

##### children

`children` is a list of `node` `id` values.  This is array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `predicates` array.  If index *i* in `predicates` is the first predicate yielding *true*, then the value at 
index *i* in `children` will contain the *id* of the node that will be visited.

##### predicates 

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
    <td>selectorType</td>
    <td>String</td>
    <td>N / A</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>children</td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>probabilities</td>
    <td>Array</td>
    <td>Number</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>features</td>
    <td>Array</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table>

##### selectorType

`selectorType` must be `random`.

##### children

`children` is a list of `node` `id` values.  This is array is [parallel](https://en.wikipedia.org/wiki/Parallel_array)
to the `probabilities` array.  Samples drawn from the 
[Categorical distribution](https://en.wikipedia.org/wiki/Categorical_distribution) backed by `probabilities` yield an
index into the `children` array.  The `id` at the chosen index in the `children` array is used to select the child
to traverse. 

**Note**: There is a special case when the length of `children` can be one less than the length of the `probabilties`
array.  In such a case, if `returnBest` is set to *true* then when the sampling from the categorical distribution 
selects last index in `probabilities`, the *current node* (and not a child node) is selected.  See JSON examples
for how this is useful.

##### probabilities

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

##### features

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


### JSON Examples

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

asdf


## Error model

asdf


## Error-swallowing model

asdf


## Model decision tree model

asdf


## Regression model

asdf


## Segmentation model

asdf


## Vowpal Wabbit model

asdf


