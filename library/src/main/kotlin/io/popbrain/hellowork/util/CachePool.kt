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
package io.popbrain.hellowork.util

import java.util.concurrent.ConcurrentHashMap

class CachePool private constructor() {

    companion object: SingletonHolder<CachePool>(::CachePool)
    private var pool = ConcurrentHashMap<Any, Any>()

    fun put(key: Any, value: Any): Any? {
        synchronized(this.pool) {
            return this.pool.put(key, value)
        }
    }
    fun <R> get(key: Any): R? {
        try {
            val result =  this.pool.get(key) as R
            if (result != null) return result
            return synchroGet<R>(key)
        } catch (e: Exception) {
            return null
        }
    }

    fun isContain(key: Any): Boolean {
        var result = this.pool.contains(key)
        if (!result) return synchronized(this.pool) { this.pool.contains(key) }
        return result
    }

    fun flush() {
        this.pool = ConcurrentHashMap()
    }

    private fun <R> synchroGet(key: Any): R? {
        synchronized(this.pool) {
            return this.pool.get(key) as R
        }
    }
}