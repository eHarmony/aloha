## Method 1

*[F-bounded polymorphism](https://ktoso.github.io/scala-types-of-types/#f-bounded-type),
[path-dependent types](https://ktoso.github.io/scala-types-of-types/#path-dependent-type)*.

In this method, we have `Auditor`s and `MorphableAuditor`s.  Auditors are
inputs to models and are responsible for producing the
[audit trail](https://en.wikipedia.org/wiki/Audit_trail) that is returned as the
output of a model's prediction function.  Morphable auditors are auditors that
can produce new auditors with the same structure but with different type
parameters.

```scala
trait Auditor[K, A, +B] {
  type AuditOutput[_]                    // B = AuditOutput[A]
  private[aloha] def failure[S](...): B  // model fails to predict
  private[aloha] def success[S](...): B  // model successfully predicts
}

// Notice Impl is a self type describing the implementation.
trait MorphableAuditor[K, A, +B, Impl <: MorphableAuditor[K, A, B, Impl]]
  extends Auditor[K, A, B] {
  def auditor[S: RefInfo]: Option[Auditor[K, S, Impl#AuditOutput[S]]]
}
```

Models are just functions.  All `Model` instances also must also extends a more decorated interface called `AuditedModel`, which contains an `Auditor`.  Notice
audited models have a "*natural output type*" `N` and an actual output `B` where the relationship between the two types is `B = auditor.AuditOutput[N]`.

```scala
trait Model[-A, +B] extends (A => B) { self: AuditedModel[A, _, B] => }

trait AuditedModel[-A, N, +B] extends Model[A, B] {
  def auditor: Auditor[ModelIdentity, N, B]
}
```

So a constant model will have three type parameters: `A, `N`, and `B`, with the normal relationship between `N`, and `B`.  This works fine but we have an extra type parameter.

```scala
case class ConstantModel[-A, N, +B](
  modelId: ModelIdentity,
  constant: N,
  auditor: Auditor[ModelIdentity, N, B])
extends AuditedModel[A, N, B] {
  def apply(a: A): B = auditor.success(modelId, constant)
}
```


### Pro: Works well from Java

This method works much better when instantiating model factories and directly instantiating models in Java.  For instance:


#### Prerequisite code

```java
@SuppressWarnings("unchecked")
import deaktator.reflect.runtime.manifest.ManifestParser;

private static <A> Manifest<A> manifest(final String strRep) {
    return (Manifest<A>) ManifestParser.parse(strRep).right().get();
}
```


#### Auditor Creation
```java
Manifest<Integer> refInfoInt =
    manifest(Integer.class.getCanonicalName());
OptionAuditor<ModelIdentity, Integer> aud =
    new OptionAuditor<>(refInfoInt);
```


#### ConstantModel Creation
```java
// Using the diamond operator and extracting to a local variable
// in an IDE would produce the type on the LHS automatically.
ConstantModel<Object, Integer, Option<Integer>> constModel =
    new ConstantModel<>(ModelId.empty(), 1, aud);
```


#### Seamless Downcasting
```java
// Downcasting works without issue because of the type signatures.
Model<Object, Option<Integer>> model = constModel;
```


#### Creation from factory works without type coercion

```java
private static Model<Object, Option<Float>> getModel(
    final Float constant
) throws JavaModelFactoryException {
    Manifest<Float> refInfo = manifest("java.lang.Float");
    Manifest<Object> refInfoObj = manifest("java.lang.Object");
    OptionAuditor<ModelIdentity, Float> aud =
        new OptionAuditor<ModelIdentity, Float>(refInfo);
    JavaModelFactory factory =
        new JavaModelFactory(new StdModelFactory());
    Semantics<Object> semantics =
        new Semantics<Object>(refInfoObj);
    ModelId modelId = new ModelId(1, "test");
    return factory.createConstantModel(semantics, aud, modelId, constant);
}
```


### Con: Extraneous type parameters
As demonstrated, this works nicely from Java but you'll notice that the since
there is a direct relationship between the `N` and `B` type parameters, the
`B` parameter is redundant (at least in Scala).


### Con: path-dependent types are required

The constant model instantiation works just fine, but when trying to create a
model that takes submodels as parameters, things become much more hairy.
That's because the *exact same* type constructor is required in the model and the submodels inside the model. This makes path-dependent types a natural fit
but that makes model creation from Scala more difficult.  This means that a models necessarily look like:

```scala
trait ModelWithSubmodel[-A, SN, N, +B] {
  val auditor: Auditor[ModelIdentity, N, B]
  def sub: Model[A, auditor.AuditOutput[SN]]
}
```

where `SN` is the submodel's "*natural output type*".  Notice that the
submodel's output type **depends on** the `auditor` *value*.  The downside of
this is that we can't just pass a submodel to *case class* constructor. We have
to have a factory method with two parameter lists that uses
[type refinements](https://ktoso.github.io/scala-types-of-types/#refined-types-refinements)
to construct its instances.  This is required because `auditor` needs to have a
singleton type in the refined type.  That's all just so that we can ensure the submodel has the same type constructor as the `auditor`.

Concretely, this looks like (*which is pretty ugly and unwieldy*):

```scala
abstract class HierarchicalConstantModel[-A, SN, N, +B](
    override val modelId: ModelIdentity,
    constant: N
) extends AuditedModel[A, N, B] {
  val auditor: Auditor[ModelIdentity, N, B]
  def sub: Model[A, auditor.AuditOutput[SN]]
  def apply(a: A): B = auditor.success(modelId, constant, subValues = Seq(sub(a)))
}

object HierarchicalConstantModel {
  def apply[A, SN, N, B](
      mId: ModelIdentity,
      v: N,
      aud: Auditor[ModelIdentity, N, B])(
      s: Model[A, aud.AuditOutput[SN]] // to refer to aud, must be in list #2
  ): HierarchicalConstantModel[A, SN, N, B] = {
    new HierarchicalConstantModel[A, SN, N, B](mId, v) {
      val auditor: aud.type = aud
      val sub: Model[A, auditor.AuditOutput[SN]] = s
    }
  }
}
```

## Method 2

*Wrapped
[type constructors](https://ktoso.github.io/scala-types-of-types/#type-constructor-span-style-color-red-span),
models w/ type constructors in output type*.

In this method, the type constructor `AuditOutput` is removed from `Auditor`
and is in its own type: `TypeCtor`.  `TypeCtor` has a self type of `Singleton`
which means that only `object`s can extend `TypeCtor`.  In this method
`Auditor`, `Model` implementations, and factories take a type parameter `T`
that is a subtype of `TypeCtor`.  `TypeCtor` also implements the
[*Aux* pattern from Shapeless](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/generic.scala#L148).
This looks like:

```scala
trait TypeCtor { self: Singleton =>
  type TC[+A]
  def refInfo[A: RefInfo]: RefInfo[TC[A]]
}

object TypeCtor {
  type Aux[C[+_]] = TypeCtor { type TC[A] = C[A] }
}
```

An example of a type constructor for `Option`s looks like:

```scala
object OptionTC extends TypeCtor {
  type TC[+A] = Option[A]
  def refInfo[A: RefInfo] = RefInfo[Option[A]]
  def instance: this.type = this // added for easy of use in Java
}
```

A constant model looks like:

```scala
case class ConstantModel[T <: TypeCtor, -A, N](
    modelId: ModelIdentity,
    constant: N,
    tc: T,
    auditor: Auditor[ModelIdentity, T, N]) extends Model[A, T#TC[N]] {
  def apply(a: A): T#TC[N] = auditor.success(tc, modelId, constant)
}
```

Notice that there's no `B` type parameter and that the output type of the model
is `T#TC[N]`. Here we're using
[type projection](https://ktoso.github.io/scala-types-of-types/#type-projection)
rather than path dependent types as in Method 1.  This doesn't seem like a big
gain but when write model implementations with submodels, it bears fruit, for
instance:

```scala
case class HierarchicalConstantModel[T <: TypeCtor, -A, +SN, N](
    modelId: ModelIdentity,
    constant: N,
    sub: Model[A, T#TC[SN]],
    tc: T,
    auditor: Auditor[ModelIdentity, T, N]
) extends Model[A, T#TC[N]] {
  def apply(a: A): T#TC[N] =
    auditor.success(tc, modelId, constant, subValues = Seq(sa))
}

object HierarchicalConstantModel {
  def apply[T <: TypeCtor, A, SN, N](
      modelId: ModelIdentity,
      constant: N,
      tc: T,
      auditor: Auditor[ModelIdentity, T, N])(
      sub: Model[A, T#TC[SN]]
  ): HierarchicalConstantModel[T, A, SN, N] = {
    new HierarchicalConstantModel[T, A, SN, N](modelId, constant, sub, tc, auditor)
  }
}
```

### Pro: No path-dependent types

### Pro: Writing Model Impls is *easier* and instantiating is easy

```scala
val cm = ConstantModel(ModelId(), 2f, OptionTC, OptionAuditor[Float])
val hcm = HierarchicalConstantModel(ModelId(), 1, OptionTC, OptionAuditor[Int])(cm)
require(Option(1) == hcm(None))
```

### Con: Directly instantiating models Java is sub-par

See `com.eharmony.aloha.score.audit.take2.JavaFactoryTest.test()` for details.

### Pro: Model instantiation from factories works well
*Unfortunately, it requires reflection via `RefInfo` to coerce the model output
types*.


## Method 3 (not yet done)

*A hybrid of Method 1 and 2: Use extraneous type parameters and `TypeCtor` and
parameterize models, auditors, and factory by `TypeCtor` type.*