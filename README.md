[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.popbrain/hellowork/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.popbrain/hellowork) 
[![license](https://img.shields.io/badge/Java-1.8-brightgreen.svg?style=flat)](https://github.com/popbrain/hellowork)
[![license](https://img.shields.io/badge/Kotlin-1.3.61-brightgreen.svg?style=flat)](https://github.com/popbrain/hellowork)
[![license](https://img.shields.io/badge/license-Apache2.0-green.svg?style=flat)](https://github.com/popbrain/hellowork)

# Hello Work

### Overview
HelloWork is a library for a library. Perhaps many will be used this by SDK developers. (BTW, "HelloWork" is Public Employment Security Offices in Japanese.)<br>
This is for Java and Android what can call modules from a module without the reflection implementation.

A base module is parent and another modules are child module if calls another modules from a base module.
It use to extend the function of the base module by child modules. The application that introduces the base module can use the extended functions of the base module by adding child modules to the dependencies as needed.
In that case, modules do not depend on each other by using HelloWork.

Base module is employer, child modules are the worker(employee).<br>
HelloWork finds a worker that matches employer's job.

It can use by the definition of some annotations and builder patterns.

### Download

Download a Jar using Gradle :

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.github.popbrain:hellowork:0.1.0'
}
```

Or download [the latest version Jar](https://search.maven.org/remote_content?g=com.github.popbrain&a=hellowork&v=LATEST) from maven central.

Or download a jar from the [release page](https://github.com/Popbrain/HelloWork/releases).

### Implementation

Please see [the details](./doc/implementation).

### License

```
Copyright (C) 2020 Popbrain aka Garhira.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```