plugins {
    application
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("stdlib", "1.3.21"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application.mainClassName = "org.gradle.buildeng.analysis.failures.FailureAnalyzerApp"
