package io.popbrain.hellowork

import android.os.Build
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Method
import java.util.concurrent.Executor

enum class NewEnv {

    INSTANCE;

    private val project: Project
    init {
        this.project = get()
    }

    open fun defaultCallbackExecutor(): Executor = this.project.defaultCallbackExecutor()
    open fun isDefaultMethod(method: Method): Boolean = method.isDefault
    open fun defaultFrontDeskAdapterFactory(callbackExecutor: Executor?): FrontDeskAdapter.Factory {
        callbackExecutor?.let {
            return FrontDeskAdapterFactory(it)
        }
        return DefaultFrontDeskAdaterFactory()
    }

    private fun get(): Project {
        try {
            Class.forName("android.app.Activity")
            if (0 < Build.VERSION.SDK_INT) {
                return Project.Android()
            }
        } catch (e: ClassNotFoundException) {
        }
        return Project.JavaDefault()
    }

    private abstract class Project {

        abstract fun defaultCallbackExecutor(): Executor

        class Android: Project() {
            override fun defaultCallbackExecutor(): Executor = MainThreadExecutor()
            private class MainThreadExecutor : Executor {
                private val handler = Handler(Looper.getMainLooper())
                override fun execute(command: Runnable) {
                    this.handler.post(command)
                }
            }
        }

        class JavaDefault: Project() {
            override fun defaultCallbackExecutor(): Executor = DefaultExecutor()
            private class DefaultExecutor : Executor {
                override fun execute(command: Runnable) {
                    command.run()
                }
            }
        }

    }


}