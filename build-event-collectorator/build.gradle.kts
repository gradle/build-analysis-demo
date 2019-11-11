plugins {
    application
    `maven-publish`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("stdlib", "1.3.21"))
    implementation("io.netty:netty-codec-http:4.1.5.Final")
    implementation("io.reactivex:rxnetty-common:0.5.2")
    implementation("io.reactivex:rxnetty-http:0.5.2")
    implementation("io.reactivex:rxjava:1.2.10")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")
    implementation("com.google.cloud:google-cloud-storage:1.63.0")

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
