plugins {
    `build-scan`
    `kotlin-dsl`
    kotlin("jvm") version "1.3.20"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
}

allprojects {
    group = "org.gradle.buildeng.analysis"
    version = "0.4.1"

    repositories {
        maven { url = uri("https://maven-central.storage.googleapis.com") }
        jcenter()
    }
}

subprojects {
    apply(plugin = "org.gradle.kotlin.kotlin-dsl")

    kotlinDslPluginOptions {
        experimentalWarning.set(false)
    }
}
