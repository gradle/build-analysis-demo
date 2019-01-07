package org.gradle.buildeng.data.collection

import java.net.InetSocketAddress

data class ServerConnectionInfo(val socketAddress: InetSocketAddress, val username: String, val password: String)
