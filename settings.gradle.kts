plugins {
    id("com.gradle.enterprise").version("3.4.1")
}

rootProject.name = "build-analysis"

include(":analysis-common")
include(":build-event-collectorator")
include(":build-event-indexerator")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}
