package org.gradle.buildeng.analysis.consumer

import io.netty.buffer.Unpooled
import io.reactivex.netty.protocol.http.sse.ServerSentEvent
import org.gradle.buildeng.analysis.common.ServerConnectionInfo
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildConsumerTest {
    private val serverConnectionInfo = ServerConnectionInfo(InetSocketAddress("bogus.com", 443), "user", "pass")

    @Test
    fun testByteBufsToJoinedArray() {
        val events = mutableListOf<ServerSentEvent>()
        for (i in 1..3) {
            events.add(ServerSentEvent(Unpooled.directBuffer().writeBytes("string$i".toByteArray())))
        }

        val bytes = BuildConsumer(serverConnectionInfo)
                .byteBufsToJoinedArray(events)

        assertEquals("string1\nstring2\nstring3\n", String(bytes))
    }

    @Test
    fun testParse() {
        val jsonString = """{"buildId":"7ocipcgq42jds","pluginVersion":"2.1","gradleVersion":"5.1-milestone-1","timestamp":1544267050201}"""
        val sse = ServerSentEvent(Unpooled.directBuffer().writeBytes(jsonString.toByteArray()))
        val parsedJson = BuildConsumer(serverConnectionInfo).parse(sse)

        assertEquals("7ocipcgq42jds", parsedJson["buildId"].asText())
        assertEquals(1544267050201, parsedJson["timestamp"].asLong())
    }
}
