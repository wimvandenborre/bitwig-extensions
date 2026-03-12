group = "com.gregross.bitwig"
version = "0.1.0"

val bitwigExtensionsDir: String by project

dependencies {
    testImplementation(libs.bitwig.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    archiveFileName.set("LaunchpadMk2.bwextension")
}

tasks.register<Copy>("install") {
    description = "Installs the extension to the Bitwig Studio Extensions directory"
    from(tasks.named("jar"))
    into(bitwigExtensionsDir)
}
