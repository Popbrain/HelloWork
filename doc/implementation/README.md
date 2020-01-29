## Implementation

### Example


```
- Target App
   ┗ Base library (Parent module)
   ┗ Sub library (Child module)
```

---
### Before implement HelloWork

In case of base library uses sub library's function. (And the base library works by itself without sub library)

 **Child module's function**
```kotlin
package io.popbrain.sample.module.a

class Child {

    fun doMultiply(a: Int, b: Int): Int {
        val ans = a*b
        System.out.println("""$a * $b = $ans""")
        return ans
    }
}
```

**Parent module**
```kotlin
package io.popbrain.sample.base

import io.popbrain.sample.module.a.Child

class Parent {

    fun math() {
        try {
          Class.forName("io.popbrain.sample.module.a.Child")
          val ans = Child().doMultiply(2, 5)
        } catch (e: Exception) {
          System.out.println("Child library is not imported.")
        }
    }
}
```
**build.gradle of parent module**
```gradle
dependencies {
    implementation(project(":child"))
}
```
---
### Implemented HelloWork

**Child module's function**
```kotlin
package io.popbrain.sample.module.a

import io.popbrain.hellowork.annotation.employee.Job;
import io.popbrain.hellowork.annotation.employee.Worker;

@Worker
class Child {

    @Job("multiply")
    fun doMultiply(a: Int, b: Int): Int {
        val ans = a * b
        System.out.println("""$a * $b = $ans""")
        return ans
    }
}
```
> **@Worker** : Register Child class to HelloWork as the worker.
>
> **@Job** : Define worker's job. Argument is a job name.

**Parent module**

The definition of Employer class.
```kotlin
package io.popbrain.sample.base

@HelloWork("io.popbrain.sample.module")
interface CalculatorOffer: Job {

    @JobOffer("multiply")
    fun multiply(a: Int, b: Int): FrontDesk<Int>
}
```

> **@HelloWork** : Register CalculatorOffer class to HelloWork as the employer. The argument is the package to search for worker classes. It can input full path of worker class directly to argument.
>  
> **@JobOffer** : JobOffer is an annotation to ask a worker for work. An argument of JobOffer is a job name. The arguments of the defined method(multiply) must be the same as the arguments on the worker's method.
>
> **FrontDesk<Int>** : FrontDesk is the return value for requesting work. Generics type(Int) is result type from worker's. This type must be same with the return type of a worker.


Execute find for workers
```kotlin
  HelloWork.Bureau(CalculatorOffer::class.java)
           .entry()
           .execute()
```

> ※ This process is preferably executed at the timing when the parent module is initialized.<br>
> `Bureau` will looking for matching workers according to the package definition of HelloWork annotation in 'CalculatorOffer'. And, It can execute by asynchronously in case of call `enqueue` method.<br>
> Bureau will cached target workers after found them.


Execute call a worker.
```kotlin
package io.popbrain.sample.base

class Parent {

    fun math() {
         val ans = HelloWork.Dispatcher().build()
                            .callWorker(CalculatorOffer::class.java)
                            .multiply(2, 5)
                            .execute()
         System.out.println("Answer : $ans")
    }
}

> 2 * 5 = 10
> Answer : 10
```

> `Dispatcher` is caller to execute worker's job. Dispatcher can also execute a job by asynchronously in case of call `enqueue` method.
