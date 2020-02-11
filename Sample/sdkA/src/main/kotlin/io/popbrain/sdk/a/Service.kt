package io.popbrain.sdk.a

import io.popbrain.hellowork.Callback
import io.popbrain.hellowork.Effort
import io.popbrain.hellowork.FrontDesk
import io.popbrain.hellowork.HelloWork
import io.popbrain.hellowork.Status
import io.popbrain.sdk.a.api.VolunteerOffer
import io.popbrain.sdk.a.api.WorkerCallback


class Service {

    /**
     * Example for find workers in advance
     */
    fun recruitWorker() {
        HelloWork.Bureau(VolunteerOffer::class.java)
            .entry()
            .execute().run {
                get()?.let {
                    it.forEach { worker ->
                        System.out.println("worker : $worker")
                    }
                }
            }
    }

    /**
     * Example for find workers in advance by asynchronous
     */
    fun asynchronousRecruitWorker() {
        HelloWork.Bureau(VolunteerOffer::class.java)
            .entry()
            .enqueue(object : Callback<Array<String>> {
                override fun onResponse(call: FrontDesk<Array<String>>?, res: Effort<Array<String>>) {
                    res.get()?.let {
                        it.forEach { worker ->
                            System.out.println("   worker : $worker")
                        }
                    }
                }

                override fun onFailure(res: Effort<Array<String>>, t: Throwable?) {

                }
            })
    }

    /**
     * Exsample for execute a found target worker.
     */
    fun executeWorkerJob(color: String, quantity: Int) {
        HelloWorkImpl.build(VolunteerOffer::class.java)
            .pickupStones(color, quantity)
            .execute().run {
                this?.let {
                    System.out.println("Result : $it")
                    return
                }
                System.out.println("""WTF?!""")
            }
    }

    /**
     * Exsample for execute a found target worker by asynchronous.
     */
    fun asynchronousExecuteWorkerJob(color: String, quantity: Int) {
        HelloWorkImpl.build(VolunteerOffer::class.java)
            .pickupStones(color, quantity)
            .enqueue(object : WorkerCallback<Boolean> {
                override fun onResponse(res: Effort<Boolean>) {
                    res.get()?.let {
                        if (res.status == Status.COMPLETE) {
                            System.out.println("Result : $it")
                        }
                        return
                    }
                    System.out.println("""WTF?!""")
                }

                override fun onFailure(res: Effort<Boolean>, error: Throwable?) {
                    System.out.println("WTF : ${res.isError}")
                    System.out.println("WTF reason : ${res.message}")
                }
            })
    }
}