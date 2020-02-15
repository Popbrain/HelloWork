group = "com.github.popbrain"
version = "1.0.0"
val artifactId = "hellowork"

plugins {
    java
    kotlin("jvm")
    `maven-publish`
    signing
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

artifacts {
    archives(javadocJar)
    archives(sourceJar)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "$group"
            artifactId = "hellowork"
            version = "$version"
            artifact(sourceJar.get())
            artifact(javadocJar.get())
            pom {
                name.set("Hello Work")
                description.set("Hellowork is for Java and Android what can call modules from a module without the reflection implementation.")
                url.set("https://github.com/Popbrain/HelloWork")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set(project.properties.get("developer.id") as String)
                        name.set(project.properties.get("developer.name") as String)
                        email.set(project.properties.get("developer.email") as String)
                        organization {
                            name.set(project.properties.get("developer.organization.name") as String)
                            url.set(project.properties.get("developer.organization.url") as String)
                        }
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Popbrain/HelloWork.git")
                    developerConnection.set("scm:git:ssh://github.com/Popbrain/HelloWork.git")
                    url.set("https://github.com/Popbrain/HelloWork.git")
                }
            }
        }

    }
    repositories {
        maven {

            url = uri("$buildDir/mavenRepo")
        }
        maven {
            val sonatypeUsername = project.properties.get("sonatypeUsername") as String
            val sonatypePassword = project.properties.get("sonatypePassword") as String
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }

    signing {
        val signingKeyId = project.properties.get("signing.keyId") as String
        val signingPasswork = project.properties.get("signing.password") as String
        System.out.println("signingKeyID : ${signingKeyId}")
        isRequired = true
        useInMemoryPgpKeys(signingKeyId, signingPasswork)
        sign(publishing.publications["mavenJava"])
    }
}