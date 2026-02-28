plugins {
    java
    alias(libs.plugins.shadow)
}

group = "dev.gregross"
version = "0.1.0"

val bitwigApiPath: String by project
val bitwigExtensionsDir: String = System.getProperty("user.home") +
    "/Documents/Bitwig Studio/Extensions"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(bitwigApiPath))
    testImplementation(files(bitwigApiPath))
    implementation(libs.java.websocket)
    implementation(libs.gson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveFileName.set("GigMaestro.bwextension")
    destinationDirectory.set(file(bitwigExtensionsDir))
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
