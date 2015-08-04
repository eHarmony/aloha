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
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>features</td>
    <td>Array</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>probabilities</td>
    <td>Array</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>labels</td>
    <td>Array</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>missingOk</td>
    <td>Boolean</td>
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

### JSON Fields



<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Type</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
    <td>String</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>value</td>
    <td>?</td>
    <td>true</td>
    <td>N / A</td>
  </tr>
</table> 


## Decision tree model

asdf


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


