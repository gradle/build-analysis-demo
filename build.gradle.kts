plugins {
    `build-scan`
    `kotlin-dsl`
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
}

configure(subprojects - project(":ratpack-client")) {
    apply(plugin = "org.gradle.kotlin.kotlin-dsl")

    kotlinDslPluginOptions {
        experimentalWarning.set(false)
    }
}
