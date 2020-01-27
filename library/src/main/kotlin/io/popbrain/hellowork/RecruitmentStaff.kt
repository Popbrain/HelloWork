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

import io.popbrain.hellowork.bureau.RecruitmentAgency
import io.popbrain.hellowork.exception.SuspendHelloWorkException
import io.popbrain.hellowork.util.Log
import java.util.concurrent.ExecutorService

class RecruitmentStaff(private val agency: RecruitmentAgency,
                       private val executorService: ExecutorService? = null)
    : Efforter<Array<String>>(), FrontDesk<Array<String>> {

    @Volatile
    private var isCanceled = false

    override fun execute(): Effort<Array<String>> {
        if (isCanceled) return createErrorEffort(Status.CANCELED)
        try {
            val result = agency.execute()
            result?.let {
                HelloWork.WaitingRoom.saveWorkerList(it)
            }
            return createSuccessEffort(result, """Completed find worker.""")
        } catch (e: SuspendHelloWorkException) {
            Log.out.e("Failed find a worker because occurred any error.", e)
            return createErrorEffort(e.error, "", e)
        } catch (e: Exception) {
            return createErrorEffort(Status.Error.OTHER, "", e)
        }
    }

    override fun enqueue(callback: Callback<Array<String>>) {
        var executor = Env.instance().defaultCallbackExecutor()
        enqueue {
            val effort = execute()
            executor.execute {
                if (!effort.isError) {
                    callback.onResponse(this, effort)
                } else {
                    callback.onFailure(effort, effort.error)
                }
            }
        }
    }

    override fun cancel() {
        this.isCanceled = true
    }

    override fun clone(): FrontDesk<Array<String>> {
        return RecruitmentStaff(agency, executorService)
    }

    private fun enqueue(onExecute: () -> Unit) {
        this.executorService?.let {
            it.execute(onExecute)
            return
        }
        Env.instance()
            .defaultThreadPool
            .execute(onExecute)
    }
}