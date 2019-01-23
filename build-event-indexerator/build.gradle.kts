plugins {
    `maven-publish`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("reflect", "1.3.11"))
    implementation(kotlin("stdlib-jdk8", "1.3.11"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")

    implementation("org.apache.beam:beam-sdks-java-core:2.9.0")
    implementation("org.apache.beam:beam-runners-direct-java:2.9.0")
    implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java:2.9.0")
    implementation("com.google.cloud:google-cloud-bigquery:1.55.0")
    implementation("com.google.cloud:google-cloud-storage:1.55.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks {
    // TODO: custom task type for Dataflow jobs
    register<JavaExec>("indexTaskEvents") {
        main = "org.gradle.buildeng.analysis.indexing.TaskEventsIndexer"
        classpath = sourceSets["main"].runtimeClasspath

        doFirst {
            println("* main job class  : $main")
            println("* pipeline options: \n${args?.joinToString("\n")}")
        }
        dependsOn("compileKotlin")
    }

    register<JavaExec>("indexTestEvents") {
        main = "org.gradle.buildeng.analysis.indexing.TestEventsIndexer"
        classpath = sourceSets["main"].runtimeClasspath

        doFirst {
            println("* main job class  : $main")
            println("* pipeline options: \n${args?.joinToString("\n")}")
        }
        dependsOn("compileKotlin")
    }

    register<JavaExec>("indexExceptionEvents") {
        main = "org.gradle.buildeng.analysis.indexing.ExceptionEventsIndexer"
        classpath = sourceSets["main"].runtimeClasspath

        doFirst {
            println("* main job class  : $main")
            println("* pipeline options: \n${args?.joinToString("\n")}")
        }
        dependsOn("compileKotlin")
    }
}
