package io.popbrain.sdk.a.api

import io.popbrain.hellowork.Effort

interface WorkerCallback<R> {

    fun onResponse(response: Effort<R>)

    fun onFailure(response: Effort<R>, error: Throwable?)
}