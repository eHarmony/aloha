# Model Formats

## Model Types

* [Categorical distribution model](#Categorical_distribution_model): returns psuedo-random values based on a designated
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
* [Segmentation model](#Segmentation_model) vanilla [segmentation](https://en.wikipedia.org/wiki/Market_segmentation) 
  model based on linear search in an [interval](https://en.wikipedia.org/wiki/Interval_(mathematics)) space
* [Vowpal Wabbit model](#Vowpal_Wabbit_model) model exposing [VW](https://github.com/JohnLangford/vowpal_wabbit/wiki) 
  through its [JNI](https://github.com/JohnLangford/vowpal_wabbit/tree/master/java) wrapper

## Categorical distribution model


<table>
  <tr>
    <th>Field Name</th>
    <th>JSON Value Type</th>
    <th>Required</th>
    <th>Default</th>
  </tr>
  <tr>
    <td>modelType</td>
    <td>String</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>modelId</td>
    <td>Object</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>features</td>
    <td>Array</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>labels</td>
    <td>Array</td>
    <td>false</td>
    <td>N / A</td>
  </tr>
  <tr>
    <td>missingOk</td>
    <td>Boolean</td>
    <td>true</td>
    <td>false</td>
  </tr>
</table> 


```json
{
  "modelType": "CategoricalDistribution",
  "modelId": { "id": 0, "name": "" },
  "features": [ "${profile.id}", "${calculated_values.days_since_epoch}" ],
  "labels": [1, 2, 3, 4],
  "missingOk": false
}
```

## Constant model

asdf


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


