package org.gradle.buildeng.analysis.indexing

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.Validation

interface IndexingDataflowPipelineOptions : DataflowPipelineOptions {
    @get:Description("The GCS location of the text you'd like to process")
    @get:Validation.Required
    var input: String

    @get:Description("Output table to write to")
    @get:Validation.Required
    var output: String
}
