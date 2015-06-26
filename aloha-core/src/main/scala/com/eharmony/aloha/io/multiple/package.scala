package com.eharmony.aloha.io

/** Provides a way of parsing multiple resources in bulk so that the factory object can be thrown away once all
  * resources are parsed and converted to objects.
  *
  * This concept is especially useful to Spring because factories can be
  * [http://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/html/beans.html#beans-factory-scopes-prototype prototype scoped].
  * Model factories with prototype scoping can be constructed once per usage and then thrown away, so
  * limiting the number of calls to the factory is desirable.  Therefore, bulk calls are desirable.  Once the factory is
  * no longer needed, it will be garbage collected which may free up large amounts of memory.
  */
package object multiple {}
