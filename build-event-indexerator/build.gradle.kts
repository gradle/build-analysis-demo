plugins {
    `maven-publish`
}

dependencies {
    implementation(project(":analysis-common"))
    implementation(kotlin("reflect", "1.3.20"))
    implementation(kotlin("stdlib-jdk8", "1.3.20"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.2")

    implementation("org.apache.beam:beam-sdks-java-core:2.9.0")
    implementation("org.apache.beam:beam-runners-direct-java:2.9.0")
    implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java:2.9.0")
    implementation("com.google.cloud:google-cloud-bigquery:1.55.0")
    implementation("com.google.cloud:google-cloud-storage:1.55.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

open class DataflowExec : JavaExec() {
    @TaskAction
    fun doExec() {
        println("* main job class  : $main")
        println("* pipeline options: \n${args?.joinToString("\n")}")
        super.exec()
    }
}

// TODO: Create default args instead of specifying all on CLI
// TODO: Different rules for different runners
tasks.addRule("Pattern: index<EventType>Events") {
    val taskName = this
    if (startsWith("index") && endsWith("Events")) {
        val eventTypes = taskName.removePrefix("index").removeSuffix("Events")
        tasks.register(taskName, DataflowExec::class) {
            main = "org.gradle.buildeng.analysis.indexing.${eventTypes}EventsIndexer"
            classpath = project.the<SourceSetContainer>()["main"].runtimeClasspath
            dependsOn("compileKotlin")
        }
    }
}
