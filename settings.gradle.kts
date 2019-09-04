rootProject.name = "build-analysis"

include(":analysis-common")
include(":build-event-collectorator")
include(":build-event-indexerator")

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if(requested.id.id == "org.jetbrains.kotlin.jvm") {
                useVersion("1.3.50")
            }
        }
    }
}
