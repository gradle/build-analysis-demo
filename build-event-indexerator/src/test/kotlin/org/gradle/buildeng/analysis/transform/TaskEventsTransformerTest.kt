package org.gradle.buildeng.analysis.transform

import org.apache.beam.sdk.testing.TestPipeline
import org.junit.Rule
import org.junit.Test

class TaskEventsTransformerTest {
    @Rule
    @Transient
    @JvmField
    val pipeline: TestPipeline = TestPipeline.create()

    @Test
    fun shouldDoTheThing() {
//        val results = pipeline
//                .apply(Create.of(
//                        "apache beam in kotlin",
//                        "this is kotlin",
//                        "awesome kotlin",
//                        ""))
//                .flatMap { it.split(Regex(TaskEventsTransformer.TOKENIZER_PATTERN)).filter { it.isNotEmpty() }.toList() }
//
//        PAssert.that(results).containsInAnyOrder(
//                "this: 1", "apache: 1", "beam: 1", "is: 1", "kotlin: 3", "awesome: 1", "in: 1")
//
//        pipeline.run()
    }
}
