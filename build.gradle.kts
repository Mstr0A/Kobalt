plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
//    signing // commented out as it is not needed as of now
}

// /////////////////////////////////
//                                //
//     Project Configuration      //
//                                //
// /////////////////////////////////

group = "com.a0"
version = "0.0.21"

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

dependencies {

    // External dependencies
    api(libs.jda)

    // Internal dependencies
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(kotlin("reflect"))

    // Testing
    testImplementation(kotlin("test"))
}

// /////////////////////////////////
//                                //
//    GeneralTask Configuration   //
//                                //
// /////////////////////////////////

tasks.test {
    useJUnitPlatform()
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
            artifactId = "kobalt"

            pom {
                name.set("Kobalt")
                description.set("A Kotlin wrapper library for building Discord bots with JDA")
                url.set("https://github.com/Mstr0A/Kobalt")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("a0")
                        name.set("Ameen")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Mstr0A/Kobalt.git")
                    developerConnection.set("scm:git:ssh://github.com/Mstr0A/Kobalt.git")
                    url.set("https://github.com/Mstr0A/Kobalt")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}
