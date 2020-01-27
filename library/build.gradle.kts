group = "io.popbrain"
version = "0.1.0"
val artifactId = "hellowork"

plugins {
    java
    kotlin("jvm")
    `maven-publish`
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
    val versions: Map<String, String> by project
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
//    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("stdlib"))
    implementation("""org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}""")
    testCompile("junit", "junit", "4.12")
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

val sourceJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
    archiveClassifier.set("javadoc")
    from(JavaPlugin.JAVADOC_TASK_NAME)
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/mavenRepo")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            groupId = "$group"
            artifactId = "hellowork"
            version = "$version"
            artifact(sourceJar.get())
            artifact(javadocJar.get())
        }
    }
}