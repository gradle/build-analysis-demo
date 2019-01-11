plugins {
    application
    `kotlin-dsl`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("stdlib", "1.3.11"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")
    implementation("com.google.cloud:google-cloud-storage:1.55.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "org.gradle.buildeng.analysis.transform.AppKt"
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
