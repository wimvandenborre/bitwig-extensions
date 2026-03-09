plugins {
    alias(libs.plugins.shadow)
}

group = "dev.gregross"
version = "0.1.0"

val bitwigExtensionsDir: String by project

// --- Main (extension) source set ---

dependencies {
    implementation(libs.java.websocket)
    implementation(libs.gson)

    testImplementation(libs.bitwig.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
