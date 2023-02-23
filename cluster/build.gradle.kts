/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

plugins {
    application
    checkstyle
}

repositories {
    mavenCentral()
}

dependencies {
    checkstyle(libs.checkstyle)
    implementation(libs.agrona)
    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(project(":cluster-protocol"))
    testImplementation(libs.bundles.testing)
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
    task("runSingleNodeCluster", JavaExec::class) {
        group = "run"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("io.aeron.samples.ClusterApp")
        jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
    }


    task ("uberJar", Jar::class) {
        group = "uber"
        manifest {
            attributes["Main-Class"]="io.aeron.samples.ClusterApp"
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
