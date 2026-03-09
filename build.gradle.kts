subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.bitwig.com")
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        "compileOnly"(rootProject.libs.bitwig.api)
    }
}
