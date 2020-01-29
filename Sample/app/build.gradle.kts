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
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation(kotlin("stdlib-jdk8"))
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