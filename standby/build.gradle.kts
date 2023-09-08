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

tasks {
    task("runClusterStandby", JavaExec::class) {
        group = "run"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("io.aeron.samples.ClusterStandbyApp")
        jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
    }

    task ("uberJar", Jar::class) {
        group = "uber"
        manifest {
            attributes["Main-Class"]="io.aeron.samples.ClusterStandbyApp"
            attributes["Add-Opens"]="java.base/sun.nio.ch"
        }
        archiveClassifier.set("uber")
        from(sourceSets.main.get().output)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
}

repositories {
    maven {
        url = uri("https://weareadaptive.jfrog.io/artifactory/aeron-premium-virtual")
        credentials {
            username = providers.gradleProperty("artifactory_user").get()
            password = providers.gradleProperty("artifactory_password").get()
        }
        content {
            includeModule("io.aeron", "aeron-cluster-standby")
        }
    }
}