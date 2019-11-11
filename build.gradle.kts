plugins {
    `build-scan`
    kotlin("jvm").version("1.3.50").apply(false)
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
}

allprojects {
    group = "org.gradle.buildeng.analysis"
    version = "0.6.3"

    repositories {
        maven { url = uri("https://maven-central.storage.googleapis.com") }
        jcenter()
    }

    // Get DRY with dependency versions
    configurations.all {
        resolutionStrategy {
            eachDependency {
                default("org.jetbrains.kotlin", "1.3.50")
                default("org.apache.beam", "2.10.0")
                default("com.google.cloud", "1.63.0")
                default("com.fasterxml.jackson.core", "2.8.2")
                default("io.netty", "4.1.5.Final")
                default("io.reactivex", "0.5.2")
            }
        }
    }
}

fun DependencyResolveDetails.default(group: String, version: String) {
    if(requested.group == group && requested.version.isNullOrBlank()) {
        useVersion(version)
        because("Selected default as no other version was requested")
    }
}
