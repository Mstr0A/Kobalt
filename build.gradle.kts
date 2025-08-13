plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.6"
    `maven-publish`
    signing
}

// /////////////////////////////////
//                                //
//     Project Configuration      //
//                                //
// /////////////////////////////////

group = "com.a0"
version = "0.0.2"

java {
    withJavadocJar()
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
}

// /////////////////////////////////
//                                //
//    Dependency Configuration    //
//                                //
// /////////////////////////////////

// Centralize the version management
val jdaVersion: String = "5.6.1"
val loggingVersion: String = "7.0.7"
val loggingAPIVersion: String = "2.0.17"
val logbackVersion: String = "1.5.18"

dependencies {

    // External dependencies
    api("net.dv8tion:JDA:$jdaVersion")

    // Internal dependencies
    implementation("org.slf4j:slf4j-api:$loggingAPIVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(kotlin("reflect"))

    // Testing
    testImplementation(kotlin("test"))
}

// /////////////////////////////////
//                                //
//    GeneralTask Configuration   //
//                                //
// /////////////////////////////////

tasks {
    // Have shadowJar setup just in case
    shadowJar {
        archiveBaseName.set("Kobalt")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        exclude(
            "logback.xml",
        )
    }

    test {
        useJUnitPlatform()
    }
}

// /////////////////////////////////
//                                //
//    Publishing And Signing      //
//                                //
// /////////////////////////////////

// Currently only publish to maven local
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "Kobalt"
        }
    }
    repositories {
        mavenLocal()
    }
}
