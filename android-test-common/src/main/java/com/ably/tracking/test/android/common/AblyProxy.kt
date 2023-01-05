package com.ably.tracking.test.android.common

import io.ably.lib.types.ClientOptions
import io.ably.lib.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import javax.net.ssl.SSLSocketFactory

private const val AGENT_HEADER_NAME = "ably-asset-tracking-android-publisher-tests"

private const val PROXY_HOST = "localhost"
private const val PROXY_PORT = 13579
private const val REALTIME_HOST = "realtime.ably.io"
private const val REALTIME_PORT = 443


/**
 * A local proxy that can be used to intercept Realtime traffic for testing
 */
interface RealtimeProxy {
    /**
     * Start the proxy listening for connections
     */
    fun start()

    /**
     * Stop the proxy and close any active connetions
     */
    fun stop()

    /**
     * Ably ClientOptions that have been configured to direct traffic
     * through this proxy service
     */
    val clientOptions: ClientOptions
}

/**
 * A TCP Proxy, which can run locally and intercept traffic to Ably realtime.
 *
 * This proxy is only capable of simulating faults at the transport layer, such
 * as connections being interrupted or packets being dropped entirely.
 */
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

    /**
     * Block current thread and wait for a new incoming client connection on the server socket.
     * Returns a connection object when a client has connected.
     */
    private fun accept() : Layer4ProxyConnection {
        val clientSock = server?.accept()
        testLogD( "$loggingTag: accepted connection")

        val serverSock = sslSocketFactory.createSocket(targetAddress, targetPort)
        val conn = Layer4ProxyConnection(serverSock, clientSock!!, targetAddress, parentProxy = this)
        connections.add(conn)
        return conn
    }

    /**
     * Pre-configured client options to configure AblyRealtime to send traffic locally through
     * this proxy. Note that TLS is disabled, so that the proxy can act as a man in the middle.
     */
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

    /**
     * Close open connections and stop listening for new local connections
     */
    override fun stop() {
        server?.close()
        server = null

        connections.forEach {
            it.stop()
        }
        connections.clear()
    }

    /**
     * Begin a background thread listening for local Realtime connections
     */
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

/**
 * A TCP Proxy connection between a local client and the remote Ably service.
 */
internal class Layer4ProxyConnection(
    private val server: Socket,
    private val client: Socket,
    private val targetHost: String,
    private val parentProxy: Layer4Proxy
) {

    private val loggingTag = "Layer4ProxyConnection"

    /**
     * Starts two threads, one forwarding traffic in each direction between
     * the local client and the Ably Realtime service.
     */
    fun run() {
        Thread { proxy(server, client, true) }.start()
        Thread { proxy(client, server) }.start()
    }

    /**
     * Close socket connections, causing proxy threads to exit.
     */
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

    /**
     * Copies traffic between source and destination sockets, rewriting the
     * HTTP host header if requested to remove the proxy host details.
     */
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
