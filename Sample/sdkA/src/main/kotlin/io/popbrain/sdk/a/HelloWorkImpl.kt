package io.popbrain.sdk.a

import io.popbrain.hellowork.HelloWork
import io.popbrain.hellowork.Job
import io.popbrain.sdk.a.api.WorkerResultHandlingAdapter

class HelloWorkImpl {

    companion object {

        fun <T: Job> build(job: Class<T>): T {
            return HelloWork.Dispatcher()
                .addFrontDeskAdapterFactory(WorkerResultHandlingAdapter.AdapterFactory())
                .build()
                .callWorker(job)
        }

    }

}