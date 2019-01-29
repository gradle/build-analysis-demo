package org.gradle.buildeng.analysis.model

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.gson.Gson
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

object BigQueryTableSchemaGenerator {
    /**
     * Given a Kotlin data class, return a TableSchema that represents the given class.
     */
    fun generateFieldList(kClass: KClass<*>): ArrayList<TableFieldSchema> {
        val allFields = ArrayList<TableFieldSchema>()

        for (prop in kClass.memberProperties) {
            // Extract the type parameter from collections because List<String> is represented as a single field in BigQuery
            val unwrappedType = getUnwrappedType(prop.returnType)
            val type = when(unwrappedType) {
                Boolean::class -> "BOOLEAN"
                String::class -> "STRING"
                ByteArray::class -> "BYTES"
                Instant::class -> "TIMESTAMP"
                Int::class, Long::class, Duration::class -> "INTEGER" // This is actually INT64 so Long fits
                Float::class, Double::class -> "FLOAT"
                // NOTE: not yet handled: Array, Geography
                else -> "RECORD"
            }

            val mode = when {
                prop.returnType.toString().startsWith("kotlin.collections.") -> "REPEATED"
                prop.returnType.isMarkedNullable -> "NULLABLE"
                else -> "REQUIRED"
            }

            var field = TableFieldSchema().setName(prop.name).setMode(mode).setType(type)
            if (type == "RECORD") {
                field = field.setFields(generateFieldList(unwrappedType))
            }
            allFields.add(field)
        }

        return allFields
    }

    fun generateJsonSchema(columns: ArrayList<TableFieldSchema>): String = Gson().toJson(columns)

    /**
     * Unwrap the type parameter from a List<String>.
     */
    private fun getUnwrappedType(input: KType): KClass<*> {
        val inputStr = input.toString()
        return if (inputStr.startsWith("kotlin.collections.")) {
            input.arguments[0].type?.classifier as KClass<*>
        } else {
            input.classifier as KClass<*>
        }
    }
}
