package com.ably.tracking.test.common

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class AblyProxy
constructor(
    listenPort: Int,
    private val targetAddress: String,
    private val targetPort: Int,
){
    private val server: ServerSocket = ServerSocket(listenPort)

    fun accept() : AblyConnection {
        val clientSock = server.accept()
        val serverSock = Socket(targetAddress, targetPort)
        return AblyConnection(serverSock, clientSock)
    }

    fun close() {
        server.close()
    }
}

class AblyConnection
constructor(
    private val server: Socket,
    private val client: Socket,
) {

    suspend fun run() = coroutineScope{
        launch { proxy(server, client) } // TODO snoop on packets
        launch { proxy(client, server) }
    }

    fun stop() {
        try {
            server.close()
        } catch (ignored: Exception) {}

        try {
            client.close()
        } catch (ignored: Exception) {}
    }
    
    private fun proxy(dstSock: Socket , srcSock: Socket) {
        try {
            val dst = dstSock.getOutputStream()
            val src = srcSock.getInputStream()
            val buff = ByteArray(4096)
            var bytesRead: Int
            while (-1 != src.read(buff).also { bytesRead = it }) {
                dst.write(buff, 0, bytesRead)
            }
        } catch (ignored: SocketException) {
        } catch (e: Exception ) {
            e.printStackTrace();
        } finally {
            try {
                srcSock.close();
            } catch (ignored: Exception) {}
        }
    }

}
