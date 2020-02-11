group = "io.popbrain.hellowork.app"
version = "1.0.0"

plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
}

project.sourceSets {
    getByName("main") {
        java.srcDir("src/main/kotlin")
    }
    getByName("test") {
        java.srcDir("src/test/kotlin")
    }
}

dependencies {
//    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
//    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("hellowork-*.jar"))))
//    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("sdkA-*.jar"))))
//    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("sdkB-*.jar"))))
    implementation(kotlin("stdlib-jdk8"))

    val versions: Map<String, String> by project
    implementation("""org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}""")

    implementation(project(":library"))
    implementation(project(":Sample:sdkA"))
    implementation(project(":Sample:sdkB"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
        kotlinOptions.jvmTarget = sourceCompatibility
    }
    compileTestKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
        kotlinOptions.jvmTarget = sourceCompatibility
    }
}