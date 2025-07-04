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
    id("java-application-conventions")
}

dependencies {
    implementation(libs.agrona)
    implementation(libs.aeron)
    implementation(libs.clusterStandby)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(project(":cluster"))
    testImplementation(libs.bundles.testing)
}

application {
    mainClass = "io.aeron.samples.ClusterStandbyApp"
}

tasks {
    register<Task>("runClusterStandby") {
        group = "run"
        dependsOn("run")
    }
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "adaptive"
                url = uri("https://weareadaptive.jfrog.io/artifactory/aeron-premium/")
                credentials(PasswordCredentials::class)
            }
        }
        filter {
            includeGroupAndSubgroups("io.aeron.premium")
        }
    }
}
