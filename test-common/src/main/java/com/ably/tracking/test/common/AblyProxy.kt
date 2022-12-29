package com.ably.tracking.test.common

import org.msgpack.core.MessageFormat
import org.msgpack.core.MessagePack
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.net.ssl.SSLSocketFactory
import kotlin.experimental.and


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

            // TODO check the message ends with CLRF CLRF or save off the start of the ws payload

            // HTTP is plaintext so we can just read it
            val msg = String(buff, 0, bytesRead)
            logHandler("PROXY-MSG: " + String(buff.copyOfRange(0, bytesRead)))
            if (rewriteHost) {
                val newMsg = msg.replace("localhost:13579", targetHost)
                val newBuff = newMsg.toByteArray()
                dst.write(newBuff, 0, newBuff.size)
            } else {
                dst.write(buff, 0, bytesRead)
            }


            while (-1 != src.read(buff).also { bytesRead = it }) {
                // get the length of the websocket frame in bytes:
                //      0                   1                   2                   3
                //      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                //     +-+-+-+-+-------+-+-------------+-------------------------------+
                //     |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
                //     |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
                //     |N|V|V|V|       |S|             |   (if payload len==126/127)   |
                //     | |1|2|3|       |K|             |                               |
                //     +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
                //     |     Extended payload length continued, if payload len == 127  |
                //     + - - - - - - - - - - - - - - - +-------------------------------+
                //     |                               |Masking-key, if MASK set to 1  |
                //     +-------------------------------+-------------------------------+
                //     | Masking-key (continued)       |          Payload Data         |
                //     +-------------------------------- - - - - - - - - - - - - - - - +
                //     :                     Payload Data continued ...                :
                //     + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
                //     |                     Payload Data continued ...                |
                //     +---------------------------------------------------------------+
                var dataOff = 2
                var payloadLen = buff[1].and(0x7F).toUInt()
                if (payloadLen == 126U) {
                    dataOff += 2
                    payloadLen = bigEndianConversion(buff.copyOfRange(2, 4), logHandler)
                } else if (payloadLen == 127U) {
                    dataOff += 4
                    payloadLen = bigEndianConversion(buff.copyOfRange(2, 10), logHandler)
                }
                var mask = buff[1].toInt().and(0x01) == 1
                if (mask) {
                    dataOff +=4
                }
                val op = buff[0].toUInt().and(0x0Fu)
                if (op<3u) {
                    logHandler("PROXY-MSG: payload length: " + payloadLen + " data offset: " + dataOff + " buff len: " + bytesRead)
                    val unpacker = MessagePack.newDefaultUnpacker(
                        buff.copyOfRange(
                            dataOff,
                            dataOff + payloadLen.toInt()
                        )
                    )
                    if (unpacker.hasNext()) {
                        try {
                            logHandler("PROXY-MSG: " + unpacker.unpackValue())
                        } catch (e: Exception) {
                            logHandler("PROXY-MSG: unpacking msg " + e.message)
                        }
                    }
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

fun bigEndianConversion(bytes: ByteArray, logHandler: (String)->Unit): UInt {
    var result = 0U
    val size = bytes.size
    for (i in bytes.indices) {
        // Kotlin does stupid things when converting from byte to uint and fills the preceding bits with 1s so remove them
        logHandler("PROXY-DECODE: byte: "+ i + " val: " + bytes[i].toUInt().and(0xFFu))
        result = result or (bytes[i].toUInt().and(0xFFu) shl (8 * (size - i -1)))
    }
    return result
}
