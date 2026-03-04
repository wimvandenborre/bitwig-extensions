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

// --- Main (extension) source set ---

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

// --- CLI source set ---

sourceSets {
    create("cli") {
        java.srcDir("src/cli/java")
    }
}

val cliImplementation by configurations.getting
val cliRuntimeOnly by configurations.getting

dependencies {
    cliImplementation(libs.picocli)
    cliImplementation(libs.gson)

    // CLI classes available in tests
    testImplementation(sourceSets["cli"].output)
    testImplementation(libs.picocli)
}

tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("cliShadowJar") {
    from(sourceSets["cli"].output)
    configurations = listOf(project.configurations["cliRuntimeClasspath"])
    archiveFileName.set("gig-cli.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    manifest {
        attributes("Main-Class" to "dev.gregross.gig.cli.GigCli")
    }
    mergeServiceFiles()
}
