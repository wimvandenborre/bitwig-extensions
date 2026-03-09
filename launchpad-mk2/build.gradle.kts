group = "com.gregross.bitwig"
version = "0.1.0"

val bitwigExtensionsDir: String by project

tasks.named<Jar>("jar") {
    archiveFileName.set("LaunchpadMk2.bwextension")
}

tasks.register<Copy>("install") {
    description = "Installs the extension to the Bitwig Studio Extensions directory"
    from(tasks.named("jar"))
    into(bitwigExtensionsDir)
}
