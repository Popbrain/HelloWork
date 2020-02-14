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

import android.os.Build
import android.os.Handler
import android.os.Looper
import io.popbrain.hellowork.exception.SuspendHelloWorkException
import io.popbrain.hellowork.util.SingletonHolder
import java.lang.reflect.Method
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

//enum class Env {
//
//    INSTANCE;
//
//    val project: Project
//    val defaultThreadPool: DefaultThreadPool
//    var isVerboseEnable = false
//
//    init {
//        this.project = get()
//        this.defaultThreadPool = DefaultThreadPool.instance()
//    }
//
//    open fun defaultCallbackExecutor(): Executor = this.project.defaultCallbackExecutor()
//    open fun isDefaultMethod(method: Method): Boolean = method.isDefault
//    open fun defaultFrontDeskAdapterFactory(callbackExecutor: Executor?): FrontDeskAdapter.Factory {
//        callbackExecutor?.let {
//            return FrontDeskAdapterFactory(it)
//        }
//        return DefaultFrontDeskAdaterFactory()
//    }
//
//    private fun get(): Project {
//        try {
//            Class.forName("android.app.Activity")
//            if (0 < Build.VERSION.SDK_INT) {
//                return Project.Android()
//            }
//        } catch (e: ClassNotFoundException) {
//        }
//        return Project.JavaDefault()
//    }
//
//    abstract class Project {
//
//        abstract fun defaultCallbackExecutor(): Executor
//
//        class Android: Project() {
//            override fun defaultCallbackExecutor(): Executor = MainThreadExecutor()
//            private class MainThreadExecutor : Executor {
//                private val handler = Handler(Looper.getMainLooper())
//                override fun execute(command: Runnable) {
//                    this.handler.post(command)
//                }
//            }
//        }
//
//        class JavaDefault: Project() {
//            override fun defaultCallbackExecutor(): Executor = DefaultExecutor()
//            private class DefaultExecutor : Executor {
//                override fun execute(command: Runnable) {
//                    command.run()
//                }
//            }
//        }
//
//    }

open class Env private constructor() {

    companion object : SingletonHolder<Env>({ Env().get() })
    val defaultThreadPool: DefaultThreadPool
    var isVerboseEnable = false

    init {
        this.defaultThreadPool = DefaultThreadPool.instance()
    }

    private fun get(): Env {
        try {
            Class.forName("android.app.Activity")
            if (0 < Build.VERSION.SDK_INT) {
                return Project.Android
            }
        } catch (e: ClassNotFoundException) {
        }
        return Project.JavaDefault
    }
    open fun isAndroid(): Boolean = false
    open fun defaultCallbackExecutor(): Executor = Project.JavaDefault.defaultCallbackExecutor()
    open fun isDefaultMethod(method: Method): Boolean = method.isDefault
    open fun defaultFrontDeskAdapterFactory(callbackExecutor: Executor?): FrontDeskAdapter.Factory {
        callbackExecutor?.let {
            return FrontDeskAdapterFactory(it)
        }
        return DefaultFrontDeskAdaterFactory()
    }

    sealed class Project : Env() {

        object JavaDefault : Project() {
            override fun isAndroid(): Boolean = false
            override fun defaultCallbackExecutor(): Executor = DefaultExecutor()
            private class DefaultExecutor : Executor {
                override fun execute(command: Runnable) {
                    command.run()
                }
            }
        }

        object Android : Project() {
            override fun isAndroid(): Boolean = true
            override fun defaultCallbackExecutor(): Executor = MainThreadExecutor()
            private class MainThreadExecutor : Executor {
                private val handler = Handler(Looper.getMainLooper())
                override fun execute(command: Runnable) {
                    this.handler.post(command)
                }
            }
        }
    }

    class DefaultThreadPool private constructor() {

        companion object {
            fun instance() = DefaultThreadPool()
        }

        private val defaultPool: ExecutorService =
            ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, SynchronousQueue<Runnable>())

        fun execute(r: () -> Unit) {
            try {
                this.defaultPool.execute(r)
            } catch (e: Exception) {
                throw SuspendHelloWorkException(Status.Error.OTHER, "Ran on HelloWork's thread pool.", e)
            }
        }

        fun <T> execute(task: FutureTask<T>, timeOut: Long, unit: TimeUnit): T {
            try {
                val future = this.defaultPool.submit(task)
                return future.get(timeOut, unit) as T
            } catch (e: Exception) {
                throw SuspendHelloWorkException(Status.Error.OTHER, "Ran on HelloWork's thread pool.", e)
            }
        }
    }
}