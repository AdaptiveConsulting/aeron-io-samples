
plugins {
    application
    checkstyle
}

    dependencies {
    checkstyle(libs.checkstyle)
    implementation(libs.agrona)
    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(libs.picocli)
    implementation(libs.jline)
    implementation(libs.picoJline)
    implementation(project(":cluster-protocol"))
}

application {
    mainClass.set("io.aeron.samples.admin.Admin")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter(libs.versions.junitVersion.get())
        }
    }
}

tasks {

    task("createParticipant", JavaExec::class) {
        group = "run"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("io.aeron.samples.admin.Admin")
        args("-participant-id=1", "-participant-name=adaptive1")
        jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
    }

    task ("uberJar", Jar::class) {
        group = "uber"
        manifest {
            attributes["Main-Class"]="io.aeron.samples.admin.Admin"
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