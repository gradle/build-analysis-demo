import org.gradle.internal.impldep.aQute.bnd.osgi.Constants.options

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

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

// ./gradlew runJob --args="--runner=DirectRunner --project=build-analysis --output=gs://gradle-build-events/transformed"

// TODO: configure publishing? so stuff isn't just invoked locally
//publishing {
//    repositories {
//        maven {
//            url = uri("gcs://gradle-build-analysis-apps/maven2")
//        }
//    }
//    publications.withType<MavenPublication> {
//        artifact(tasks.distZip.get())
//    }
//}

tasks {
    register<JavaExec>("runJob") {
        val mainClassStr = "org.gradle.buildeng.analysis.transform.TaskEventsTransformer"
        val options = this.args?.joinToString("\n")

        main = mainClassStr
        classpath = sourceSets["main"].runtimeClasspath

        doFirst {
            println("* main job class  : $mainClassStr")
            println("* pipeline options: \n$options\n")
        }
        dependsOn("compileKotlin")
    }
}
