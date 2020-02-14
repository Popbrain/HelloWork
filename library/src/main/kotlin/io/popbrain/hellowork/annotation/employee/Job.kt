package io.popbrain.hellowork.annotation.employee

//@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Job(val value: String = "")