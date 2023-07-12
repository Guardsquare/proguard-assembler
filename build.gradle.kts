plugins {
    `java-gradle-plugin`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
    id("com.adarshr.test-logger") version "3.2.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0" apply false
    `java-test-fixtures`
}

allprojects {
    group = "com.guardsquare"
    version = "1.0.0"
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

nexusPublishing {
    repositories.sonatype {
        val name = findProperty("PROGUARD_STAGING_USERNAME")
        if (name != null) {
            username.set(name as String)
        }
        val pass = findProperty("PROGUARD_STAGING_PASSWORD")
        if (pass != null) {
            password.set(pass as String)
        }
    }
}
