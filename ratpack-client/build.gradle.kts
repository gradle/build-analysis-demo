plugins {
    java
    application
}

dependencies {
    compile(fileTree("libs"))
    compile("com.beust:jcommander:1.58")
    compile("io.ratpack:ratpack-exec:1.5.4")
    compile("io.ratpack:ratpack-core:1.5.4")
    compile("org.slf4j:slf4j-api:1.7.23")
    compile("org.apache.commons:commons-lang3:3.5")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.4")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.4")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.4")
    compile("com.fasterxml.jackson.module:jackson-module-parameter-names:2.9.4")
    runtime("org.slf4j:slf4j-simple:1.7.23")
}

application.mainClassName = "com.gradle.export.client.ExportClientMain"