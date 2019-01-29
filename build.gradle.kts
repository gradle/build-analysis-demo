plugins {
    `build-scan`
    `kotlin-dsl`
    id("org.gradle.kotlin.kotlin-dsl.precompiled-script-plugins") version "1.1.3"
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
    apply<org.gradle.kotlin.dsl.plugins.precompiled.PrecompiledScriptPlugins>()
// TODO:    apply(plugin = "dataflow-exec")

    kotlinDslPluginOptions {
        experimentalWarning.set(false)
    }
}
