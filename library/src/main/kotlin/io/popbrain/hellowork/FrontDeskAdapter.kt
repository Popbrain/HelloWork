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

import io.popbrain.hellowork.util.getFrontDeskResponseType
import io.popbrain.hellowork.util.getParameterUpperBound
import io.popbrain.hellowork.util.rawType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

interface FrontDeskAdapter<R, out T> {

    fun responseType(): Type

    fun liaison(frontDesk: FrontDesk<R>): T

    abstract class Factory {

        abstract fun get(returnType: Type, annotations: Array<Annotation>, helloWork: HelloWork): FrontDeskAdapter<*, *>?

        companion object {

            @JvmStatic
            fun getRawType(returnType: Type): Type = returnType.rawType()

            @JvmStatic
            fun getParameterUpperBound(returnType: Type, index: Int): Type {
                if (returnType !is ParameterizedType) {
                    throw IllegalStateException("""returnType must have generic type (e.g., FrontDesk<Effort<*>>)""")
                }
                return returnType.getParameterUpperBound(index)
            }
        }

    }
}