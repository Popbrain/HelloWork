package io.popbrain.sdk.a.api

import io.popbrain.hellowork.Callback
import io.popbrain.hellowork.Effort
import io.popbrain.hellowork.FrontDesk
import io.popbrain.hellowork.FrontDeskAdapter
import io.popbrain.hellowork.HelloWork
import io.popbrain.hellowork.util.getParameterUpperBound
import io.popbrain.hellowork.util.rawType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor

/**
 * Customized FontDeskAdapter.Factory
 */
class WorkerResultHandlingAdapter {

    class AdapterFactory: FrontDeskAdapter.Factory() {

        override fun get(returnType: Type, annotations: Array<Annotation>, helloWork: HelloWork): FrontDeskAdapter<*, *>? {
            if (returnType.rawType() != WorkerCall::class.java) {
                throw IllegalStateException("""Returns type must be WorkerCall<>.""")
            }
            if (returnType !is ParameterizedType) throw IllegalStateException("""WorkerCall must have generic type (e.g., WorkerCall<Effort<*>>)""")
            val responseType = returnType.getParameterUpperBound(0)
            val callbackExecutor = helloWork.callbackExecutor
            return ResultHandlingAdapter<Any>(responseType, callbackExecutor)
        }

        private class ResultHandlingAdapter<T> internal constructor(private val responseType: Type,
                                                                    private val callbackExecutor: Executor): FrontDeskAdapter<T, WorkerCall<T>> {
            override fun responseType(): Type = responseType
            override fun liaison(frontDesk: FrontDesk<T>): WorkerCall<T> = WorkerCallAdapter(frontDesk, callbackExecutor)
        }
    }

    /**
     * @param frontDesk : FrontDesk
     * @param callbackExecutor : Executor
     */
    internal class WorkerCallAdapter<R>(private val frontDesk: FrontDesk<R>,
                                        private val callbackExecutor: Executor): WorkerCall<R> {
        override fun execute(): R? = frontDesk.execute().get()

        override fun enqueue(callBack: WorkerCallback<R>) {
            frontDesk.enqueue(object: Callback<R> {
                override fun onResponse(call: FrontDesk<R>?, res: Effort<R>) {
                    // mainThread Executor
                    callbackExecutor.execute {
                        callBack.onResponse(res)
                    }
                }

                override fun onFailure(res: Effort<R>, t: Throwable?) {
                    callbackExecutor.execute {
                        callBack.onFailure(res, t)
                    }
                }
            })
        }

        override fun cancel() {
            frontDesk.cancel()
        }
    }

}