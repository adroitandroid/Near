package com.adroitandroid.near.discovery.server

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.collection.ArrayMap
import com.adroitandroid.near.model.Host
import java.io.IOException
import java.net.*

class UdpBroadcastListeningHandler internal constructor(looper: Looper) : Handler(looper) {
    private var mSocket: DatagramSocket? = null
    private var mListener: UdpBroadcastListener? = null
    private lateinit var mHostHandlerMap: ArrayMap<Host, StaleHostHandler>
    private var mCurrentIps: Set<String>? = null
    private var isHostClientToo = false
    private var mStaleTimeout: Long = 0


    private fun updateListenersTo(listener: UdpBroadcastListener) {
        mListener = listener
        synchronized(this@UdpBroadcastListeningHandler) {
            for (hostHandler: StaleHostHandler in mHostHandlerMap.values) {
                hostHandler.setListener(listener)
            }
        }
    }

    override fun handleMessage(msg: Message) {
        if (msg.what == LISTEN) {
            try {
                if (mSocket == null) {
                    mSocket = DatagramSocket(null)
                    val socketAddress = InetSocketAddress(InetAddress.getByName("0.0.0.0"), 8888)
                    mSocket!!.reuseAddress = true
                    mSocket!!.broadcast = true
                    mSocket!!.soTimeout = 0
                    mSocket!!.bind(socketAddress)
                }
                val recvBuf = ByteArray(15000)
                val packet = DatagramPacket(recvBuf, recvBuf.size)
                val socket = mSocket!!
                socket.receive(packet)
                val host = Host(packet.address, String(packet.data).trim { it <= ' ' })

                if (isHostClientToo || !mCurrentIps!!.contains(host.hostAddress)) {
                    var handler = mHostHandlerMap!![host]
                    if (handler == null) {
                        handler = StaleHostHandler(host, mHostHandlerMap, mListener)
                        synchronized(this@UdpBroadcastListeningHandler) { mHostHandlerMap!!.put(host, handler) }
                        if (mListener != null) {
                            Handler(Looper.getMainLooper()).post { mListener!!.onHostsUpdate(mHostHandlerMap!!.keys) }
                        }
                    } else if (hostNameChanged(host, mHostHandlerMap)) {
                        if (mListener != null) {
                            Handler(Looper.getMainLooper()).post { mListener!!.onHostsUpdate(mHostHandlerMap!!.keys) }
                        }
                    }
                    handler.removeMessages(StaleHostHandler.STALE_HOST)
                    handler.sendEmptyMessageDelayed(StaleHostHandler.STALE_HOST, mStaleTimeout)
                }
            } catch (e: SocketException) {
                e.printStackTrace()
                if (mSocket != null) {
                    mSocket!!.close()
                    mSocket = null
                }
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                if (mSocket != null) {
                    mSocket!!.close()
                    mSocket = null
                }
            } catch (e: IOException) {
                e.printStackTrace()
                if (mListener != null) {
                    Handler(Looper.getMainLooper()).post { mListener!!.onReceiveFailed() }
                }
            }
            if (looper.thread.isAlive) {
                sendEmptyMessage(LISTEN)
            }
        }
    }

    private fun hostNameChanged(updatedHost: Host, hostHandlerMap: ArrayMap<Host, StaleHostHandler>): Boolean {
        for (host: Host in hostHandlerMap.keys) {
            if (updatedHost.equals(host) && updatedHost.name != host.name) {
                hostHandlerMap[updatedHost] = hostHandlerMap.remove(host)
                return true
            }
        }
        return false
    }

    private fun stop() {
        for (handler: StaleHostHandler? in mHostHandlerMap.values) {
            handler!!.removeMessages(StaleHostHandler.STALE_HOST)
        }
        mListener = null
        mSocket!!.close()
        mSocket = null
        removeMessages(LISTEN)
        looper.quitSafely()
    }

    companion object {
        private const val LISTEN = 5678
        private var handler: UdpBroadcastListeningHandler? = null
        fun startBroadcastListening(hostHandlerMap: ArrayMap<Host, StaleHostHandler>,
                                    currentHostIps: Set<String>?,
                                    isHostClientToo: Boolean, staleTimeout: Long) {
            val handlerThread: HandlerThread = object : HandlerThread("ServerService") {
                override fun onLooperPrepared() {
                    handler = UdpBroadcastListeningHandler(looper)
                    handler!!.mHostHandlerMap = hostHandlerMap
                    handler!!.mCurrentIps = currentHostIps
                    handler!!.isHostClientToo = isHostClientToo
                    handler!!.mStaleTimeout = staleTimeout
                    handler!!.sendEmptyMessage(LISTEN)
                }
            }
            handlerThread.start()
        }

        fun setListener(listener: UdpBroadcastListener) {
            if (handler != null) {
                handler!!.updateListenersTo(listener)
            }
        }

        fun stopListeningForBroadcasts() {
            if (handler != null) {
                handler!!.stop()
                handler = null
            }
        }
    }
}