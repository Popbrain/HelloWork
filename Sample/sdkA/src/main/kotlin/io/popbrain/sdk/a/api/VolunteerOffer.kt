package io.popbrain.sdk.a.api

import io.popbrain.hellowork.FrontDesk
import io.popbrain.hellowork.annotation.HelloWork
import io.popbrain.hellowork.Job
import io.popbrain.hellowork.annotation.employer.JobOffer

/**
 * - Annotation @HelloWork's args are the search target packages or classes.
 * - Job definition class must be implement a Job interface.
 */
@HelloWork("io.popbrain.sdk.b")
interface VolunteerOffer : Job {

    /**
     * - Annotation @JobOffer's arg is a job name to connect with a worker.
     * - Returns type is customizable.
     */
    @JobOffer("pickup_stones")
    fun pickupStones(color: String, quantity: Int): WorkerCall<Boolean>

    @JobOffer("collect_stones")
    fun collectStone(color: String, quantity: Int): FrontDesk<Void>
}