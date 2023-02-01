plugins {
    `java-library`
}

val generatedDir = file("${buildDir}/generated/src/main/java")
val codecGeneration = configurations.create("codecGeneration")

dependencies {
    "codecGeneration"(libs.sbe)
    implementation(libs.agrona)
}

sourceSets {
    main {
        java.srcDir(generatedDir)
    }
}

tasks {
    task("generateCodecs", JavaExec::class) {
        group = "sbe"
        val codecsFile = "src/main/resources/protocol/protocol-codecs.xml"
        val sbeFile = "src/main/resources/protocol/fpl/sbe.xsd"
        inputs.files(codecsFile, sbeFile)
        outputs.dir(generatedDir)
        classpath = codecGeneration
        mainClass.set("uk.co.real_logic.sbe.SbeTool")
        args = listOf(codecsFile)
        systemProperties["sbe.output.dir"] = generatedDir
        systemProperties["sbe.target.language"] = "Java"
        systemProperties["sbe.validation.xsd"] = sbeFile
        systemProperties["sbe.validation.stop.on.error"] = "true"
        outputs.dir(generatedDir)
    }

    compileJava {
        dependsOn("generateCodecs")
    }
}