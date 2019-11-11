package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildEventsJsonTransformerTest {
    @Test
    fun testTransformGradleBuild() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("gradle-build-events-json.txt").file)
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(BuildEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("gradle", jsonNode.get("rootProjectName").asText())
        assertEquals("f23vwoax6n4uy", jsonNode.get("buildId").asText())
        assertEquals("5.1-rc-3", jsonNode.get("buildToolVersion").asText())
        assertEquals("tcagent1@windows25", jsonNode.get("buildAgentId").asText())
        assertEquals("2019-01-01 23:02:51.267+00", jsonNode.get("buildTimestamp").asText())
        assertEquals(15407, jsonNode.get("wallClockDuration").asInt())
        assertEquals(0, jsonNode.get("failureIds").size())
        assertEquals(false, jsonNode.get("failed").asBoolean())
        assertEquals(2, jsonNode.get("buildRequestedTasks").size())
        assertEquals(0, jsonNode.get("buildExcludedTasks").size())
        assertEquals(11, jsonNode.get("environmentParameters").size())
        assertEquals(5, jsonNode.get("userLink").size())
        assertEquals(15, jsonNode.get("userNamedValue").size())
        assertEquals(7, jsonNode.get("userTag").size())

        assertEquals(expectedOutput, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode))
    }

    @Test
    fun testTransformMavenBuild() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("maven-failed-json.txt").file)
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(BuildEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("timeline-failed-goals", jsonNode.get("rootProjectName").asText())
        assertEquals("qjxkgdbcuaac2", jsonNode.get("buildId").asText())
        assertEquals("3.6.1", jsonNode.get("buildToolVersion").asText())
        assertEquals("tcagent1@dev31.gradle.org", jsonNode.get("buildAgentId").asText())
        assertEquals("2019-06-03 07:26:10.170+00", jsonNode.get("buildTimestamp").asText())
        assertEquals(1740, jsonNode.get("wallClockDuration").asInt())
        assertTrue(jsonNode.get("failed").asBoolean())
        assertEquals(2, jsonNode.get("failureIds").size())
        assertEquals("6929713202549594971", jsonNode.get("failureIds")[0].asText())
        assertEquals("-4272005080405076924", jsonNode.get("failureIds")[1].asText())
        assertEquals(2, jsonNode.get("buildRequestedTasks").size())
        assertEquals(0, jsonNode.get("buildExcludedTasks").size())
        assertEquals(8, jsonNode.get("environmentParameters").size())
        assertEquals(0, jsonNode.get("userLink").size())
        assertEquals(3, jsonNode.get("userNamedValue").size())
        assertEquals(0, jsonNode.get("userTag").size())

        assertEquals("VERIFICATION", jsonNode.get("failureData").get("category").asText())
        assertEquals("org.apache.maven.plugin.compiler.CompilationFailureException", jsonNode.get("failureData").get("causes")[0].get("exceptionClassName").asText())
        assertEquals("compiler:compile", jsonNode.get("failureData").get("causes")[1].get("failedTaskGoalName").asText())
        assertEquals("org.apache.maven.plugin.compiler.CompilerMojo", jsonNode.get("failureData").get("causes")[1].get("failedTaskTypeOrMojoClassName").asText())

        assertEquals(expectedOutputMaven, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode))
    }

    @Test
    fun testTransformGradleBuildWithFailure() {
        val buildEventsFile = File(this::class.java.classLoader.getResource("failing-test-events-json.txt").file)
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(BuildEventsJsonTransformer().transform(buildEventsFile.readText()))

        assertEquals("build-analysis", jsonNode.get("rootProjectName").asText())
        assertEquals("cokuz2qlzlhck", jsonNode.get("buildId").asText())
        assertEquals(1, jsonNode.get("failureIds").size())
        assertEquals("3107853242888327571", jsonNode.get("failureIds")[0].asText())
        assertEquals(true, jsonNode.get("failed").asBoolean())
        assertEquals("VERIFICATION", jsonNode.get("failureData").get("category").asText())
        assertTrue(jsonNode.get("failureData").get("causes").isArray)
        assertEquals(4, jsonNode.get("failureData").get("causes").size())
        assertEquals(":build-event-transformerator:test", jsonNode.get("failureData").get("causes")[2].get("failedTaskGoalName").asText())
        assertEquals("org.junit.ComparisonFailure", jsonNode.get("failureData").get("causes")[0].get("exceptionClassName").asText())
        assertEquals("org.gradle.api.tasks.testing.Test", jsonNode.get("failureData").get("causes")[2].get("failedTaskTypeOrMojoClassName").asText())
        assertEquals("org.gradle.api.GradleException", jsonNode.get("failureData").get("causes")[3].get("exceptionClassName").asText())
    }

    private val expectedOutput = """{
  "buildId" : "f23vwoax6n4uy",
  "rootProjectName" : "gradle",
  "buildTool" : "Gradle",
  "buildToolVersion" : "5.1-rc-3",
  "buildAgentId" : "tcagent1@windows25",
  "buildRequestedTasks" : [ "clean", "baseServicesGroovy:platformTest" ],
  "buildExcludedTasks" : [ ],
  "environmentParameters" : [ {
    "key" : "DaemonState",
    "value" : "{\"startTime\":1546381961320,\"buildNumber\":49,\"numberOfRunningDaemons\":1,\"idleTimeout\":10800000,\"singleUse\":false}"
  }, {
    "key" : "BuildModes",
    "value" : "{\"refreshDependencies\":false,\"parallelProjectExecution\":true,\"rerunTasks\":false,\"continuous\":false,\"continueOnFailure\":true,\"configureOnDemand\":false,\"daemon\":true,\"offline\":false,\"dryRun\":false,\"maxWorkers\":4,\"taskOutputCache\":true}"
  }, {
    "key" : "Hardware",
    "value" : "{\"numProcessors\":4}"
  }, {
    "key" : "Os",
    "value" : "{\"family\":\"windows\",\"name\":\"Windows 7\",\"version\":\"6.1\",\"arch\":\"amd64\"}"
  }, {
    "key" : "Jvm",
    "value" : "{\"version\":\"11.0.1\",\"vendor\":\"Oracle Corporation\",\"runtimeName\":\"OpenJDK Runtime Environment\",\"runtimeVersion\":\"11.0.1+13\",\"classVersion\":\"55.0\",\"vmInfo\":\"mixed mode\",\"vmName\":\"OpenJDK 64-Bit Server VM\",\"vmVersion\":\"11.0.1+13\",\"vmVendor\":\"Oracle Corporation\"}"
  }, {
    "key" : "JvmArgs",
    "value" : "{\"effective\":[\"-XX:MaxMetaspaceSize=512m\",\"-XX:+HeapDumpOnOutOfMemoryError\",\"--add-opens=java.base/java.util=ALL-UNNAMED\",\"--add-opens=java.base/java.lang=ALL-UNNAMED\",\"--add-opens=java.base/java.lang.invoke=ALL-UNNAMED\",\"--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED\",\"-Xmx2500m\",\"-Dfile.encoding=UTF-8\",\"-Djava.io.tmpdir=C:\\\\tcagent1\\\\temp\\\\buildTmp\",\"-Duser.country=US\",\"-Duser.language=en\",\"-Duser.variant\"]}"
  }, {
    "key" : "Locality",
    "value" : "{\"localeLanguage\":\"en\",\"localeCountry\":\"US\",\"localeVariant\":\"\",\"timeZoneId\":\"GMT+01:00\",\"timeZoneOffsetMillis\":3600000}"
  }, {
    "key" : "Encoding",
    "value" : "{\"defaultCharset\":\"UTF-8\"}"
  }, {
    "key" : "FileRefRoots",
    "value" : "{\"rootPaths\":{\"WORKSPACE\":\"C:\\\\tcagent1\\\\work\\\\668602365d1521fc\",\"GRADLE_USER_HOME\":\"C:\\\\Users\\\\tcagent1\\\\.gradle\"}}"
  }, {
    "key" : "ScopeIds",
    "value" : "{\"buildInvocationId\":\"anz66mihlbhc5pg4xjrp2lzin4\",\"workspaceId\":\"5xkayv4w7jdlnd5uqil5wzoyqu\",\"userId\":\"vtmsuxgn2bfbrnxfiyxdnyujca\"}"
  }, {
    "key" : "BasicMemoryStats",
    "value" : "{\"free\":449013816,\"total\":1029701632,\"max\":2621440000,\"peakSnapshots\":[{\"name\":\"CodeHeap 'non-nmethods'\",\"heap\":false,\"init\":2555904,\"used\":2433280,\"committed\":2555904,\"max\":5832704},{\"name\":\"Metaspace\",\"heap\":false,\"init\":0,\"used\":111861456,\"committed\":126562304,\"max\":536870912},{\"name\":\"CodeHeap 'profiled nmethods'\",\"heap\":false,\"init\":2555904,\"used\":48333824,\"committed\":58916864,\"max\":122880000},{\"name\":\"Compressed Class Space\",\"heap\":false,\"init\":0,\"used\":14106624,\"committed\":18296832,\"max\":528482304},{\"name\":\"G1 Eden Space\",\"heap\":true,\"init\":25165824,\"used\":367001600,\"committed\":390070272,\"max\":-1},{\"name\":\"G1 Old Gen\",\"heap\":true,\"init\":444596224,\"used\":598592512,\"committed\":975175680,\"max\":2621440000},{\"name\":\"G1 Survivor Space\",\"heap\":true,\"init\":0,\"used\":45088768,\"committed\":45088768,\"max\":-1},{\"name\":\"CodeHeap 'non-profiled nmethods'\",\"heap\":false,\"init\":2555904,\"used\":39802752,\"committed\":39845888,\"max\":122945536}],\"gcTime\":428}"
  } ],
  "buildTimestamp" : "2019-01-01 23:02:51.267+00",
  "wallClockDuration" : 15407,
  "failureIds" : [ ],
  "failed" : false,
  "failureData" : null,
  "userLink" : [ {
    "label" : "TeamCity Build",
    "url" : "https://builds.gradle.org/viewLog.html?buildId=18783100"
  }, {
    "label" : "Source",
    "url" : "https://github.com/gradle/gradle/commit/62f067d01f0f3d535325c7ae6e9c50d33f1b45ab"
  }, {
    "label" : "Git Commit Scans",
    "url" : "https://e.grdev.net/scans?search.names=Git+Commit+ID&search.values=62f067d01f0f3d535325c7ae6e9c50d33f1b45ab"
  }, {
    "label" : "CI CompileAll Scan",
    "url" : "https://e.grdev.net/scans?search.names=Git+Commit+ID&search.values=62f067d01f0f3d535325c7ae6e9c50d33f1b45ab&search.tags=CompileAll"
  }, {
    "label" : "Build Type Scans",
    "url" : "https://e.grdev.net/scans?search.names=CI+Build+Type&search.values=Gradle_Check_Platform_Java11_Openjdk_Windows_baseServicesGroovy"
  } ],
  "userNamedValue" : [ {
    "key" : "coverageOs",
    "value" : "windows"
  }, {
    "key" : "coverageJvmVersion",
    "value" : "java11"
  }, {
    "key" : "coverageJvmVendor",
    "value" : "openjdk"
  }, {
    "key" : "Build ID",
    "value" : "18783100"
  }, {
    "key" : "Git Commit ID",
    "value" : "62f067d01f0f3d535325c7ae6e9c50d33f1b45ab"
  }, {
    "key" : "CI Build Type",
    "value" : "Gradle_Check_Platform_Java11_Openjdk_Windows_baseServicesGroovy"
  }, {
    "key" : "Git Branch Name",
    "value" : "release"
  }, {
    "key" : "Git Status",
    "value" : "?? subprojects/kotlin-dsl/"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-0-VisitableURLClassLoader-hash",
    "value" : "cbe5a4147caef36820bd799cebaa7c3c"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-0-classpath",
    "value" : "file:/C:/Users/tcagent1/.gradle/caches/jars-3/3f7d2dc8ed225f0c59642859f6c15a82/buildSrc.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/0c8abb2ea472050c13da1abf19b6f8e6/buildquality.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/f3109ae47e6156b24818e7e2bf348c70/uberPlugins.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/49c5cfa91f9f25bab1df99cf540b7889/plugins.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/38fd80bc06c911a1a404502e2b664d86/binaryCompatibility.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/1535b01b56ea0d071ead2914a5c4036a/performance.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/1921b730afa0b0f13f05d2fae3f215a6/integrationTesting.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/0775b10e19f1205226062909311d2591/packaging.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/5bb0de6de59cdc306a38bdd703887182/versioning.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/5f7e60ab30780eb898a9d83844f6f94c/build.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/3f7d2dc8ed225f0c59642859f6c15a82/buildPlatform.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/0ecc31ba7932f9373a44ea71ea580539/cleanup.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/511ea0b5dc7ed45192e3fd3da1d1ebcc/ide.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/199852fbe87962c904d3426518983002/docs.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/2d507918964df675726211e1714677fe/profiling.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/ed33c9286a4a1cda45f6f9d04b3a0abd/configuration.jar:file:/C:/Users/tcagent1/.gradle/caches/jars-3/9cdb25dc6a01163b159842d317f98ce3/kotlinDsl.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/gradle.plugin.org.jetbrains.gradle.plugin.idea-ext/gradle-idea-ext/0.4.2/36f66c2bc9cccdb72dd136b8b2b6867d819b8219/gradle-idea-ext-0.4.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.owasp/dependency-check-gradle/3.1.0/53d9483c0e2c757fe173888568e3066e95893e4e/dependency-check-gradle-3.1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.owasp/dependency-check-core/3.1.0/6769ad2c92a2067a7cb9cd7ba0214224d8ebda1a/dependency-check-core-3.1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.8.2/3edcfe49d2c6053a70a2a47e4e1c2f94998a49cf/gson-2.8.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/me.champeau.gradle/japicmp-gradle-plugin/0.2.4/e797b187939b0016570a2ead7f2d8c5c011c35e3/japicmp-gradle-plugin-0.2.4.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.github.javaparser/javaparser-symbol-solver-core/3.6.11/76ba477f3b220ea21b29a9811e8d8fdc83a8afe4/javaparser-symbol-solver-core-3.6.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.github.siom79.japicmp/japicmp/0.10.0/cc38ecaaa6de94b2906924c6e672d39ebc80a8c5/japicmp-0.10.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.github.javaparser/javaparser-symbol-solver-logic/3.6.11/ef3b8d5c114f9c4b5444a74943b6921cda5e62ab/javaparser-symbol-solver-logic-3.6.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.github.javaparser/javaparser-symbol-solver-model/3.6.11/4aa7d02ff4391e7b2cac743b09c6bb8d5d943ed1/javaparser-symbol-solver-model-3.6.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.javassist/javassist/3.23.0-GA/5c71cd6815cc207379639aca8c88478b7e959e35/javassist-3.23.0-GA.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.github.javaparser/javaparser-core/3.6.11/bc5f53ab447bdb12b511e35e77ac4b0d17d56cf/javaparser-core-3.6.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/26.0-jre/6a806eff209f36f635f943e16d97491f00f6bfab/guava-26.0-jre.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.codehaus.groovy.modules.http-builder/http-builder/0.7.2/323092cd786480311c1cf693770f9e6fc20a8bef/http-builder-0.7.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/net.sf.json-lib/json-lib/2.3/f35340c0a0380141f62c72b76c8fb4bfa638d8c1/json-lib-2.3-jdk15.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.velocity/velocity/1.7/2ceb567b8f3f21118ecdec129fe1271dbc09aa7a/velocity-1.7.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/net.sf.ezmorph/ezmorph/1.0.6/1e55d2a0253ea37745d33062852fd2c90027432/ezmorph-1.0.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-lang/commons-lang/2.6/ce1edb914c94ebc388f086c6827e8bdeec71ac2/commons-lang-2.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.asciidoctor/asciidoctorj-pdf/1.5.0-alpha.11/cbfc984df46fe321ce884aa62f7a6fb35100ec02/asciidoctorj-pdf-1.5.0-alpha.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.asciidoctor/asciidoctorj/1.5.5/15d22a1d8e3880c0632f81da474d48b873d5579/asciidoctorj-1.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.asciidoctor/asciidoctor-gradle-plugin/1.5.3/ed827afd210a6bce6d597a89400e61c56c8de57f/asciidoctor-gradle-plugin-1.5.3.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.codenarc/CodeNarc/1.0/bab1fac2ef463452e41804e66a2097adcd701c39/CodeNarc-1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-all/0.34.56/74ec500ab8e145a33ef6ab2ffe80a39228f0cbce/flexmark-all-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.uwyn/jhighlight/1.0/b1774029ee29472df8c25e5ba796431f7689fd6/jhighlight-1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-html-parser/0.34.56/6707b1aad5c88b536fc320feae91a4a9b967c5ec/flexmark-html-parser-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-pdf-converter/0.34.56/a564d7b561925e8f2d208b678dc969f178db5ba1/flexmark-pdf-converter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.openhtmltopdf/openhtmltopdf-jsoup-dom-converter/0.0.1-RC13/2887c313e229da6667bd18c3698959191b6da787/openhtmltopdf-jsoup-dom-converter-0.0.1-RC13.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.jsoup/jsoup/1.11.3/36da09a8f68484523fa2aaa100399d612b247d67/jsoup-1.11.3.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.3.20/4cbc5922a54376018307a731162ccaf3ef851a39/kotlin-stdlib-1.3.20.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-commons/6.0/f256fd215d8dd5a4fa2ab3201bf653de266ed4ec/asm-commons-6.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-tree/6.0/a624f1a6e4e428dcd680a01bab2d4c56b35b18f0/asm-tree-6.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/6.0/bc6fa6b19424bb9592fe43bbc20178f92d403105/asm-6.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.thoughtworks.qdox/qdox/2.0-M9/ea940f1cdba9205d03e4f54b9ac0ec988de751dd/qdox-2.0-M9.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.openmbee.junit/junit-xml-parser/1.0.0/9909bbc96521538a1ac30dbb14c9fe8aaa2a66ba/junit-xml-parser-1.0.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.owasp/dependency-check-utils/3.1.0/16a5fe765605c0f761f5199c75cb42f26c397437/dependency-check-utils-3.1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-io/commons-io/2.6/815893df5f31da2ece4040fe0a12fd44b577afaf/commons-io-2.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/javax.activation/activation/1.1.1/485de3a253e23f645037828c07f1d7f1af40763a/activation-1.1.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/javax.xml.bind/jaxb-api/2.2.12/4c83805595b15acf41d71d49e3add7c0e85baaed/jaxb-api-2.2.12.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.guava/guava-jdk5/14.0.1/ec21c29e3f8afccff893486de213a86998daf134/guava-jdk5-14.0.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/me.champeau.gradle/jmh-gradle-plugin/0.4.7/130d4195b7c7c2d1524e1449fd901336ba1703b4/jmh-gradle-plugin-0.4.7.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.gradle/build-scan-plugin/2.1/bade2a9009f96169d2b25d3f2023afb2cdf8119f/build-scan-plugin-2.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/2.5.2/cea74543d5904a30861a61b4643a5f2bb372efc4/checker-qual-2.5.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.errorprone/error_prone_annotations/2.1.3/39b109f2cd352b2d71b52a3b5a1a9850e1dc304b/error_prone_annotations-2.1.3.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.google.j2objc/j2objc-annotations/1.1/976d8d30bebc251db406f2bdb3eb01962b5685b3/j2objc-annotations-1.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.codehaus.mojo/animal-sniffer-annotations/1.14/775b7e22fb10026eed3f86e8dc556dfafe35f2d5/animal-sniffer-annotations-1.14.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/1.7.26/c09885af02af34266ed929f94cedcf87cc965f46/jruby-complete-1.7.26.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.beust/jcommander/1.35/47592e181b0bdbbeb63029e08c5e74f6803c4edd/jcommander-1.35.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-collections/commons-collections/3.2.2/8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5/commons-collections-3.2.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.gmetrics/GMetrics/1.0/706273cc3529d2851400620b0ba07aaa681dffe4/GMetrics-1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.25/da76ca59f6a57ee3102f8f9bd9cee742973efa8a/slf4j-api-1.7.25.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-profile-pegdown/0.34.56/d772cddecb4a4ff98d476908707dbc1889dcaf8a/flexmark-profile-pegdown-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-abbreviation/0.34.56/4bddd1b5c149e6ab3d64f0bdfd7025bb4796373/flexmark-ext-abbreviation-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-admonition/0.34.56/f39b0eaf6ed39049a38702ddf57f76c678b3f9e3/flexmark-ext-admonition-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-anchorlink/0.34.56/38fbbf7b8c727052a238d1a881810008cffb5af3/flexmark-ext-anchorlink-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-aside/0.34.56/e6cef3ca818886ad93615020b18e1bd0d3ed3138/flexmark-ext-aside-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-enumerated-reference/0.34.56/c850a5358b9824ac971724cf34964fc2df90a2a3/flexmark-ext-enumerated-reference-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-attributes/0.34.56/13b69dd00e555903603b33807ebafad07f8ff25b/flexmark-ext-attributes-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-autolink/0.34.56/86a975c571c403ce1de5b1e4eeeabd56e4cc1813/flexmark-ext-autolink-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-definition/0.34.56/5e98709f23d9fc24219379c0ca7f02aaf393185f/flexmark-ext-definition-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-emoji/0.34.56/953be04ed785bf9810a6501907b0b8fa2a0a17c8/flexmark-ext-emoji-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-escaped-character/0.34.56/5f904e0242c8c17f83b84ee7fb870e3aae319b27/flexmark-ext-escaped-character-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-footnotes/0.34.56/71c0541d3edcc1df08226fa1c06403003acc6811/flexmark-ext-footnotes-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gfm-issues/0.34.56/78e82302f6147dfff7f1cec934fe69d2faedf535/flexmark-ext-gfm-issues-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-jira-converter/0.34.56/88e4534ac03c1b7847a4d9c16802db82a421e385/flexmark-jira-converter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-youtrack-converter/0.34.56/e876a5a5beb2663bb418ae748b46dfc0cf99a5b8/flexmark-youtrack-converter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gfm-strikethrough/0.34.56/df4c3ca2224fadb919f35cc63b2625f483376/flexmark-ext-gfm-strikethrough-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gfm-tables/0.34.56/b20d66296a9134f45f9255cb0080020470576ce3/flexmark-ext-gfm-tables-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gfm-tasklist/0.34.56/5786018ea31a6693506f9987450e464549f986a5/flexmark-ext-gfm-tasklist-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gfm-users/0.34.56/6b035de87a8e8c32bdaa67e0cb2ba72c5b3f5e88/flexmark-ext-gfm-users-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-macros/0.34.56/3a93b513428acc83c0f3f7c811c119fdd445bd/flexmark-ext-macros-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-gitlab/0.34.56/49734a527eb7a7c45f708202684348587201c8b9/flexmark-ext-gitlab-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-jekyll-front-matter/0.34.56/ec2966b53755838d33d2e4fc97fe699e4d4d8a98/flexmark-ext-jekyll-front-matter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-jekyll-tag/0.34.56/c7e9945a1c7246e749cfcc6f526ac81649a71db2/flexmark-ext-jekyll-tag-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-media-tags/0.34.56/3a9860f4d9c0f9e9cb282e4e9a7dcf16b65d5db9/flexmark-ext-media-tags-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-ins/0.34.56/8717c23aa4780c2c2376b635f438b39bc1367eb/flexmark-ext-ins-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-xwiki-macros/0.34.56/5cacd4375918a6cee32b78eb87f1406e862dd580/flexmark-ext-xwiki-macros-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-superscript/0.34.56/6e627ec293f24b53ac65f2c6a32329ca8e12432b/flexmark-ext-superscript-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-tables/0.34.56/2fb3b7d6da64432c2bf8934755295c3766534a1b/flexmark-ext-tables-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-toc/0.34.56/79b40ac1d0eb6f99e8c97a39ef7cf413b87f5fed/flexmark-ext-toc-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-typographic/0.34.56/fb19c21a33f317fed6da4623e7fa00f5bdc6d65c/flexmark-ext-typographic-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-wikilink/0.34.56/fe9f9cc64f00321a5dca96b9be0c4371fb945fba/flexmark-ext-wikilink-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-yaml-front-matter/0.34.56/9006c337a805db6fb81aab116228ef8353640ee2/flexmark-ext-yaml-front-matter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-ext-youtube-embedded/0.34.56/1a9a66be72d9c83034e805a9fcef8d4f918d3f05/flexmark-ext-youtube-embedded-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-formatter/0.34.56/f9447bbc670f020349ed95488ca0b9a59fc96286/flexmark-formatter-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark/0.34.56/d3ab5e9b35af1839c69b3e3aaacc5f494986c100/flexmark-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-test-util/0.34.56/9f0897271234a5c61153845aa4ad9bc1f03e93a3/flexmark-test-util-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark/flexmark-util/0.34.56/c7d860d253cf868f855b9d9d26c4f2ccf9e85a9d/flexmark-util-0.34.56.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-common/1.3.20/d8b8e746e279f1c4f5e08bc14a96b82e6bb1de02/kotlin-stdlib-common-1.3.20.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpclient/4.2.1/b69bd03af60bf487b3ae1209a644ecac587bf6fc/httpclient-4.2.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/net.sourceforge.nekohtml/nekohtml/1.9.16/61e35204e5a8fdb864152f84e2e3b33ab56f50ab/nekohtml-1.9.16.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/xml-resolver/xml-resolver/1.2/3d0f97750b3a03e0971831566067754ba4bfd68c/xml-resolver-1.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-lang3/3.5/6c6c702c89bfff3cd9e80b04d668c5e190d588c6/commons-lang3-3.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.sun.xml.bind/jaxb-core/2.2.11/c3f87d654f8d5943cd08592f3f758856544d279a/jaxb-core-2.2.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.sun.xml.bind/jaxb-impl/2.2.11/a49ce57aee680f9435f49ba6ef427d38c93247a6/jaxb-impl-2.2.11.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.openjdk.jmh/jmh-core/1.21/442447101f63074c61063858033fbfde8a076873/jmh-core-1.21.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.vdurmont/semver4j/2.1.0/f4123dbb6a2d7991eff772e9a4d8f4111dac8e55/semver4j-2.1.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/joda-time/joda-time/1.6/5a18504e34c5cbe9259d6fd0123ccf6f16115a41/joda-time-1.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-compress/1.15/b686cd04abaef1ea7bc5e143c080563668eec17e/commons-compress-1.15.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-analyzers-common/5.5.5/e6b3f5d1b33ed24da7eef0a72f8062bd4652700c/lucene-analyzers-common-5.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-queryparser/5.5.5/6c965eb5838a2ba58b0de0fd860a420dcda11937/lucene-queryparser-5.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-core/5.5.5/c34bcd9274859dc07cfed2a935aaca90c4f4b861/lucene-core-5.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.h2database/h2/1.4.196/dd0034398d593aa3588c6773faac429bbd9aea0e/h2-1.4.196.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.glassfish/javax.json/1.0.4/3178f73569fd7a1e5ffc464e680f7a8cc784b85a/javax.json-1.0.4.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.sun.mail/mailapi/1.6.0/8ef610e245ad01a88e4b68f3a991c1183f21106d/mailapi-1.6.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.nibor.autolink/autolink/0.6.0/3986d016a14e8c81afeec752f19af29b20e8367b/autolink-0.6.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.openhtmltopdf/openhtmltopdf-pdfbox/0.0.1-RC13/11d082f6959e07f470f0449462fd1629e5c962d6/openhtmltopdf-pdfbox-0.0.1-RC13.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.openhtmltopdf/openhtmltopdf-rtl-support/0.0.1-RC13/bc23d414d5b5c888d35b76d2f53e3ba2d69952c3/openhtmltopdf-rtl-support-0.0.1-RC13.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.openhtmltopdf/openhtmltopdf-core/0.0.1-RC13/2d3108c06026884114f56c85d2a3db72a6da7d12/openhtmltopdf-core-0.0.1-RC13.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.httpcomponents/httpcore/4.2.1/2d503272bf0a8b5f92d64db78b4ba9abbaccc6fd/httpcore-4.2.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-beanutils/commons-beanutils/1.8.0/c651d5103c649c12b20d53731643e5fffceb536/commons-beanutils-1.8.0.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/de.rototor.pdfbox/graphics2d/0.12/7df4072cd4d94233bdb9921b07ebaeb8a8508603/graphics2d-0.12.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.pdfbox/pdfbox/2.0.8/17bdf273d66f3afe41eedb9d3ab6a7b819c44a0c/pdfbox-2.0.8.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.pdfbox/xmpbox/2.0.8/bcce394a05b96c6155d27e144eb6e359ee7ff6b1/xmpbox-2.0.8.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.pdfbox/fontbox/2.0.8/52f852fcfc7481d45efdffd224eb78b85981b17b/fontbox-2.0.8.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-logging/commons-logging/1.2/4bfc12adfe4842bf07b657f0369c4cb522955686/commons-logging-1.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/commons-codec/commons-codec/1.6/b7f0fc8f61ecadeb3695f0b9464755eee44374d4/commons-codec-1.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/net.sf.jopt-simple/jopt-simple/4.6/306816fb57cf94f108a43c95731b08934dcae15c/jopt-simple-4.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-math3/3.2/ec2544ab27e110d2d431bdad7d538ed509b21e62/commons-math3-3.2.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.objenesis/objenesis/2.6/639033469776fd37c08358c6b92a4761feb2af4b/objenesis-2.6.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-queries/5.5.5/d99719e7c58c149113f897bca301f1d68cbf3241/lucene-queries-5.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-sandbox/5.5.5/d145d959109257c47151be43b211213dff455f47/lucene-sandbox-5.5.5.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/junit/junit/4.12/2973d150c0dc1fefe998f834810d68f278ea58ec/junit-4.12.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/com.ibm.icu/icu4j/59.1/6f06e820cf4c8968bbbaae66ae0b33f6a256b57f/icu4j-59.1.jar:file:/C:/Users/tcagent1/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-1-CachingClassLoader-hash",
    "value" : "1390d3b8b823cce66b4ab57e53897488"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-2-FilteringClassLoader-hash",
    "value" : "1390d3b8b823cce66b4ab57e53897488"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-3-MixInLegacyTypesClassLoader-hash",
    "value" : "9304af8e2d9fa0b1c08cec7a2b00600d"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-4-VisitableURLClassLoader-hash",
    "value" : "d9a171db0904f311b645809e5ae762d0"
  }, {
    "key" : ":baseServices:classpathManifest-classloader-5-PlatformClassLoader-hash",
    "value" : "8f76bfec5408d18e8414e586dba94759"
  } ],
  "userTag" : [ "Check", "FunctionalTest", "ReadyforMerge", "CI", "CACHED", "release", "dirty" ]
}
    """.trimIndent()

    private val expectedOutputMaven = """{
  "buildId" : "qjxkgdbcuaac2",
  "rootProjectName" : "timeline-failed-goals",
  "buildTool" : "Maven",
  "buildToolVersion" : "3.6.1",
  "buildAgentId" : "tcagent1@dev31.gradle.org",
  "buildRequestedTasks" : [ "clean", "compile" ],
  "buildExcludedTasks" : [ ],
  "environmentParameters" : [ {
    "key" : "MvnHardware",
    "value" : "{\"numProcessors\":8}"
  }, {
    "key" : "MvnOs",
    "value" : "{\"family\":\"linux\",\"name\":\"Linux\",\"version\":\"4.15.0-50-generic\",\"arch\":\"amd64\"}"
  }, {
    "key" : "MvnJvm",
    "value" : "{\"version\":\"1.8.0_181\",\"vendor\":\"Oracle Corporation\",\"runtimeName\":\"Java(TM) SE Runtime Environment\",\"runtimeVersion\":\"1.8.0_181-b13\",\"classVersion\":\"52.0\",\"vmInfo\":\"mixed mode\",\"vmName\":\"Java HotSpot(TM) 64-Bit Server VM\",\"vmVersion\":\"25.181-b13\",\"vmVendor\":\"Oracle Corporation\"}"
  }, {
    "key" : "MvnLocality",
    "value" : "{\"localeLanguage\":\"en\",\"localeCountry\":\"US\",\"localeVariant\":\"\",\"timeZoneId\":\"Europe/Berlin\",\"timeZoneOffsetMillis\":7200000}"
  }, {
    "key" : "MvnEncoding",
    "value" : "{\"defaultCharset\":\"UTF-8\"}"
  }, {
    "key" : "MvnFileRefRoots",
    "value" : "{\"rootPaths\":{\"WORKSPACE\":\"/home/tcagent1/agent/work/8d3306ac5f39e07b/maven-scans-example-builds/build/gen-work/timeline-failed-goals-1.0.8\"}}"
  }, {
    "key" : "MvnScopeIds",
    "value" : "{\"buildInvocationId\":\"67sabxrb5rcfzluqun7gfusrh4\"}"
  }, {
    "key" : "MvnBasicMemoryStats",
    "value" : "{\"max\":7443841024,\"peakSnapshots\":[{\"name\":\"Code Cache\",\"heap\":false,\"init\":2555904,\"used\":7021440,\"committed\":7077888,\"max\":251658240},{\"name\":\"Metaspace\",\"heap\":false,\"init\":0,\"used\":30623104,\"committed\":32112640,\"max\":-1},{\"name\":\"Compressed Class Space\",\"heap\":false,\"init\":0,\"used\":3943992,\"committed\":4325376,\"max\":1073741824},{\"name\":\"PS Eden Space\",\"heap\":true,\"init\":131596288,\"used\":131596288,\"committed\":168820736,\"max\":2750939136},{\"name\":\"PS Survivor Space\",\"heap\":true,\"init\":21495808,\"used\":21365600,\"committed\":21495808,\"max\":21495808},{\"name\":\"PS Old Gen\",\"heap\":true,\"init\":349700096,\"used\":20408408,\"committed\":349700096,\"max\":5582618624}],\"gcTime\":68}"
  } ],
  "buildTimestamp" : "2019-06-03 07:26:10.170+00",
  "wallClockDuration" : 1740,
  "failureIds" : [ "6929713202549594971", "-4272005080405076924" ],
  "failed" : true,
  "failureData" : {
    "category" : "VERIFICATION",
    "causes" : [ {
      "exceptionId" : "6093028342085047029",
      "exceptionClassName" : "org.apache.maven.plugin.compiler.CompilationFailureException",
      "message" : "Compilation failure\n/home/tcagent1/agent/work/8d3306ac5f39e07b/maven-scans-example-builds/build/gen-work/timeline-failed-goals-1.0.8/b/src/main/java/Error.java:[1,1] class, interface, or enum expected\n",
      "failedTaskGoalName" : null,
      "failedTaskTypeOrMojoClassName" : null
    }, {
      "exceptionId" : "6929713202549594971",
      "exceptionClassName" : "org.apache.maven.lifecycle.LifecycleExecutionException",
      "message" : "Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.1:compile (default-compile) on project a: Compilation failure\n/home/tcagent1/agent/work/8d3306ac5f39e07b/maven-scans-example-builds/build/gen-work/timeline-failed-goals-1.0.8/a/src/main/java/Error.java:[1,1] class, interface, or enum expected\n",
      "failedTaskGoalName" : "compiler:compile",
      "failedTaskTypeOrMojoClassName" : "org.apache.maven.plugin.compiler.CompilerMojo"
    }, {
      "exceptionId" : "-4272005080405076924",
      "exceptionClassName" : "org.apache.maven.lifecycle.LifecycleExecutionException",
      "message" : "Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.1:compile (default-compile) on project b: Compilation failure\n/home/tcagent1/agent/work/8d3306ac5f39e07b/maven-scans-example-builds/build/gen-work/timeline-failed-goals-1.0.8/b/src/main/java/Error.java:[1,1] class, interface, or enum expected\n",
      "failedTaskGoalName" : "compiler:compile",
      "failedTaskTypeOrMojoClassName" : "org.apache.maven.plugin.compiler.CompilerMojo"
    }, {
      "exceptionId" : "-7666900219935905648",
      "exceptionClassName" : "org.apache.maven.plugin.compiler.CompilationFailureException",
      "message" : "Compilation failure\n/home/tcagent1/agent/work/8d3306ac5f39e07b/maven-scans-example-builds/build/gen-work/timeline-failed-goals-1.0.8/a/src/main/java/Error.java:[1,1] class, interface, or enum expected\n",
      "failedTaskGoalName" : null,
      "failedTaskTypeOrMojoClassName" : null
    } ]
  },
  "userLink" : [ ],
  "userNamedValue" : [ {
    "key" : "example-build-description",
    "value" : "A project with failing goals"
  }, {
    "key" : "example-build-identifier",
    "value" : "timeline-failed-goals"
  }, {
    "key" : "example-build-type",
    "value" : "Scan"
  } ],
  "userTag" : [ ]
}
    """.trimIndent()
}
