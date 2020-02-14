/**
 * Copyright (C) 2020 Popbrain aka Garhira.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.popbrain.hellowork

import io.popbrain.hellowork.util.equalRawType
import io.popbrain.hellowork.util.getFrontDeskResponseType
import java.lang.reflect.Type
import java.util.concurrent.Executor

class FrontDeskAdapterFactory(private val executor: Executor): FrontDeskAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, helloWork: HelloWork): FrontDeskAdapter<*, *>? {
        if (!returnType.equalRawType<FrontDesk<*>>()) return null
        val responseType = returnType.getFrontDeskResponseType()
        return object: FrontDeskAdapter<Any, FrontDesk<*>> {
            override fun responseType(): Type = responseType
            override fun liaison(frontDesk: FrontDesk<Any>): FrontDesk<*> = FrontDeskCaller(executor, frontDesk)
        }
    }

    private class FrontDeskCaller<R>(private val executor: Executor,
                                     private val frontDesk: FrontDesk<R>): FrontDesk<R> {
        private var isCancel = false
        override fun execute(): Effort<R> = frontDesk.execute()

        override fun enqueue(callback: Callback<R>?) {
            frontDesk.enqueue(object: Callback<R> {
                override fun onResponse(call: FrontDesk<R>?, res: Effort<R>) {
                    this@FrontDeskCaller.executor.execute {
                        if (isCancel) {
                            callback?.onFailure(res)
                        } else {
                            callback?.onResponse(this@FrontDeskCaller, res)
                        }
                    }
                }

                override fun onFailure(res: Effort<R>, t: Throwable?) {
                    this@FrontDeskCaller.executor.execute {
                        callback?.onFailure(res, t)
                    }
                }
            })
        }

        override fun cancel() {
            this.isCancel = true
        }

        override fun clone(): FrontDesk<R> = FrontDeskCaller(executor, frontDesk)
    }
}