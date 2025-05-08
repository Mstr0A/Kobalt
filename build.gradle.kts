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

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
    maven { url = uri("https://m2.chew.pro/releases") }
}

val jdaVersion: String = "5.4.0"
val utilsVersion: String = "2.1"
val loggingVersion: String = "7.0.4"
val loggingAPIVersion: String = "2.0.17"
val logbackVersion: String = "1.5.16"


////////////////////////////////////
//                                //
//    Dependency Configuration    //
//                                //
////////////////////////////////////


dependencies {

    /* External dependencies */
    api("net.dv8tion:JDA:$jdaVersion")
    api("org.slf4j:slf4j-api:$loggingAPIVersion")

    /* Internal dependencies */
    implementation("pw.chew:jda-chewtils:$utilsVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(kotlin("reflect"))
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")

    /* Testing */
    testImplementation(kotlin("test"))
}


////////////////////////////////////
//                                //
//    GeneralTask Configuration   //
//                                //
////////////////////////////////////


tasks {
    shadowJar {
        archiveBaseName.set("Kobalt")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        manifest.attributes["Main-Class"] = "com.a0.kobalt.MainKt"
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