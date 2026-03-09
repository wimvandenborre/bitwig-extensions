plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "bitwig-extensions"

include("gig-maestro", "launchpad-mk2")
