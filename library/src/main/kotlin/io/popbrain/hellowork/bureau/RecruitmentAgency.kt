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

import io.popbrain.hellowork.util.Log
import io.popbrain.hellowork.util.deepEqual
import io.popbrain.hellowork.util.equalJavaObjectType
import io.popbrain.hellowork.util.getAllAnnotations
import io.popbrain.hellowork.util.isStringContain
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
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
class RecruitmentAgency(private val workerAddressList: Array<String>) {

    private val PACKAGE_SEPARATOR = "."
    private val CLASS_SUFFIX = ".class"
    private val classLoader: WeakReference<ClassLoader> = WeakReference(ClassLoader.getSystemClassLoader())
    private var filterAnnotationClasses: Array<Class<out Annotation>>? = null
    private var filterStrings: Array<String>? = null
    private var callReponseHandler: CallReponseHandler? = null

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
        classListFactory(target).run {
            if (0 < size) return this
        }
        val classLoader = classLoader.get()
        if (classLoader == null) return null

        val rootPackageName = target.replace(PACKAGE_SEPARATOR, File.separator)
        Log.out.v("""Find Package Name : ${rootPackageName}""")
        return findByPackage(classLoader, rootPackageName)
    }

    private fun findByPackage(classLoader: ClassLoader, rootPackageName: String): Array<String> {
        val urls = classLoader.getResources(rootPackageName)
        val classNameList = ArrayList<String>()
        while (urls.hasMoreElements()) {
            val rootUrl = urls.nextElement()
            findByUrl(rootPackageName, rootUrl)?.let {
                classNameList.addAll(it)
            }
        }
        return classNameList.toTypedArray()
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
            var classNameList: Array<String>? = null
            val jarUrlConnection = rootUrl.openConnection() as JarURLConnection
            val jarEnum = jarUrlConnection.jarFile.entries()
            while (jarEnum.hasMoreElements()) {
                val fileName = jarEnum.nextElement().name
                Log.out.v("   Searching in jar : $fileName")
                getClassCanonicalName(fileName, rootPackageName).run {
                    this?.let { classFullName ->
                        classNameList = classListFactory(classFullName)
                    }
                }
            }
            return classNameList
        } catch (e: Exception) {
            return null
        }
    }

    private fun findFromFile(rootPackageName: String, rootUrl: URL): Array<String>? {
        var classNameList: Array<String>? = null
        val rootPath = Paths.get(rootUrl.toURI())
        try {
            Files.walkFileTree(rootPath, object : FileVisitor<Path> {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    file?.let { path ->
                        Log.out.v("   Searching file : $path")
                        getClassCanonicalName(path.toString(), rootPackageName).run {
                            this?.let { classFullName ->
                                classNameList = classListFactory(classFullName)
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
            return classNameList
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

    private fun classListFactory(packageName: String): Array<String> {
        val classNameList = ArrayList<String>()
        if (isFilterEnable()) {
            doFiltering(packageName) { res, klass ->
                if (res && klass != null) {
                    if (callReponseHandler != null) {
                        if (callReponseHandler!!.onSort(packageName, klass)) {
                            classNameList.add(packageName)
                        }
                    } else {
                        classNameList.add(packageName)
                    }
                }
            }
        } else {
            try {
                Class.forName(packageName)
                classNameList.add(packageName)
            } catch (e: Exception) {
            }
        }
        return classNameList.toTypedArray()
    }

    private fun doFiltering(classPackage: String, onResult: (Boolean, Class<*>?) -> Unit) {
        if (classPackage.isNullOrEmpty() || !isFilterEnable()) onResult.invoke(false, null)
        try {
            val klass = Class.forName(classPackage)
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

    interface CallReponseHandler {

        fun onSort(classPackage: String, resultClass: Class<*>): Boolean

    }

}
