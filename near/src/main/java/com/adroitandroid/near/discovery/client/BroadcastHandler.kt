package com.adroitandroid.near.discovery.client

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.io.IOException
import java.net.*

class BroadcastHandler internal constructor(looper: Looper?,
                                            private val mHostName: String,
                                            private val mBroadcastInterval: Long): Handler(looper ?: Looper.getMainLooper()) {
    private var mSocket: DatagramSocket? = null

    override fun handleMessage(msg: Message) {
        if (msg.what == REPEAT_BROADCAST) {
            broadcast()
        } else if (msg.what == STOP_BROADCAST) {
            removeMessages(REPEAT_BROADCAST)
            mSocket!!.close()
        }
    }

    fun broadcast() {
        if (mSocket != null) {
            try {
                val sendData = mHostName.toByteArray()
                try {
                    val sendPacket = DatagramPacket(sendData, sendData.size,
                            InetAddress.getByName("255.255.255.255"), 8888)
                    mSocket!!.send(sendPacket)
                } catch (ignored: Exception) {
                }
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) {
                        continue
                    }
                    for (interfaceAddress in networkInterface.interfaceAddresses) {
                        val broadcast = interfaceAddress.broadcast ?: continue
                        try {
                            val sendPacket = DatagramPacket(sendData, sendData.size, broadcast, 8888)
                            mSocket!!.send(sendPacket)
                        } catch (ignored: Exception) {
                        }
                    }
                }
                sendEmptyMessageDelayed(REPEAT_BROADCAST, mBroadcastInterval)
            } catch (ignored: IOException) {
            }
        }
    }

    companion object {
        const val STOP_BROADCAST = 1235
        const val REPEAT_BROADCAST = 2345
    }

    init {
        try {
            mSocket = DatagramSocket()
            mSocket!!.broadcast = true
        } catch (ignored: SocketException) { }
    }
}