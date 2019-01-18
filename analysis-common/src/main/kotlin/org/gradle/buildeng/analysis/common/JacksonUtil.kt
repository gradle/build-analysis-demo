package org.gradle.buildeng.analysis.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

class InstantSerializer : JsonSerializer<Instant>() {
    companion object {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX")
    }

    @Throws(IOException::class)
    override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(dateFormat.format(Date(value.toEpochMilli())))
    }

    override fun handledType(): Class<Instant> {
        return Instant::class.java
    }
}

class DurationSerializer : JsonSerializer<Duration>() {
    @Throws(IOException::class)
    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.toMillis())
    }

    override fun handledType(): Class<Duration> {
        return Duration::class.java
    }
}
