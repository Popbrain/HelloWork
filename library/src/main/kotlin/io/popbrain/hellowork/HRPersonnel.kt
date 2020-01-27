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

import io.popbrain.hellowork.exception.SuspendHelloWorkException
import java.util.concurrent.ExecutorService

/**
 * HRPersonnel has a role for finding workers from
 * target job and let it execute.
 *
 */
class HRPersonnel<R>(
    private val workerJob: WorkerJob<R, *>,
    private val args: Array<out Any>? = null
) : Efforter<R>(), FrontDesk<R> {
    private var executorService: ExecutorService? = null

    fun executorService(executorService: ExecutorService?): HRPersonnel<R> {
        this.executorService = executorService
        return this
    }

    @Volatile
    private var isCanceled = false

    override fun execute(): Effort<R> {

        if (isCanceled) {
            return createErrorEffort(Status.CANCELED,"Canceled")
        }
        try {
            args?.let {
                val result = workerJob.execute(*it) as R
                return createSuccessEffort(result, "Complete a task.")
            }
            val result = workerJob.execute() as R
            return createSuccessEffort(result, "Complete a task.")
        } catch (e: SuspendHelloWorkException) {
            return createErrorEffort(e.error,"", e)
        } catch (e: Exception) {
            return createErrorEffort(Status.Error.OTHER,"Failed", e)
        }
    }

    override fun enqueue(callback: Callback<R>) {
        enqueue {
            val effort = execute()
            if (!effort.isError) {
                callback.onResponse(this, effort)
            } else {
                callback.onFailure(effort, effort.error)
            }
        }
    }

    override fun cancel() {
        this.isCanceled = true
    }

    override fun clone(): FrontDesk<R> {
        return HRPersonnel(workerJob, args)
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