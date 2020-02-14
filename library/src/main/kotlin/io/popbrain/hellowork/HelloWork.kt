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

import android.content.Context
import io.popbrain.hellowork.annotation.employee.Worker
import io.popbrain.hellowork.bureau.RecruitmentAgency
import io.popbrain.hellowork.bureau.WorkerSorting
import io.popbrain.hellowork.exception.SuspendHelloWorkException
import io.popbrain.hellowork.util.CachePool
import io.popbrain.hellowork.util.getWorkerAddress
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * HelloWork
 *
 */
class HelloWork private constructor(builder: Dispatcher) {

    val frontDeskAdapterFactories: Array<FrontDeskAdapter.Factory>
    val callbackExecutor: Executor
    val executorService: ExecutorService?

    init {
        this.frontDeskAdapterFactories = builder.frontDeskAdapterFactories.toTypedArray()
        this.callbackExecutor = builder.callbackExecutor
        this.executorService = builder.executorService
    }

    companion object {

        @JvmStatic
        fun verboseEnable(enable: Boolean) {
            Env.instance().isVerboseEnable = enable
        }
    }

    /**
     * Call worker from waiting room.
     * If not found throw SuspendHelloWorkException.
     *
     * @param jobOffer Class in which the job offer is defined
     */
    fun <JOB : Job> callWorker(jobOffer: Class<JOB>): JOB {
        val env = Env.instance()
        return Proxy.newProxyInstance(jobOffer.classLoader, arrayOf(jobOffer), object : InvocationHandler {

            override fun invoke(proxy: Any?, job: Method, jobResources: Array<out Any>?): Any? {
                // JobOffer class must extend Job interface.
                if (job.declaringClass.interfaces[0] != Job::class.java) {
                    throw SuspendHelloWorkException(
                        Status.Error.FATAL,
                        "The declaring @JobOffer class musts extend Job.class."
                    )
                }
                // Not allows default method.
                if (env.isDefaultMethod(job)) {
                    throw SuspendHelloWorkException(Status.Error.FATAL, "HelloWork does not allow default method.")
                }
                val workerJob = loadWorkerJob(jobOffer, job)
                val hrPerson = HRPersonnel(workerJob, jobResources).apply {
                    executorService(executorService)
                }
                return workerJob.liaisonWith(hrPerson)
            }
        }) as JOB
    }

    fun frontDeskAdapter(returnType: Type, annos: Array<Annotation>): FrontDeskAdapter<*, *> =
        frontDeskAdapter(null, returnType, annos)

    fun frontDeskAdapter(
        skipPast: FrontDeskAdapter.Factory?,
        returnType: Type,
        annos: Array<Annotation>
    ): FrontDeskAdapter<*, *> {
        val start = this.frontDeskAdapterFactories.indexOf(skipPast) + 1
        for (i in start..this.frontDeskAdapterFactories.size-1) {
            val adapter = this.frontDeskAdapterFactories[i].get(returnType, annos, this)
            adapter?.let {
                return it
            }
        }
        throw SuspendHelloWorkException(Status.Error.FATAL, "Appropriately FrontDeskAdapter factory did not found.")
    }

    private fun loadWorkerJob(jobOffer: Class<*>, job: Method): WorkerJob<Any, Any> {
        var workerJob = CachePool.instance().get<WorkerJob<Any, Any>>(jobOffer)
        if (workerJob != null) return workerJob
        return WorkerJob.Builder<Any, Any>(this, jobOffer).offer(job).build().apply {
            CachePool.instance().put(jobOffer, this)
        }
    }

    /**
     * Build a new {@link HelloWork}, Dispatcher is the builder of HelloWork.
     */
    class Dispatcher {

        var callbackExecutor: Executor = Env.instance().defaultCallbackExecutor()
        val frontDeskAdapterFactories = ArrayList<FrontDeskAdapter.Factory>()
        var executorService: ExecutorService? = null

        /**
         * Sets an Executor
         *
         * @param executor Executor
         */
        fun callbackExecutor(executor: Executor): Dispatcher {
            this.callbackExecutor = executor
            return this
        }

        /**
         * Adds an AdapterFactory
         *
         * @param adapterFactory FrontDeskAdapter.Factory
         */
        fun addFrontDeskAdapterFactory(adapterFactory: FrontDeskAdapter.Factory): Dispatcher {
            this.frontDeskAdapterFactories.add(adapterFactory)
            return this
        }

        /**
         * Adds an ExecutorService.
         * To use your thread pool, if you have already ExecutorService.
         *
         * @param executorService @nullable
         */
        fun executorService(executorService: ExecutorService): Dispatcher {
            this.executorService = executorService
            return this
        }

