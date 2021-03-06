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
package io.popbrain.hellowork.bureau

import io.popbrain.hellowork.HelloWork
import io.popbrain.hellowork.annotation.employee.Job
import io.popbrain.hellowork.util.Log

class WorkerSorting: RecruitmentAgency.CallReponseHandler {

    override fun isTarget(classPackage: String, resultClass: Class<*>): Boolean {
        resultClass.declaredMethods.forEach { method ->
            method.annotations.forEach {
                if (it is Job) {
                    if (!HelloWork.WaitingRoom.isExistWorker(it.value)) {
                        Log.out.v("Found worker's job : ${it.value}")
                        HelloWork.WaitingRoom.passingWorker(it.value, io.popbrain.hellowork.Worker(resultClass, method))
                    } else {
//                    throw SuspendHelloWorkException(Status.Error.WORKERS_TROUBLE, """JobName "${it.value}" is duplicated!! Please check multiple definition.""")
                        Log.out.e("""JobName "${it.value}" is duplicated!! Please check multiple definition.""")
                    }
                }
            }
        }
        return true
    }
}