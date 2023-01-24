plugins {
    `application`
    checkstyle
}

repositories {
    mavenCentral()
}

dependencies {
    checkstyle(libs.checkstyle)
    implementation(libs.agrona)
    implementation(libs.aeron)
    implementation(libs.sbe)
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

