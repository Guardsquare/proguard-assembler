plugins {
    distribution
    java
    application
    `maven-publish`
    signing
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(project(":pga-lib"))
}

application {
    mainClass = "com.guardsquare.proguard.assembler.AssemblerCli"
}

tasks.jar {
    dependsOn(":pga-lib:jar")
    manifest {
        Attribute.of("Main-Class", application.mainClass.javaClass)
    }
    from(
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) },
    )
    archiveFileName.set("assembler.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("copyBuild") {
    dependsOn(tasks.jar)
    tasks.assemble.get().dependsOn(this)

    from(tasks.jar.get().outputs)
    into(file("$rootDir/lib"))
}

distributions {
    main {
        distributionBaseName.set("proguard-assembler")
        contents {
            into("$rootDir/lib") {
                from(tasks.jar.get().outputs)
            }
            into("docs") {
                from("$rootDir/docs/md") {
                    includeEmptyDirs = false
                    include("**/*.md")
                }
            }
            from(rootDir) {
                include("$rootDir/bin/")
                include("LICENSE")
            }
        }
    }
}

tasks.distTar {
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
}

tasks.clean {
    delete(file("$rootDir/lib"))
}
