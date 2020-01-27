package io.popbrain.sdk.b

import io.popbrain.hellowork.annotation.employee.Job
import io.popbrain.hellowork.annotation.employee.Worker

/**
 * Worker side definition.
 *
 * - Annotation @HelloWork's arg is unnecessary.
 */
@Worker
class WorkerSample {

    /**
     * - Annotation @Worker's arg value must be same with @JobOffer's arg value.
     */
    @Job("pickup_stones")
    fun pickupStone(color: String, quantity: Int): Boolean {
        System.out.println("""Picked up Color : $color / Quantity : $quantity stones""")
        return true
    }

    @Job("collect_stones")
    fun collectStone(color: String, quantity: Int) {
        System.out.println("""Collected Color : $color / Quantity : $quantity stones""")
    }

}