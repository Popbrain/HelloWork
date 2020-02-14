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

import android.util.Log
import io.popbrain.hellowork.Env
import io.popbrain.hellowork.FrontDesk
import io.popbrain.hellowork.Status
import io.popbrain.hellowork.annotation.HelloWork
import io.popbrain.hellowork.exception.SuspendHelloWorkException
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.sql.Timestamp

fun Type.rawType(): Class<*> {
    this.checkNotNull("Type is null")
    val klass: Class<*>? = when (this) {
        is Class<*> -> this
        is ParameterizedType -> {
            val rawType = getRawType()
            if (rawType !is Class<*>) throw IllegalArgumentException() else rawType
        }
        is GenericArrayType ->
            java.lang.reflect.Array.newInstance(genericComponentType.rawType(), 0).javaClass
        is TypeVariable<*> -> Object::class.java
        is WildcardType -> upperBounds[0].rawType()
        else -> null
    }
    return if (klass == null) throw SuspendHelloWorkException(Status.Error.FATAL) else klass
}

fun ParameterizedType.getParameterUpperBound(index: Int): Type {
    val types = actualTypeArguments
    if (index < 0 || index >= types.size) {
        throw IllegalArgumentException("""Index "$index" not in range [0, ${types.size} for $this""")
    }
    val targetType = types[index]
    if (targetType.isWildcard()) {
        (targetType as WildcardType).upperBounds[0]
    }
    return targetType
}

fun Type.getFrontDeskResponseType(): Type {
    if (equalRawType<FrontDesk<*>>() && this is ParameterizedType) {
        return getParameterUpperBound(0)
    }
    throw SuspendHelloWorkException(Status.Error.FATAL, "The return type of JobOffer method must be 'FrontDesk.class', if you use DefaultStaffAdaterFactory.")
}

fun Method.hasEqualParams(method: Method): Boolean {
    val aParamTypes = parameterTypes
    val bParamTypes = method.parameterTypes
    if (aParamTypes.size != bParamTypes.size) return false
    if (aParamTypes.size == 0 && bParamTypes.size == 0) return true
    for (i in 0..aParamTypes.size-1) {
        if (aParamTypes[i] != bParamTypes[i]) {
            return false
        }
    }
    return true
}

fun Method.isEqualReturnTypes(method: Method): Boolean = returnType == method.returnType
fun Type.hasUnresolvableType(): Boolean {
    if (this is Class<*>) return false
    if (this is ParameterizedType) {
        this.actualTypeArguments.forEach {
            if (it.hasUnresolvableType()) return true
        }
        return false
    }
    if (this is GenericArrayType) {
        return genericComponentType.hasUnresolvableType()
    }
    if (this is TypeVariable<*> || this is WildcardType) {
        return true
    }
    throw SuspendHelloWorkException(Status.Error.FATAL, """""")
}

fun Method.printNameAndArgs(): String {
    val str = StringBuilder(declaringClass.canonicalName).append(".").append(name).append("(")
    val types = parameterTypes
    for (i in 0..types.size-1) {
        if (i == 0) {
            str.append(types[i].canonicalName)
        } else {
            str.append(", ").append(types[i].canonicalName)
        }
    }
    return str.append(")").toString()
}

inline fun <reified T> Type.equalRawType(): Boolean = rawType() == T::class.java
fun Type.isParameterized(): Boolean = this is ParameterizedType
fun Type.isGenericArray(): Boolean = this is GenericArrayType
fun Type.isWildcard(): Boolean = this is WildcardType

inline fun <reified T> Method.isDeclaringClass(): Boolean = declaringClass == T::class.java

fun Any?.checkNotNull(mes: String? = ""): Any {
    if (this != null) return this
    throw NullPointerException(mes)
}

fun <T: Annotation> Class<*>.getAnnotation(annotationClass: Class<T>): T {
    return getAnnotationsByType(annotationClass)[0]
}

fun Class<*>.getWorkerAddress(): Array<out String> {
    return getAnnotation(HelloWork::class.java).value
}

fun Class<*>.getAllAnnotations(): Array<Annotation> {
    val annotations = ArrayList<Annotation>()
    annotations.addAll(declaredAnnotations)
    declaredFields.forEach {
        it.declaredAnnotations.forEach {
            annotations.add(it)
        }
    }
    declaredMethods.forEach {
        it.declaredAnnotations.forEach {
            annotations.add(it)
        }
    }
    return annotations.toTypedArray()
}

fun String.isMatchCase(target: String): Boolean {
    return this.toLowerCase().equals(target.toLowerCase())
}

fun Annotation.equalSimpleName(klass: Class<*>): Boolean {
    annotationClass.simpleName?.let {
        return it.isMatchCase(klass.simpleName)
    }
    return false
}

fun Annotation.equalJavaObjectType(klass: Class<*>): Boolean {
    return (annotationClass.javaObjectType == klass)
}

fun Annotation.deepEqual(klass: Class<*>): Boolean {
    if (equalJavaObjectType(klass)) {
        return true
    } else if (equalSimpleName(klass)) {
        return true
    } else {
        return false
    }
}

fun Annotation.isStringContain(str: String): Boolean {
    try {
        val annoString = (annotationClass as Any).toString()
        return (0 <= annoString.indexOf(str))
    } catch (e: Exception) {
        return false
    }
}

open class SingletonHolder<out T>(private var creator: (() -> T)) {
    @Volatile private var instance: T? = null

    fun instance(): T {
        val i = instance
        i?.let { return it }
        return synchronized(this) {
            val i2 = instance
            i2?.let {
                it
            } ?: run {
                val created = creator.invoke()
                instance = created
                created
            }
        }
    }
}

enum class Log {
    out;

    private val isLoggable: Boolean = Env.instance().isVerboseEnable
    private val isAndroid: Boolean = Env.instance().isAndroid()

    fun v(message: String) {
        if (!isLoggable) return
        if (isAndroid) {
            Log.v(TAG, message)
        } else {
            val currentTimestamp = Timestamp(System.currentTimeMillis()).toString()
            System.out.println("""$currentTimestamp $TAG | $message""")
        }
    }

    fun e(message: String, t: Throwable? = null) {
        if (!isLoggable) return
        if (isAndroid) {
            Log.e(TAG, message, t)
        } else {
            val currentTimestamp = Timestamp(System.currentTimeMillis()).toString()
            System.out.println("""$currentTimestamp $TAG | $message""")
            t?.let {
                t.printStackTrace()
            }
        }
    }
    companion object {

        private const val TAG = "[HelloWork]"

    }
}