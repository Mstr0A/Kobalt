plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.6"
    `maven-publish`
    signing
}

////////////////////////////////////
//                                //
//     Project Configuration      //
//                                //
////////////////////////////////////


group = "com.a0"
version = "0.0.1"

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
    maven { url = uri("https://m2.chew.pro/releases") }
}


////////////////////////////////////
//                                //
//    Dependency Configuration    //
//                                //
////////////////////////////////////


// Centralize the version management
val jdaVersion: String = "5.5.1"
val utilsVersion: String = "2.1"
val loggingVersion: String = "7.0.7"
val loggingAPIVersion: String = "2.0.17"
val logbackVersion: String = "1.5.18"


dependencies {

    /* External dependencies */
    api("net.dv8tion:JDA:$jdaVersion")
    api("org.slf4j:slf4j-api:$loggingAPIVersion")

    /* Internal dependencies */
    implementation("pw.chew:jda-chewtils:$utilsVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(kotlin("reflect"))

    /* Testing */
    testImplementation(kotlin("test"))
}


////////////////////////////////////
//                                //
//    GeneralTask Configuration   //
//                                //
////////////////////////////////////


tasks {
    // Have shadowJar setup just in case
    shadowJar {
        archiveBaseName.set("Kobalt")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        manifest.attributes["Main-Class"] = "com.a0.kobalt.MainKt"

        exclude(
            "logback.xml"
        )
    }

    test {
        useJUnitPlatform()
    }
}


////////////////////////////////////
//                                //
//    Publishing And Signing      //
//                                //
////////////////////////////////////


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