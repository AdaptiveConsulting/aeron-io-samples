plugins {
    java
    checkstyle
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

checkstyle {
    maxWarnings = 0
}

group = "io.aeron"
version = "0.0.1-SNAPSHOT"

defaultTasks("check", "build")

allprojects {
    repositories {
        mavenCentral()
    }
}