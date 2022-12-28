package com.ably.tracking.test.common

import android.R.attr.host
import android.R.attr.port
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.net.SocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


class AblyProxy
constructor (
    listenPort: Int,
    private val targetAddress: String,
    private val targetPort: Int,
){
    private val server: ServerSocket = ServerSocket(listenPort)
    private  val sslsocketfactory = SSLSocketFactory.getDefault()

    fun accept() : AblyConnection {
        val clientSock = server.accept()
        Log.d("PROXY", "accepted connection")

        val serverSock = sslsocketfactory.createSocket(targetAddress, targetPort)
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

    fun run() {
        Thread { proxy(server, client) }.start() // TODO snoop on packets
        Thread { proxy(client, server) }.start()
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
