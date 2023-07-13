plugins {
    java
    `java-library`
    // `maven-publish`
    signing
    // id("org.jetbrains.kotlin.jvm") version "1.8.20"
    id("com.adarshr.test-logger")
    jacoco
    id("org.jlleitschuh.gradle.ktlint")
    `java-test-fixtures`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api("com.guardsquare:proguard-core:9.0.9")

    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
    testImplementation("dev.zacsweers.kctfork:core:0.3.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:5.6.2") // for kotest property test
    testImplementation("io.mockk:mockk:1.13.5") // for mocking

//    testImplementation(testFixtures("com.guardsquare:proguard-core:9.0.9")) {
//        exclude(group = "com.guardsquare", module ="proguard-core")
//    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    // Define which classes need to be monitored
    val sources = files(sourceSets.map { it.allSource.srcDirs })
    sourceDirectories.setFrom(sources)
    additionalSourceDirs.setFrom(sources)
    val classes = files(sourceSets.main.map { it.output.classesDirs })
    classDirectories.setFrom(classes)
    executionData.setFrom(project.fileTree(Pair(".", "**/build/jacoco/*.exec")))
    reports {
        xml.required = true
        csv.required = true
        html.outputLocation.set(layout.buildDirectory.dir("$buildDir/reports/coverage"))
    }
}

// publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            groupId = "com.guardsquare"
//            artifactId = "proguard-assembler"
//            version = version
//            pom {
//                name = "ProGuard Assembler and Disassembler"
//                description = "The ProGuard Assembler and Disassembler can assemble and disassemble Java class files."
//                url = "https://github.com/Guardsquare/kotlin-metadata-printer"
//                licenses {
//                    license {
//                        name = "Apache License Version 2.0"
//                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
//                        distribution = "repo"
//                    }
//                }
//                issueManagement {
//                    system = "Github Tracker"
//                    url = "https://github.com/Guardsquare/proguard-assembler/issues"
//                }
//                scm {
//                    url = "https://github.com/Guardsquare/proguard-assembler.git"
//                    connection = "scm:git:https://github.com/Guardsquare/proguard-assembler.git"
//                }
//                developers {
//                    developer {
//                        id = "james.hamilton"
//                        name = "James Hamilton"
//                        organization = "Guardsquare"
//                        organizationUrl = "https://www.guardsquare.com/"
//                        roles.set(arrayOf("Project Administrator", "Developer").asIterable())
//                    }
//                }
//            }
//
//            from(components["java"])
//
//            if (project.hasProperty("PROGUARD_SIGNING_KEY") && project.hasProperty("PROGUARD_SIGNING_PASSWORD")) {
//                // We use in-memory ascii-armored keys
//                // See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys
//                signing {
//                    val signingKey = project.findProperty("PROGUARD_SIGNING_KEY") as String
//                    val signingPassword = project.findProperty("PROGUARD_SIGNING_PASSWORD") as String
//                    useInMemoryPgpKeys(signingKey, signingPassword)
//                    sign(publishing.publications["maven"])
//                }
//            }
//        }
//    }
// }
