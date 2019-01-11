plugins {
    `build-scan`
}

group = "org.gradle.buildeng.analysis"
version = "0.1.0"

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
}

allprojects {
  repositories {
      maven { url = uri("https://maven-central.storage.googleapis.com") }
      jcenter()
  }
}
