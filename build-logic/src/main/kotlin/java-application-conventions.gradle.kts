/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("java-base-convention")
    application
    id("com.gradleup.shadow")
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes("Add-Opens" to "java.base/jdk.internal.misc java.base/java.util.zip")
        }
    }

    named<Jar>("shadowJar") {
        archiveClassifier = "uber"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // Disable Application Distributions at this time.
    named("distTar") {
        enabled = false
    }
    named("distZip") {
        enabled = false
    }
    named("shadowDistTar") {
        enabled = false
    }
    named("shadowDistZip") {
        enabled = false
    }
}
