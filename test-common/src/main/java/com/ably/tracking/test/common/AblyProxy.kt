package com.ably.tracking.test.common

import org.msgpack.core.MessageFormat
import org.msgpack.core.MessagePack
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.net.ssl.SSLSocketFactory


class AblyProxy
constructor (
    listenPort: Int,
    private val targetAddress: String,
    private val targetPort: Int,
    private val logHandler: (String)->Unit,
){
    private val server: ServerSocket = ServerSocket(listenPort)
    private  val sslsocketfactory = SSLSocketFactory.getDefault()

    fun accept() : AblyConnection {
        val clientSock = server.accept()
        logHandler( "PROXY accepted connection")

        val serverSock = sslsocketfactory.createSocket(targetAddress, targetPort)
        return AblyConnection(serverSock, clientSock, targetAddress, logHandler)
    }

    fun close() {
        server.close()
    }
}

class AblyConnection
constructor(
    private val server: Socket,
    private val client: Socket,
    private val targetHost: String,
    private val logHandler: (String)->Unit,
) {

    fun run() {
        Thread { proxy(server, client, true) }.start() // TODO snoop on packets
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
            logHandler("PROXY-MSG: " + buff.copyOfRange(0, bytesRead +1).asList())
            if (rewriteHost) {
                val newMsg = msg.replace("localhost:13579", targetHost)
                val newBuff = newMsg.toByteArray()
                dst.write(newBuff, 0, newBuff.size)
            } else {
                dst.write(buff, 0, bytesRead)
            }


            while (-1 != src.read(buff).also { bytesRead = it }) {
                logHandler("PROXY-MSG: " + buff.copyOfRange(0, bytesRead +1).asList())
                val unpacker = MessagePack.newDefaultUnpacker(buff.copyOfRange(0, bytesRead +1))
                if (unpacker.hasNext()) {
                    logHandler("PROXY-MSG: " + unpacker.unpackValue())
                }
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
