plugins {
    kotlin("jvm")
    application
    `maven-publish`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.netty:netty-codec-http")
    implementation("io.reactivex:rxnetty-common")
    implementation("io.reactivex:rxnetty-http")
    implementation("io.reactivex:rxjava:1.2.10")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.google.cloud:google-cloud-storage")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application.mainClassName = "org.gradle.buildeng.analysis.consumer.AppKt"

publishing {
    repositories {
        maven {
            url = uri("gcs://gradle-build-analysis-apps/maven2")
            // NOTE: Credentials for Google Cloud are sourced from well-known files or env variables
            //   See https://docs.gradle.org/current/userguide/repository_types.html#sub:supported_transport_protocols
        }
    }
    publications.withType<MavenPublication> {
        artifact(tasks.distZip.get())
    }
}