        /**
         * Execute the build
         *
         * @return {@link HelloWork}
         */
        fun build(): HelloWork {
            if (this.frontDeskAdapterFactories.size == 0) {
                this.frontDeskAdapterFactories.add(Env.instance().defaultFrontDeskAdapterFactory(callbackExecutor))
            }
            return HelloWork(this)
        }
    }

    /**
     * Execute to search the workers package in child modules.
     *
     * <p>
     * For example
     * <pre><code>
     * Bureau(JobOffer::class.java)
     *     .entry()
     *     .execute()
     */
    class Bureau<T : Job>(private vararg val jobs: Class<T>) {

        private val workerAddressList = ArrayList<String>()
        private val isAndroid: Boolean = Env.instance().isAndroid()
        private var executorService: ExecutorService? = null

        fun addRequest(vararg packageName: String): Bureau<*> {
            this.workerAddressList.addAll(packageName)
            return this
        }

        /**
         * To use your thread pool, if you have already ExecutorService.
         *
         * @param executorService @nullable
         */
        fun executorService(executorService: ExecutorService? = null): Bureau<T> {
            this.executorService = executorService
            return this
        }

        /**
         * Request for search the worker.
         */
        fun entry(): FrontDesk<Array<String>> {
            if (isAndroid) {
                throw SuspendHelloWorkException(Status.Error.FATAL, "This environment is Android. Please call entry(context)")
            }
            jobs.forEach {
                workerAddressList.addAll(it.getWorkerAddress())
            }
            val agency = RecruitmentAgency(workerAddressList.toTypedArray()).apply {
                filterByAnnotation(Worker::class.java, io.popbrain.hellowork.annotation.employee.Job::class.java)
                callResponseHandler(WorkerSorting())
            }
            return RecruitmentStaff(agency, this.executorService)
        }

        fun entry(context: Context): FrontDesk<Array<String>> {
            if (!isAndroid) {
                throw SuspendHelloWorkException(Status.Error.FATAL, "This environment is not Android. Please call entry()")
            }
            jobs.forEach {
                workerAddressList.addAll(it.getWorkerAddress())
            }
            val agency = RecruitmentAgency(context, workerAddressList.toTypedArray()).apply {
                filterByAnnotation(Worker::class.java, io.popbrain.hellowork.annotation.employee.Job::class.java)
                callResponseHandler(WorkerSorting())
            }
            return RecruitmentStaff(agency, this.executorService)
        }
    }

    /**
     * The waiting room for workers.
     * <p>
     * WaitingRoom is the wrapper class of {@link io.popbrain.hellowork.util.CachePool CachePool} for
     * save to workers class information on the memory.
     */
    class WaitingRoom {
        companion object {
            private const val SECRET_WORK = "loves_man_power"

            /**
             * Saves workers class path list.
             *
             * @param workderList workers class paths.
             */
            @JvmStatic
            fun saveWorkerList(workderList: Array<String>) =
                CachePool.instance().put(SECRET_WORK, workderList)

            /**
             * Returns workers class path list.
             *
             * @return String[]
             */
            @JvmStatic
            fun getWorkerList(): Array<String>? =
                CachePool.instance().get<Array<String>>(SECRET_WORK)

            /**
             * Check exist of the saved workers in cache.
             *
             * @return <code>true</code> worker is exist.
             *         <code>false</code> otherwise
             */
            @JvmStatic
            fun isExistWorker(): Boolean = !getWorkerList().isNullOrEmpty()

            /**
             * Save worker's info to cache.
             *
             * @param jobName {@link io.popbrain.hellowork.annotation.employer.JobOffer JobOffer}'s arg
             * @param worker Worker's class and target method.
             */
            @JvmStatic
            fun passingWorker(jobName: String, worker: io.popbrain.hellowork.Worker) =
                CachePool.instance().put(jobName, worker)

            /**
             * To get worker by job name.
             *
             * @param jobname {@link io.popbrain.hellowork.annotation.employer.JobOffer JobOffer}'s arg
             *
             * @return Pair<Class<*>, Method> Returns worker's Class and target Method classes.
             */
            @JvmStatic
            fun callWorker(jobName: String): io.popbrain.hellowork.Worker? =
                CachePool.instance().get<io.popbrain.hellowork.Worker>(jobName)

            /**
             *
             * The Worker's exist checking.
             *
             * @return <code>true</code> if found worker. however It must be registered.
             *         <code>false</code> otherwise
             */
            @JvmStatic
            fun isExistWorker(jobName: String) =
                CachePool.instance().isContain(jobName)
        }
    }

}