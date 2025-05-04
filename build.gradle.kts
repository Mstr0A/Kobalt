plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.6" // we use shadowJar to package the library here
}

private val jdaVersion: String = "5.4.0" // to make changing the JDA version easier
private val utilsVersion: String = "2.1" // to make changing the utils version easier
private val loggingVersion: String = "7.0.4" // to make changing the logging version easier
private val loggingAPIVersion: String = "2.0.17" // to make changing the logging API version easier
private val logbackVersion: String = "1.5.16" // to make changing the logging API version easier

group = "com.a0.kobalt"
version = "0.0.1"

repositories {
    mavenCentral()

    maven { url = uri("https://m2.chew.pro/releases") }
}

dependencies {
    // Main dependencies
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("pw.chew:jda-chewtils:$utilsVersion")
    implementation("org.slf4j:slf4j-api:$loggingAPIVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")

    // Kotlin utilities
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Test dependencies (keep even if unused)
    testImplementation(kotlin("test"))
}

tasks {
    // to specify the main entry point to the main file
    jar {
        manifest.attributes["Main-Class"] = "com.a0.kobalt.MainKt"
    }

    // to exclude the library file from getting compiled into the final version
    shadowJar {
        archiveBaseName.set("Kobalt")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        dependencies {
            exclude(dependency("net.dv8tion:JDA:.*"))
            exclude(dependency("pw.chew:jda-chewtils:.*"))
            exclude(dependency("org.slf4j:slf4j-api:.*"))
            exclude(dependency("ch.qos.logback:logback-classic:.*"))
            exclude(dependency("io.github.oshai:kotlin-logging-jvm:.*"))
        }
        exclude(
            "net/dv8tion/**",
            "pw/chew/**",
            "com/jagrosh/**",
            "org/slf4j/**",
            "ch/qos/logback/**",
            "io/github/oshai/**",
            "com/a0/kobalt/ExampleKt*",
            "logback.xml"
        )
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(21)
}