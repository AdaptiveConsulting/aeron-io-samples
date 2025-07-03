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
    java
    id("idea")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val generatedDir = project.layout.buildDirectory.dir("generated/src/main/java")
val codecGeneration = configurations.create("codecGeneration")

idea {
    module {
        generatedSourceDirs.add(generatedDir.get().getAsFile())
    }
}

dependencies {
    "codecGeneration"(libs.findLibrary("sbe").get())
}

sourceSets {
    main {
        java {
            srcDir(generatedDir)
        }
    }
}

tasks {
    register<JavaExec>("generateCodecs") {
        group = "sbe"
        val codecsFile = project.layout.projectDirectory.file("src/main/resources/protocol/protocol-codecs.xml")
        val sbeFile = project.layout.projectDirectory.file("src/main/resources/protocol/fpl/sbe.xsd")
        inputs.files(codecsFile, sbeFile)
        outputs.dir(generatedDir)
        classpath = codecGeneration
        mainClass.set("uk.co.real_logic.sbe.SbeTool")
        args = listOf(codecsFile.getAsFile().toString())
        systemProperties["sbe.output.dir"] = generatedDir.get().getAsFile().toString()
        systemProperties["sbe.target.language"] = "Java"
        systemProperties["sbe.validation.xsd"] = sbeFile.getAsFile().toString()
        systemProperties["sbe.validation.stop.on.error"] = "true"
        outputs.dir(generatedDir)
    }

    named("compileJava") {
        dependsOn("generateCodecs")
    }
}
