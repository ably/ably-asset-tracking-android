package com.ably.tracking.test.android.common

import io.ably.lib.types.ClientOptions
import io.ably.lib.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import javax.net.ssl.SSLSocketFactory
import kotlin.math.log

private const val AGENT_HEADER_NAME = "ably-asset-tracking-android-publisher-tests"

private const val PROXY_HOST = "localhost"
private const val PROXY_PORT = 13579
private const val REALTIME_HOST = "realtime.ably.io"
private const val REALTIME_PORT = 443


interface RealtimeProxy {
    fun start()
    fun stop()

    val clientOptions: ClientOptions
}

class Layer4Proxy(
    val listenHost: String = PROXY_HOST,
    val listenPort: Int = PROXY_PORT,
    private val targetAddress: String = REALTIME_HOST,
    private val targetPort: Int = REALTIME_PORT
    ): RealtimeProxy {

    private val loggingTag = "Layer4Proxy"

    private var server: ServerSocket? = null
    private val sslSocketFactory = SSLSocketFactory.getDefault()
    private val connections : MutableList<Layer4ProxyConnection> = mutableListOf()

    private fun accept() : Layer4ProxyConnection {
        val clientSock = server?.accept()
        testLogD( "$loggingTag: accepted connection")

        val serverSock = sslSocketFactory.createSocket(targetAddress, targetPort)
        val conn = Layer4ProxyConnection(serverSock, clientSock!!, targetAddress, parentProxy = this)
        connections.add(conn)
        return conn
    }

    override val clientOptions = ClientOptions().apply {
        this.clientId = "AatTestProxy_${UUID.randomUUID()}"
        this.agents = mapOf(AGENT_HEADER_NAME to BuildConfig.VERSION_NAME)
        this.idempotentRestPublishing = true
        this.autoConnect = false
        this.key = BuildConfig.ABLY_API_KEY
        this.logHandler = Log.LogHandler { _, _, msg, tr ->
            testLogD("${msg!!} - $tr")
        }
        this.realtimeHost = listenHost
        this.port = listenPort
        this.tls = false
    }

    override fun stop() {
        server?.close()
        server = null

        connections.forEach {
            it.stop()
        }
        connections.clear()
    }

    override fun start() {
        server = ServerSocket(listenPort)
        Thread {
            while (true) {
                testLogD("$loggingTag: proxy trying to accept")
                try {
                    val conn = this.accept()
                    testLogD("$loggingTag: proxy starting to run")
                    conn.run()
                } catch (e : Exception) {
                    testLogD("$loggingTag: proxy shutting down: " + e.message)
                    break
                }
            }
        }.start()
    }
}

internal class Layer4ProxyConnection(
    private val server: Socket,
    private val client: Socket,
    private val targetHost: String,
    private val parentProxy: Layer4Proxy
) {

    private val loggingTag = "Layer4ProxyConnection"

    fun run() {
        Thread { proxy(server, client, true) }.start()
        Thread { proxy(client, server) }.start()
    }

    fun stop() {
        try {
            server.close()
        } catch (e: Exception) {
            testLogD("$loggingTag: stop() server: $e")
        }

        try {
            client.close()
        } catch (e: Exception) {
            testLogD("$loggingTag: stop() client: $e")
        }
    }

    private fun proxy(dstSock: Socket , srcSock: Socket, rewriteHost: Boolean = false) {
        try {
            val dst = dstSock.getOutputStream()
            val src = srcSock.getInputStream()
            val buff = ByteArray(4096)
            var bytesRead: Int

            // deal with the initial HTTP upgrade packet
            bytesRead = src.read(buff)
            if (bytesRead <0 ) {
                return
            }

            // HTTP is plaintext so we can just read it
            val msg = String(buff, 0, bytesRead)
            testLogD("$loggingTag: ${String(buff.copyOfRange(0, bytesRead))}")
            if (rewriteHost) {
                val newMsg = msg.replace(
                    oldValue = "${parentProxy.listenHost}:${parentProxy.listenPort}",
                    newValue = targetHost
                )
                val newBuff = newMsg.toByteArray()
                dst.write(newBuff, 0, newBuff.size)
            } else {
                dst.write(buff, 0, bytesRead)
            }

            while (-1 != src.read(buff).also { bytesRead = it }) {
                dst.write(buff, 0, bytesRead)
            }

        } catch (ignored: SocketException) {
        } catch (e: Exception ) {
            testLogD("${loggingTag}: $e")
        } finally {
            try {
                srcSock.close()
            } catch (ignored: Exception) {}
        }
    }

}
