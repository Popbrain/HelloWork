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
import java.lang.reflect.Type

class DefaultFrontDeskAdaterFactory: FrontDeskAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, helloWork: HelloWork): FrontDeskAdapter<*, *>? {
        val responseType = returnType.getFrontDeskResponseType()
        return object: FrontDeskAdapter<Any, FrontDesk<*>> {
            override fun responseType(): Type = responseType
            override fun liaison(frontDesk: FrontDesk<Any>): FrontDesk<*> = frontDesk
        }
    }
}