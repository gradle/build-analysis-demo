plugins {
    application
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("stdlib", "1.3.21"))
    implementation("io.netty:netty-codec-http:4.1.5.Final")
    implementation("io.reactivex:rxnetty-common:0.5.2")
    implementation("io.reactivex:rxnetty-http:0.5.2")
    implementation("io.reactivex:rxjava:1.2.10")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")
    implementation("com.google.cloud:google-cloud-pubsub:1.63.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "org.gradle.buildeng.analysis.producer.AppKt"
}
