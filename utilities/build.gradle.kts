plugins {
    `java-library`
    checkstyle
}

repositories {
    mavenCentral()
}

dependencies {
    checkstyle(libs.checkstyle)
}