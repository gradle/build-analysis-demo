package org.gradle.buildeng.data.collection

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.IOException
import java.time.Instant

object JacksonUtils {
    private val SHARED = objectMapper()

    fun sharedObjectMapper(): ObjectMapper {
        return SHARED
    }

    @JvmOverloads
    fun objectMapper(jsonFactory: JsonFactory? = null): ObjectMapper {
        val objectMapper = ObjectMapper(jsonFactory)
                .setDefaultPrettyPrinter(MinimalPrettyPrinter(""))
                .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        return stdConfigure(objectMapper)
    }

    private fun stdConfigure(mapper: ObjectMapper): ObjectMapper {
        return mapper
                .registerModule(Jdk8Module())
                .registerModule(JavaTimeModule())
                .registerModule(GuavaModule())
                .registerModule(object : SimpleModule() {
                    init {
                        addSerializer(InstantSerializer())
                    }
                })
    }

    private class InstantSerializer : JsonSerializer<Instant>() {

        @Throws(IOException::class)
        override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeNumber(value.toEpochMilli())
        }

        override fun handledType(): Class<Instant> {
            return Instant::class.java
        }
    }
}
