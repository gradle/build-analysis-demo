import org.codehaus.groovy.tools.shell.util.Logger.io

plugins {
    `build-scan`
    `kotlin-dsl`
    application
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
}

repositories {
    jcenter()
}

dependencies {
    implementation("io.netty:netty-codec-http:4.1.5.Final")
    implementation("io.reactivex:rxnetty-common:0.5.2")
    implementation("io.reactivex:rxnetty-http:0.5.2")
    implementation("io.reactivex:rxjava:1.2.10")

    implementation("io.ratpack:ratpack-base:1.5.4")
    implementation("io.ratpack:ratpack-core:1.5.4")
    implementation("io.ratpack:ratpack-exec:1.5.4")
    implementation("io.ratpack:ratpack-guice:1.5.4")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")
    implementation("com.google.guava:guava:21.0")
    implementation("org.nield:kotlin-statistics:1.1.3")
    implementation("com.beust:jcommander:1.58")
    implementation("org.apache.commons:commons-lang3:3.8.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "org.gradle.buildeng.app.AppKt"
}
