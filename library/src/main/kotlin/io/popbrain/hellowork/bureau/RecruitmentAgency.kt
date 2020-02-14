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
package io.popbrain.hellowork.bureau

import android.content.Context
import dalvik.system.PathClassLoader
import io.popbrain.hellowork.Env
import io.popbrain.hellowork.util.Log
import io.popbrain.hellowork.util.deepEqual
import io.popbrain.hellowork.util.equalJavaObjectType
import io.popbrain.hellowork.util.getAllAnnotations
import io.popbrain.hellowork.util.isStringContain
import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.collections.ArrayList

/**
 * Find workers class
 */
class RecruitmentAgency {

    private val PACKAGE_SEPARATOR = "."
    private val CLASS_SUFFIX = ".class"
    private val workerAddressList: Array<String>
    private val classLoader: ClassLoader
    private var filterAnnotationClasses: Array<Class<out Annotation>>? = null
    private var filterStrings: Array<String>? = null
    private var callReponseHandler: CallReponseHandler? = null
    private val isAndroid = Env.instance().isAndroid()
    private val packageCodePath: String

    constructor(workerAddressList: Array<String>) {
        this.classLoader = this@RecruitmentAgency.javaClass.classLoader
        this.workerAddressList = workerAddressList
        this.packageCodePath = ""
    }

    constructor(context: Context, workerAddressList: Array<String>) {
        this.classLoader = context.classLoader
        this.workerAddressList = workerAddressList
        this.packageCodePath = context.packageCodePath
    }


    /**
     * Add annotations to filter
     */
    fun <T : Annotation> filterByAnnotation(vararg annotationClasses: Class<out T>): RecruitmentAgency {
        this.filterAnnotationClasses = arrayOf(*annotationClasses)
        return this
    }

    /**
     * Add string to filter
     */
    fun filterByString(vararg strs: String): RecruitmentAgency {
        val filters = ArrayList<String>()
        strs.forEach {
            if (!it.isNullOrEmpty()) filters.add(it)
        }
        if (0 < filters.size) {
            this.filterStrings = filters.toTypedArray()
        }
        return this
    }

    fun callResponseHandler(handler: CallReponseHandler): RecruitmentAgency {
        callReponseHandler = handler
        return this
    }

    fun execute(): Array<String>? {
        return with(findAll()) {
            if (0 < size) {
                this
            } else {
                null
            }
        }
    }

    fun isFilterEnable(): Boolean = isFilterByAnnotationEnable() || isFilterByStringEnable()
    fun isFilterByAnnotationEnable(): Boolean =
        (this.filterAnnotationClasses != null && 0 < this.filterAnnotationClasses!!.size)

    fun isFilterByStringEnable(): Boolean =
        (this.filterStrings != null && 0 < this.filterStrings!!.size)

    private fun findAll(): Array<String> {
        val classNameList = ArrayList<String>()
        Log.out.v("workderAddressList size : ${workerAddressList.size}")
        workerAddressList.forEach {
            find(it)?.let {
                classNameList.addAll(it)
            }
        }
        return classNameList.toTypedArray()
    }

    /**
     * Find target class
     */
    private fun find(target: String): Array<String>? {
        if (isAndroid) {
            Log.out.v("run on Android Environment")
            Log.out.v("""Target package name : $target""")
            findFromAndroidPackage(target)?.let {
                return arrayOf(it)
            }
            return null
        }
        Log.out.v("run on Java Environment")
        return findFromJavaPackage(target)
    }

    private fun findFromAndroidPackage(packageName: String): String? {
        var classname: String? = null
        try {
            val loader = PathClassLoader(packageCodePath, classLoader)
            val loadClass = loader.loadClass(packageName)
            if (!isFilterEnable()) {
                return if (isTarget(packageName, loadClass)) packageName else null
            }
            doFiltering(loadClass) { res, klass ->
                if (res && klass != null) {
                    if (callReponseHandler != null) {
                        if (callReponseHandler!!.isTarget(packageName, klass)) {
                            classname = packageName
                        }
                    } else {
                        classname = packageName
                    }
                }
            }
            return classname
        } catch (e: ClassNotFoundException) {
            getClass(packageName)?.let {
                return if (isTarget(packageName, it)) packageName else null
            }
            Log.out.e("Could not found a class $packageName. In case of android must be assign full path of Class to arg of @HelloWork.")
        } catch (e: java.lang.Exception) {
            Log.out.e("Failed to find Android package.", e)
        }
        return null
    }

    private fun findFromJavaPackage(target: String): Array<String>? {
        if (isAvailable(target)) {
            return arrayOf(target)
        }
        try {
            val packageName = target.replace(PACKAGE_SEPARATOR, File.separator)
            Log.out.v("""Find Package Name : $packageName""")
            if (classLoader == null) return null
            val urls = classLoader.getResources(packageName)
            val classNameList = ArrayList<String>()
            while (urls.hasMoreElements()) {
                val rootUrl = urls.nextElement()
                findByUrl(packageName, rootUrl)?.let {
                    classNameList.addAll(it)
                }
            }
            return classNameList.toTypedArray()
        } catch (e: java.lang.Exception) {
            Log.out.e("Failed to find Java package.", e)
        }
        return null
    }

    private fun findByUrl(rootPackageName: String, rootUrl: URL): Array<String>? {
        return when (rootUrl.protocol) {
            "file" -> {
                Log.out.v("""Search File Path : ${rootUrl.toURI()}""")
                findFromFile(rootPackageName, rootUrl)
            }
            "jar" -> {
                Log.out.v("""Search Jar's Path : ${rootUrl.toURI()}""")
                findFromJar(rootPackageName, rootUrl)
            }
            else -> null
        }
    }

