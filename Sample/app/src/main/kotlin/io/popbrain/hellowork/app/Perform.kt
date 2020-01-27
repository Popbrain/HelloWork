package io.popbrain.hellowork.app

import io.popbrain.sdk.a.Service

fun main(args: Array<String>) {
    /** Testing the find workers */
    Service().recruitWorker()

//    /** Testing the employer side */
    Service().asynchronousExecuteWorkerJob("Red", 5)
}