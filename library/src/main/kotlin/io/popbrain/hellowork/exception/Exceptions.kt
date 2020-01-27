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
package io.popbrain.hellowork.exception

import io.popbrain.hellowork.Status

open class SuspendHelloWorkException: RuntimeException {
    val error: Status.Error
    constructor(error: Status.Error) : super() { this.error = error }
    constructor(error: Status.Error, message: String?) : super(message) { this.error = error }
    constructor(error: Status.Error, message: String?, cause: Throwable?) : super(message, cause) { this.error = error }
    constructor(error: Status.Error, cause: Throwable?) : super(cause) { this.error = error }
}