plugins {
    id("java-application-conventions")
}

dependencies {
    implementation(platform(libs.awssdk.bom))
    implementation(libs.awssdk.licensemanager)
}

application {
    mainClass = "io.aeron.samples.aws.mp.LicenseManagerCheck"
}