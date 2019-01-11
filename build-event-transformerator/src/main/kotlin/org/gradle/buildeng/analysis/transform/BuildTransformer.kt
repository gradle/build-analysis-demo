package org.gradle.buildeng.analysis.transform

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions

class BuildTransformer {
    private val objectMapper = ObjectMapper()

    // Read raw files from input GCS bucket, transform, and write to destination bucket
    fun transform(jsonTransformer: JsonTransformer, inputBucket: String, outputBucket: String) {
        val storage = StorageOptions.getDefaultInstance().service
        println("Transforming JSON from bucket $inputBucket to bucket $outputBucket")

        storage.get(inputBucket).list().iterateAll().forEach { blob ->
            println("Transforming blob ${blob.name}")
            val jsonNode = jsonTransformer.transform(String(blob.getContent()))
            val blobId = BlobId.of(outputBucket, blob.name)
            val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
            storage.create(blobInfo, objectMapper.writeValueAsBytes(jsonNode))
        }

        println("All input files processed")
    }
}
