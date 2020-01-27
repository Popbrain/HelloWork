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

/**
 * The Response from worker.
 */
class Effort<out T> private constructor(builder: Builder<T>) {

    val result: T?
    val status: Status
    val message: String?
    val error: Throwable?
    val isError: Boolean

    init {
        this.result = builder.result
        this.status = builder.status
        this.message = builder.message
        this.error = builder.error
        this.isError = this.error != null && (status == Status.COMPLETE || status == Status.CANCELED)
    }

    fun get(): T? = this.result

    class Builder<T> {

        constructor() : this(null)
        constructor(result: T?) {
            this.result = result
        }

        var result: T? = null
        var message: String? = null
        var status: Status = Status.COMPLETE
        var error: Throwable? = null

        fun setStatus(status: Status): Builder<T> {
            this.status = status
            return this
        }

        fun setMessage(message: String): Builder<T> {
            this.message = message
            return this
        }

        fun setException(exception: Throwable): Builder<T> {
            this.error = exception
            return this
        }

        fun build(): Effort<T> = Effort(this)
    }
}