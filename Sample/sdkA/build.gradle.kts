group = "io.popbrain.sdk"
version = "1.0.0"

plugins {
    java
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

project.sourceSets {
    getByName("main") {
        java.srcDir("src/main/kotlin")
    }
}

dependencies {
    val versions: Map<String, String> by project
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
    implementation("""org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}""")
    implementation(project(":library"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}