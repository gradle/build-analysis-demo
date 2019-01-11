package org.gradle.buildeng.analysis.common

import java.net.InetSocketAddress

data class ServerConnectionInfo(val socketAddress: InetSocketAddress, val username: String, val password: String) {
    companion object {
        fun fromEnv() = ServerConnectionInfo(
                InetSocketAddress(System.getenv("GRADLE_ENTERPRISE_HOSTNAME"), 443),
                System.getenv("GRADLE_ENTERPRISE_USERNAME"),
                System.getenv("GRADLE_ENTERPRISE_PASSWORD")
        )
    }
}
