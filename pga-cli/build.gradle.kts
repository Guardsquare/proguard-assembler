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

//tasks.jar {
//    dependsOn(":pga-lib:jar")
//    manifest({
//        Attribute.of("Main-Class", application.mainClass)
//    })
//    from(
//        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
//    )
//    archiveFileName = "assembler.jar"
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}
//
//tasks.register("copyBuild", Copy) {
//    dependsOn(tasks.jar)
//    tasks.assemble.dependsOn(it)
//
//    from(tasks.jar.outputs)
//    into(file("$rootDir/lib"))
//}

//distributions {
//    main {
//        distributionBaseName.set("proguard-assembler")
//        contents({
//            into("$rootDir/lib") {
//                from(jar.outputs)
//            }
//            into("docs") {
//                from("$rootDir/docs/md") {
//                    includeEmptyDirs = false
//                    include "**/*.md"
//                }
//            }
//            from(rootDir) {
//                include("$rootDir/bin/")
//                include("LICENSE")
//            }
//        })
//    }
//}
//
//distTar {
//    compression = Compression.GZIP
//    archiveExtension.set("tar.gz")
//}
//
//clean {
//    delete(file("$rootDir/lib"))
//}
