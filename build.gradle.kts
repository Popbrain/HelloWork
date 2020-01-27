//plugins {
//    java
//    kotlin("jvm") version "1.3.61"
//}

//group = "io.popbrain"
//version = "1.0-SNAPSHOT"

buildscript {
    val versions by extra { mapOf(
        "kotlin"  to "1.3.61"
    ) }
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
//        classpath("com.android.tools.build", "gradle", "3.5.2")
        classpath("""org.jetbrains.kotlin:kotlin-gradle-plugin:${versions["kotlin"]}""")
        classpath("org.jetbrains.kotlin:kotlin-stdlib")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

subprojects {
    group = "io.popbrain.hellowork"
}

//configure<JavaPluginConvention> {
//    sourceCompatibility = JavaVersion.VERSION_1_8
//}
//tasks {
//    compileKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//    compileTestKotlin {
//        kotlinOptions.jvmTarget = "1.8"
//    }
//}
