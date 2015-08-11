# Working with Other ML Libraries

## Why use Aloha?

Even when Aloha doesn't natively support the [*ML*](https://en.wikipedia.org/wiki/Machine_learning) algorithms you 
want to use, it can still be rather useful in feature extraction stage of your data pipeline.  We can easily produce 
datasets in a variety of formats including CSV, VW, and LibSVM and pipe them to your preferred *ML* tools.  This is a
similar approach to [hadoop streaming](http://hadoop.apache.org/docs/r1.2.1/streaming.html) in that Aloha allows the 
data to be piped to any shell script, which provides a great amount of flexibility.  Let Aloha do your data mapping
and the other libraries consume the data.

## Set up

Like with other examples, make sure:

* You're in the `aloha` root directory
* You've built Aloha (`mvn clean package`)

Also, a few of the following examples assume you have Apache 
[commons-httpclient](http://mvnrepository.com/artifact/commons-httpclient/commons-httpclient/3.1) in your local maven 
repository.

## [**R**](https://www.r-project.org)

The use of **R** across machine learning applications is pervasive, so not supporting **R** in any way would be 
ludicrous.  While we are considering adding more native **R** support to Aloha, it's not on the immediate roadmap.
In the meantime, you can use Aloha to create data to pipe to **R** scripts rather easily.  For instance: 

```bash
#!/usr/bin/env bash

(cat <<EOM
# BASH SCRIPT HERE!!!
EOM
) \
| \
R --slave --quiet --vanilla \
| \
grep -

```



## [scikit-learn](http://scikit-learn.org/stable/)

Working with [scikit-learn](http://scikit-learn.org/stable/) is easy.  Since it's just normal 
[python](https://www.python.org) code, you can create a python script, place a 
[shebang](https://en.wikipedia.org/wiki/Shebang_\(Unix\)) at the top of the script, presumably: 

```bash
#!/usr/bin/env python
```

Incorporating Aloha into a scikit-learn pipeline is rather easy.  For instance, Aloha doesn't currently have any 
mechanism for computing regularization paths.  So, one can use scikit-learn's implementation of the 
[LARS algorithm](https://en.wikipedia.org/wiki/Least-angle_regression) to compute regularization paths. Let's say
we wanted to compute and chart the regularization paths for the [Abalone](https://archive.ics.uci.edu/ml/datasets/Abalone) dataset. 
We could write a small python script that does this generically and use Aloha to create the dataset to be fed to 
the script.  

### scikit-learn Example

We use Aloha to get the [Abalone](https://archive.ics.uci.edu/ml/datasets/Abalone) dataset from the 
[UCI Machine Learning Repository](https://archive.ics.uci.edu/ml/) and then transform the features.  The Aloha output 
is then piped to scikit-learn for learning and visualization:

```bash
aloha-cli/bin/aloha-cli                                                                   \
  -cp $(find $PWD/aloha-cli/target -name "*dependenc*.jar" ):\
$(find $HOME/.m2/repository/commons-httpclient -name "commons-httpclient-*.jar")          \
  --dataset                                                                               \
  -i https://archive.ics.uci.edu/ml/machine-learning-databases/abalone/abalone.data       \
  -c $PWD/aloha-core/src/test/resources/com/eharmony/aloha/dataset/cli/abalone_types.json \
  -s $PWD/aloha-core/src/test/resources/com/eharmony/aloha/dataset/cli/abalone_spec.json  \
  --csv-headers                                                                           \
  --csv -                                                                                 \
|                                                                                         \
lars.py
```

And the following appears: 

![LARS Abalone Example](images/lars.png)

This assumes a file `lars.py` that we've written.  It might look something like the following (which is a modification
of the [Lasso path using LARS](http://scikit-learn.org/stable/auto_examples/linear_model/plot_lasso_lars.html) example
on the scikit-learn site): 

```python
#!/usr/bin/env python

# ================================================================================
# Expects CSV data on STDIN where the first column contains the dependent variable 
# and the first row is a header row containing the variable names.
# ================================================================================

import sys
import numpy as np
import matplotlib.pyplot as plt

from sklearn import linear_model
from sklearn import datasets

headerString = sys.stdin.readline().strip()
headers = headerString.split(",")
cols = len(headers)

# Remove the dependent variable in the 1st column.
headers.pop(0)

raw_data = sys.stdin
dataset = np.loadtxt(raw_data, delimiter=",")

# separate the data from the target attributes
X = dataset[:,1:cols]
y = dataset[:,0]

alphas, _, coefs = linear_model.lars_path(X, y, method='lasso', verbose=True)
xx = np.sum(np.abs(coefs.T), axis=1)
xx /= xx[-1]

plt.plot(xx, coefs.T)
ymin, ymax = plt.ylim()
plt.legend(headers, loc="lower left")
plt.vlines(xx, ymin, ymax, linestyle='dashed')
plt.xlabel('|coef| / max|coef|')
plt.ylabel('Coefficients')
plt.title('LASSO Path')
plt.axis('tight')
plt.show()
```
