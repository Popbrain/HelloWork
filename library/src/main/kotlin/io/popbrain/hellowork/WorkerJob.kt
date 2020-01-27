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

import io.popbrain.hellowork.annotation.employer.JobOffer
import io.popbrain.hellowork.exception.SuspendHelloWorkException
import io.popbrain.hellowork.util.hasEqualParams
import io.popbrain.hellowork.util.hasUnresolvableType
import io.popbrain.hellowork.util.printNameAndArgs
import java.lang.reflect.Method
import java.lang.reflect.Type

/**
 * WorkerJob is identify workers in waiting room by a job name.
 *
 */
class WorkerJob<R, out T>(builder: Builder<R, out T>) {

    private val frontDeskAdapter: FrontDeskAdapter<R, out T>
    private val job: Method
    private val jobResourcesTypes: Array<Type>
    private val effortType: Type
    private val targetWorker: Worker

    init {
        this.frontDeskAdapter = builder.getStaffAdapter()
        this.job = builder.getJob()
        this.jobResourcesTypes = builder.getJobResourcesTypes()
        this.effortType = builder.getEffortType()
        this.targetWorker = builder.get()
    }

    fun liaisonWith(frontDesk: FrontDesk<R>): T = frontDeskAdapter.liaison(frontDesk)

    /**
     * Execute a job of the identified a worker.
     */
    fun execute(vararg args: Any): R? {
        val targetClass = targetWorker.lastName
        val targetMethod = targetWorker.firstName
        try {
            targetMethod.isAccessible = true
            return targetMethod.invoke(targetClass.newInstance(), *args) as R
        } catch (e: Exception) {
            val argStr = StringBuilder("Sets args : ")
            for(i in 0..args.size-1) {
                if (i == 0) argStr.append(args[i]) else argStr.append(", ").append(args[i])
            }
            throw SuspendHelloWorkException(Status.Error.WORKERS_TROUBLE, """Could not invoke "${targetClass.canonicalName}.${targetMethod.name}"\n${argStr.toString()} """, e)
        }
    }

    class Builder<R, T>(val helloWork: HelloWork, val jobOffer: Class<*>) {

        private lateinit var requestJob: Method
        private lateinit var jobName: String
        private lateinit var jobResourcesTypes: Array<Type>
        private lateinit var frontDeskAdapter: FrontDeskAdapter<R, T>
        private lateinit var effortType: Type
        private lateinit var worker: Worker

        fun offer(requestjob: Method): Builder<R, T> {
            this.requestJob = requestjob
            this.jobName = getJobName(requestJob)
            this.jobResourcesTypes = requestjob.genericParameterTypes
            return this
        }

        fun build(): WorkerJob<R, T> {
            this.frontDeskAdapter = createStaffAdapter()
            this.effortType = frontDeskAdapter.responseType()
            this.worker = callWorker()

            val workerMethod = worker.firstName
            if (!requestJob.hasEqualParams(workerMethod)) {
                throw SuspendHelloWorkException(Status.Error.FATAL, """Method ${requestJob.printNameAndArgs()}'s args type are different from the method ${worker.firstName.printNameAndArgs()}'s args.""")
            }
            if (requestJob.genericReturnType.hasUnresolvableType()) {
                throw SuspendHelloWorkException(Status.Error.FATAL, """Method ${requestJob.name}'s return type must not include a type variable or wildcard.""")
            }
            if (workerMethod.genericReturnType.hasUnresolvableType()) {
                throw SuspendHelloWorkException(Status.Error.FATAL, """Method ${workerMethod.name}'s return type must not include a type variable or wildcard.""")
            }

            return WorkerJob(this)
        }

        fun getJob() = this.requestJob
        fun getJobResourcesTypes() = this.jobResourcesTypes
        fun getStaffAdapter() = this.frontDeskAdapter
        fun getEffortType() = this.effortType
        fun get() = this.worker

        private fun getJobName(job: Method): String {
            val jobOfferAnnos = job.getAnnotationsByType(JobOffer::class.java)
            if (jobOfferAnnos.size == 0) {
                throw SuspendHelloWorkException(Status.Error.FATAL, "A JobOffer annotation is not defined.")
            }
            val jobName = jobOfferAnnos[0].value
            if (jobName.isEmpty()) throw SuspendHelloWorkException(Status.Error.FATAL, """${job.name}'s jobName is blank!!""")
            return jobName
        }

        private fun createStaffAdapter(): FrontDeskAdapter<R, T> {
            val returnType = requestJob.genericReturnType
            if (returnType.typeName.equals("void")) {
                System.out.println("""Return type is void.""")
            }
            if (returnType == Void::class.java) {
                throw Exception("""Returns type must not be "Void.class". """)
            }
            return helloWork.frontDeskAdapter(returnType, requestJob.annotations) as FrontDeskAdapter<R, T>
        }

        private fun callWorker(): Worker {
            HelloWork.WaitingRoom.callWorker(jobName)?.let {
                return it
            }
            throw SuspendHelloWorkException(Status.Error.FATAL, """Worker's @Job("$jobName") is not found. Make sure both job names(@JobOffer, @Job) and 'HelloWork.Bureau.entry()' is called before 'HelloWork.callWorker()'.""")
        }
    }
}