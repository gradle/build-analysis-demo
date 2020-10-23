plugins {
    `kotlin-dsl`
}

allprojects {
    group = "org.gradle.buildeng.analysis"
    version = "0.7.0"

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
