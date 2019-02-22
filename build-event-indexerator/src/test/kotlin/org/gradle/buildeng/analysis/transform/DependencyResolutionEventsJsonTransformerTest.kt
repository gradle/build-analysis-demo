package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class DependencyResolutionEventsJsonTransformerTest {
    @Test
    fun testTransformDependencyResolution() {
        val eventsFile = File(this::class.java.classLoader.getResource("dependency-resolution-json.txt").file)
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(DependencyResolutionEventsJsonTransformer().transform(eventsFile.readText()))

        assertEquals("build-analysis", jsonNode.get("rootProjectName").asText())
        assertEquals("obh7rlvvaedyu", jsonNode.get("buildId").asText())
        assertEquals("2019-01-16 21:32:03.671+00", jsonNode.get("buildTimestamp").asText())
        assertEquals(67, jsonNode.get("moduleDependencies").size())
        assertEquals(1, jsonNode.get("projectDependencies").size())
        assertEquals(0, jsonNode.get("unknownTypeDependencies").size())
        assertEquals(3, jsonNode.get("repositories").size())
        assertEquals(0, jsonNode.get("failureIds").size())
        assertEquals("", jsonNode.get("failures").asText())

        assertEquals(expectedOutput, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode))
    }

    private val expectedOutput = """{
  "rootProjectName" : "build-analysis",
  "buildId" : "obh7rlvvaedyu",
  "buildTimestamp" : "2019-01-16 21:32:03.671+00",
  "moduleDependencies" : [ {
    "group" : "com.google.apis",
    "module" : "google-api-services-storage",
    "version" : "v1-rev20181013-1.27.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-stdlib-jdk7",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.http-client",
    "module" : "google-http-client-jackson2",
    "version" : "1.27.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-test-annotations-common",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.guava",
    "module" : "guava",
    "version" : "26.0-android"
  }, {
    "group" : "com.google.code.findbugs",
    "module" : "jsr305",
    "version" : "3.0.2"
  }, {
    "group" : "com.google.protobuf",
    "module" : "protobuf-java",
    "version" : "3.6.1"
  }, {
    "group" : "com.fasterxml.jackson.core",
    "module" : "jackson-core",
    "version" : "2.9.6"
  }, {
    "group" : "com.google.api",
    "module" : "gax-httpjson",
    "version" : "0.52.0"
  }, {
    "group" : "com.google.http-client",
    "module" : "google-http-client",
    "version" : "1.27.0"
  }, {
    "group" : "com.fasterxml.jackson.core",
    "module" : "jackson-databind",
    "version" : "2.8.2"
  }, {
    "group" : "com.google.cloud",
    "module" : "google-cloud-bigquery",
    "version" : "1.55.0"
  }, {
    "group" : "com.google.apis",
    "module" : "google-api-services-bigquery",
    "version" : "v2-rev20181104-1.27.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-test-junit",
    "version" : "1.3.11"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-test",
    "version" : "1.3.11"
  }, {
    "group" : "commons-logging",
    "module" : "commons-logging",
    "version" : "1.2"
  }, {
    "group" : "com.fasterxml.jackson.core",
    "module" : "jackson-annotations",
    "version" : "2.8.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-test",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.api",
    "module" : "api-common",
    "version" : "1.7.0"
  }, {
    "group" : "com.google.code.gson",
    "module" : "gson",
    "version" : "2.7"
  }, {
    "group" : "io.grpc",
    "module" : "grpc-context",
    "version" : "1.12.0"
  }, {
    "group" : "org.checkerframework",
    "module" : "checker-compat-qual",
    "version" : "2.5.2"
  }, {
    "group" : "com.google.api",
    "module" : "api-common",
    "version" : "1.7.0"
  }, {
    "group" : "com.google.auth",
    "module" : "google-auth-library-oauth2-http",
    "version" : "0.12.0"
  }, {
    "group" : "io.opencensus",
    "module" : "opencensus-api",
    "version" : "0.15.0"
  }, {
    "group" : "org.jetbrains",
    "module" : "annotations",
    "version" : "13.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-stdlib",
    "version" : "1.3.11"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-sam-with-receiver",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.oauth-client",
    "module" : "google-oauth-client",
    "version" : "1.27.0"
  }, {
    "group" : "com.google.errorprone",
    "module" : "error_prone_annotations",
    "version" : "2.1.3"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-scripting-compiler-embeddable",
    "version" : "1.3.11"
  }, {
    "group" : "com.fasterxml.jackson.core",
    "module" : "jackson-core",
    "version" : "2.9.6"
  }, {
    "group" : "com.google.api.grpc",
    "module" : "proto-google-common-protos",
    "version" : "1.12.0"
  }, {
    "group" : "org.codehaus.mojo",
    "module" : "animal-sniffer-annotations",
    "version" : "1.14"
  }, {
    "group" : "com.google.protobuf",
    "module" : "protobuf-java-util",
    "version" : "3.6.1"
  }, {
    "group" : "com.google.api.grpc",
    "module" : "proto-google-common-protos",
    "version" : "1.12.0"
  }, {
    "group" : "org.hamcrest",
    "module" : "hamcrest-core",
    "version" : "1.3"
  }, {
    "group" : "com.google.guava",
    "module" : "guava",
    "version" : "26.0-android"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-script-runtime",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.api",
    "module" : "gax",
    "version" : "1.35.0"
  }, {
    "group" : "org.apache.httpcomponents",
    "module" : "httpclient",
    "version" : "4.5.5"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-stdlib-jdk8",
    "version" : "1.3.11"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-compiler-embeddable",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.cloud",
    "module" : "google-cloud-core",
    "version" : "1.55.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-test-common",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.http-client",
    "module" : "google-http-client-appengine",
    "version" : "1.27.0"
  }, {
    "group" : "com.google.api-client",
    "module" : "google-api-client",
    "version" : "1.27.0"
  }, {
    "group" : "com.google.cloud",
    "module" : "google-cloud-core-http",
    "version" : "1.55.0"
  }, {
    "group" : "joda-time",
    "module" : "joda-time",
    "version" : "2.9.2"
  }, {
    "group" : "io.opencensus",
    "module" : "opencensus-contrib-http-util",
    "version" : "0.15.0"
  }, {
    "group" : "com.google.guava",
    "module" : "guava",
    "version" : "26.0-android"
  }, {
    "group" : "com.google.auto.value",
    "module" : "auto-value",
    "version" : "1.4"
  }, {
    "group" : "org.apache.httpcomponents",
    "module" : "httpcore",
    "version" : "4.4.9"
  }, {
    "group" : "commons-codec",
    "module" : "commons-codec",
    "version" : "1.10"
  }, {
    "group" : "javax.annotation",
    "module" : "javax.annotation-api",
    "version" : "1.2"
  }, {
    "group" : "org.threeten",
    "module" : "threetenbp",
    "version" : "1.3.3"
  }, {
    "group" : "com.google.auth",
    "module" : "google-auth-library-oauth2-http",
    "version" : "0.12.0"
  }, {
    "group" : "com.google.protobuf",
    "module" : "protobuf-java",
    "version" : "3.6.1"
  }, {
    "group" : "junit",
    "module" : "junit",
    "version" : "4.12"
  }, {
    "group" : "com.google.api.grpc",
    "module" : "proto-google-iam-v1",
    "version" : "0.12.0"
  }, {
    "group" : "com.google.http-client",
    "module" : "google-http-client",
    "version" : "1.27.0"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-reflect",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.j2objc",
    "module" : "j2objc-annotations",
    "version" : "1.1"
  }, {
    "group" : "org.jetbrains.kotlin",
    "module" : "kotlin-stdlib-common",
    "version" : "1.3.11"
  }, {
    "group" : "com.google.auth",
    "module" : "google-auth-library-credentials",
    "version" : "0.12.0"
  }, {
    "group" : "com.google.cloud",
    "module" : "google-cloud-storage",
    "version" : "1.55.0"
  }, {
    "group" : "com.google.auth",
    "module" : "google-auth-library-credentials",
    "version" : "0.12.0"
  } ],
  "projectDependencies" : [ {
    "buildPath" : ":",
    "projectPath" : ":analysis-common"
  } ],
  "unknownTypeDependencies" : [ ],
  "repositories" : [ {
    "id" : "2312845989419396805",
    "name" : "maven",
    "type" : "MAVEN",
    "properties" : "{\"URL\":\"https://maven-central.storage.googleapis.com\",\"ARTIFACT_URLS\":[],\"AUTHENTICATED\":false,\"METADATA_SOURCES\":[\"mavenPom\",\"artifact\"],\"AUTHENTICATION_SCHEMES\":[]}"
  }, {
    "id" : "-1580266721305533123",
    "name" : "BintrayJCenter",
    "type" : "MAVEN",
    "properties" : "{\"URL\":\"https://jcenter.bintray.com/\",\"ARTIFACT_URLS\":[],\"AUTHENTICATED\":false,\"METADATA_SOURCES\":[\"mavenPom\",\"artifact\"],\"AUTHENTICATION_SCHEMES\":[]}"
  }, {
    "id" : "5335791236645149605",
    "name" : "Embedded Kotlin Repository",
    "type" : "MAVEN",
    "properties" : "{\"URL\":{\"path\":\"caches/5.1.1/embedded-kotlin-repo-1.3.11-2/repo\",\"root\":\"GRADLE_USER_HOME\"},\"ARTIFACT_URLS\":[],\"AUTHENTICATED\":false,\"METADATA_SOURCES\":[\"artifact\"],\"AUTHENTICATION_SCHEMES\":[]}"
  } ],
  "failureIds" : [ ],
  "failures" : ""
}""".trimIndent()
}