    private fun findFromJar(rootPackageName: String, rootUrl: URL): Array<String>? {
        try {
            val classNameList = ArrayList<String>()
            val jarUrlConnection = rootUrl.openConnection() as JarURLConnection
            val jarEnum = jarUrlConnection.jarFile.entries()
            while (jarEnum.hasMoreElements()) {
                val fileName = jarEnum.nextElement().name
                Log.out.v("   Searching in jar : $fileName")
                getClassCanonicalName(fileName, rootPackageName).run {
                    this?.let { classFullName ->
                        if (isAvailable(classFullName)) {
                            classNameList.add(classFullName)
                        }
                    }
                }
            }
            return classNameList.toTypedArray()
        } catch (e: Exception) {
            return null
        }
    }

    private fun findFromFile(rootPackageName: String, rootUrl: URL): Array<String>? {
        val classNameList = ArrayList<String>()
        val rootPath = Paths.get(rootUrl.toURI())
        try {
            Files.walkFileTree(rootPath, object : FileVisitor<Path> {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    file?.let { path ->
                        Log.out.v("   Searching file : $path")
                        getClassCanonicalName(path.toString(), rootPackageName).run {
                            this?.let { classFullName ->
                                if (isAvailable(classFullName)) {
                                    classNameList.add(classFullName)
                                }
                            }
                        }
                    }
                    Objects.requireNonNull(file)
                    Objects.requireNonNull<BasicFileAttributes>(attrs)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                    Objects.requireNonNull(dir)
                    if (exc != null) throw exc
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    Objects.requireNonNull(file)
                    throw exc!!
                }

                override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    Objects.requireNonNull(dir)
                    Objects.requireNonNull<BasicFileAttributes>(attrs)
                    return FileVisitResult.CONTINUE
                }
            })
            return classNameList.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getClassCanonicalName(path: String, rootPackageName: String): String? {
        if (path.endsWith(CLASS_SUFFIX)) {
            val beginIndex = path.lastIndexOf(rootPackageName)
            val endIndex = path.lastIndexOf(CLASS_SUFFIX)
            return path.substring(beginIndex, endIndex)
                .replace(File.separator, PACKAGE_SEPARATOR)
        }
        return null
    }

    /**
     * Java
     */
    private fun isAvailable(packageName: String): Boolean {
        var isValid = false
        if (isFilterEnable()) {
            doFiltering(packageName) { res, klass ->
                if (res && klass != null) {
                    isValid = isTarget(packageName, klass)
                }
            }
        } else {
            try {
                getClass(packageName, classLoader)?.let {
                    isValid = isTarget(packageName, it)
                }
            } catch (e: Exception) {
            }
        }
        return isValid
    }

    /**
     * Java / Android
     */
    private fun isTarget(packageName: String, klass: Class<*>): Boolean {
        if (callReponseHandler == null) return true
        return callReponseHandler!!.isTarget(packageName, klass)
    }

    private fun doFiltering(classPackage: String, onResult: (Boolean, Class<*>?) -> Unit) {
        if (classPackage.isNullOrEmpty()) onResult.invoke(false, null)
        try {
            getClass(classPackage)?.let {
                doFiltering(it, onResult)
                return
            }
        } catch (e: java.lang.Exception) {
            Log.out.e("""Occurred error.""", e)
        }
        onResult.invoke(false, null)
    }

    /**
     * Android
     */
    private fun doFiltering(klass: Class<*>, onResult: (Boolean, Class<*>?) -> Unit) {
        if (klass == null) onResult.invoke(false, null)
        try {
            val annotations = klass.getAllAnnotations()
            if (annotations.size == 0) onResult.invoke(false, null)
            var result = 0
            for (implAnno in annotations) {
                if (implAnno.equalJavaObjectType(Metadata::class.java)) continue
                if (isFilterByAnnotationEnable() && !isAnnotationClassesContain(implAnno)) {
                    continue
                }
                if (isFilterByStringEnable() && !isFilterStringsContain(implAnno)) {
                    continue
                }
                result++
            }
            onResult.invoke(this.filterAnnotationClasses!!.size <= result, klass)
        } catch (e: ClassNotFoundException) {
            // nothing
        } catch (e: Exception) {
            Log.out.e("""Occurred error.""", e)
            onResult.invoke(false, null)
        }
    }

    private fun isAnnotationClassesContain(anno: Annotation): Boolean {
        if (!isFilterByAnnotationEnable()) return false
        var result = false
        for (filterAnno in this.filterAnnotationClasses!!) {
            if (anno.deepEqual(filterAnno)) {
                result = true; continue
            }
        }
        return result
    }

    private fun isFilterStringsContain(anno: Annotation): Boolean {
        if (!isFilterByStringEnable()) return false
        var result = false
        this.filterStrings!!.forEach {
            result = anno.isStringContain(it)
        }
        return result
    }

    private fun getClass(className: String, loader: ClassLoader? = null): Class<*>? {
        try {
            if (loader == null) {
                return javaClass.classLoader.loadClass(className)
            } else {
                return loader.loadClass(className)
            }
        } catch (e: Exception) {
            Log.out.e("Could not get class.", e)
        }
        return null
    }

    interface CallReponseHandler {

        fun isTarget(classPackage: String, resultClass: Class<*>): Boolean

    }

}
