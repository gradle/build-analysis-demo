package org.gradle.buildeng.analysis.indexing

import com.google.cloud.bigquery.*


object RawBuildJsonIndexer {

    private val buildId: Field = Field.newBuilder("buildId", LegacySQLTypeName.STRING).setMode(Field.Mode.REQUIRED).setDescription("Unique build ID").build()
    private val pluginVersionField: Field = Field.newBuilder("pluginVersion", LegacySQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build()
    private val gradleVersionField: Field = Field.newBuilder("gradleVersion", LegacySQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build()
    private val buildTimestamp = Field.newBuilder("timestamp", LegacySQLTypeName.TIMESTAMP).setMode(Field.Mode.REQUIRED).build()

    private val eventRecords = FieldList.of(
            Field.newBuilder("timestamp", LegacySQLTypeName.TIMESTAMP).setMode(Field.Mode.REQUIRED).build(),
            Field.newBuilder("type", LegacySQLTypeName.RECORD,
                    FieldList.of(
                            Field.newBuilder("eventType", LegacySQLTypeName.STRING).setMode(Field.Mode.REQUIRED).build(),
                            Field.newBuilder("majorVersion", LegacySQLTypeName.INTEGER).setMode(Field.Mode.REQUIRED).build(),
                            Field.newBuilder("minorVersion", LegacySQLTypeName.INTEGER).setMode(Field.Mode.REQUIRED).build()
                    )
            ).setMode(Field.Mode.REQUIRED).build(),
            Field.newBuilder("data", LegacySQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build()
    )

    private val events: Field = Field.newBuilder("event", LegacySQLTypeName.RECORD, eventRecords).setMode(Field.Mode.REPEATED).setDescription("Stream of events related to given build").build()

    private val genericBigQuerySchema: List<Field> = listOf(buildId, pluginVersionField, gradleVersionField, buildTimestamp, events)

    fun index(datasetName: String, tableName: String, sourceLocation: String) {
        val tableId = TableId.of(datasetName, tableName)
        val bigQuery = BigQueryOptions.getDefaultInstance().service
        val schema = Schema.of(genericBigQuerySchema)

        // NOTE: table is automatically created if needed by load job
        val loadJobConfiguration = LoadJobConfiguration.builder(tableId, sourceLocation)
                .setFormatOptions(FormatOptions.json())
                .setCreateDisposition(JobInfo.CreateDisposition.CREATE_IF_NEEDED)
                .setSchema(schema)
                .build()

        val loadJob = bigQuery.create(JobInfo.of(loadJobConfiguration))
        println("Load job ${loadJob.jobId} started to table $datasetName:$tableName")
        loadJob.waitFor()
        println("Load job ${loadJob.jobId} completed")
    }
}
