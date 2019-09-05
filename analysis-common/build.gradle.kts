plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation(kotlin("stdlib-jdk8"))
}
