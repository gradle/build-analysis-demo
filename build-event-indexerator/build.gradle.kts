plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.apache.beam:beam-sdks-java-core")
    implementation("org.apache.beam:beam-runners-direct-java")
    implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java")
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-storage")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.addRule("Pattern: index<EventType>Events") {
    val taskName = this
    if (startsWith("index") && endsWith("Events")) {
        val eventTypes = taskName.removePrefix("index").removeSuffix("Events")
        tasks.register(taskName, JavaExec::class) {
            doFirst {
                println("* main job class  : $main")
                println("* pipeline options: \n${args?.joinToString("\n")}")
            }
            main = "org.gradle.buildeng.analysis.indexing.${eventTypes}EventsIndexer"
            classpath = project.the<SourceSetContainer>()["main"].runtimeClasspath

            dependsOn("compileKotlin")
        }
    }
}
