package io.popbrain.sdk.a.api

interface WorkerCall<R> {

    fun execute(): R?

    fun enqueue(callBack: WorkerCallback<R>)

    fun cancel()
}