package io.popbrain.hellowork.annotation.employee

import java.lang.annotation.ElementType
import java.lang.annotation.Target

//@MustBeDocumented
@Target(ElementType.METHOD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Job(val value: String = "")