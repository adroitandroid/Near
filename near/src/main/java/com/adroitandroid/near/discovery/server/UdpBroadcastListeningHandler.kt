package com.adroitandroid.near.discovery.server

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.collection.ArrayMap
import com.adroitandroid.near.model.Host
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.*

class UdpBroadcastListeningHandler internal constructor(looper: Looper) : Handler(looper) {
    private var mSocket: DatagramSocket? = null
    private var mListener: UdpBroadcastListener? = null
    private lateinit var mHostHandlerMap: ArrayMap<Host, StaleHostHandler>
    private lateinit var mCurrentIps: Set<String>
    private var isHostClientToo = false
    private var mStaleTimeout: Long = 0
    private var mPort: Int = UdpServerService.DISCOVERY_PORT
    private var mRegex: Regex = Regex("^$")


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
                    val socketAddress = InetSocketAddress(InetAddress.getByName("0.0.0.0"), mPort)
                    mSocket!!.apply {
                        this.reuseAddress = true
                        this.broadcast = true
                        this.soTimeout = 0
                    }.bind(socketAddress)
                }
                val recvBuf = ByteArray(15000)
                val packet = DatagramPacket(recvBuf, recvBuf.size)
                val socket = mSocket!!
                socket.receive(packet)

                try {
                    val jsonObject = JSONObject(String(packet.data).trim { it <= ' ' })
                    val host = Host(packet.address, jsonObject.getString(Host.JSON_NAME), jsonObject.getString(Host.JSON_FILTER_TEXT))

                    if ((isHostClientToo || !mCurrentIps.contains(host.hostAddress)) && hostMatchesFilter(host.filterText.trim { it <= ' ' })) {
                        var handler = mHostHandlerMap[host]
                        if (handler == null) {
                            handler = StaleHostHandler(host, mHostHandlerMap, mListener)
                            synchronized(this@UdpBroadcastListeningHandler) { mHostHandlerMap.put(host, handler) }
                            Handler(Looper.getMainLooper()).post { mListener?.onHostsUpdate(mHostHandlerMap.keys) }
                        } else if (hostNameChanged(host, mHostHandlerMap)) {
                            Handler(Looper.getMainLooper()).post { mListener?.onHostsUpdate(mHostHandlerMap.keys) }
                        }
                        handler.removeMessages(StaleHostHandler.STALE_HOST)
                        handler.sendEmptyMessageDelayed(StaleHostHandler.STALE_HOST, mStaleTimeout)
                    }
                } catch (ignored: JSONException) { }
            } catch (e: SocketException) {

                e.printStackTrace()
                mSocket?.close()
                mSocket = null

            } catch (e: UnknownHostException) {

                e.printStackTrace()
                mSocket?.close()
                mSocket = null

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
            if (updatedHost == host && updatedHost.name != host.name) {
                hostHandlerMap[updatedHost] = hostHandlerMap.remove(host)
                return true
            }
        }
        return false
    }

    private fun hostMatchesFilter(filterText: String): Boolean {
        return filterText.matches(mRegex)
    }

    private fun stop() {
        for (handler: StaleHostHandler? in mHostHandlerMap.values) {
            handler!!.removeMessages(StaleHostHandler.STALE_HOST)
        }
        mListener = null
        mSocket?.close()
        mSocket = null
        removeMessages(LISTEN)
        looper.quitSafely()
    }

    companion object {
        private const val LISTEN = 5678
        private var handler: UdpBroadcastListeningHandler? = null

        fun startBroadcastListening(hostHandlerMap: ArrayMap<Host, StaleHostHandler>,
                                    currentHostIps: Set<String>,
                                    isHostClientToo: Boolean,
                                    staleTimeout: Long,
                                    port: Int,
                                    regex: Regex) {
            val handlerThread: HandlerThread = object : HandlerThread("ServerService") {
                override fun onLooperPrepared() {
                    handler = UdpBroadcastListeningHandler(looper)
                    handler!!.apply {
                        this.mHostHandlerMap = hostHandlerMap
                        this.mCurrentIps = currentHostIps
                        this.isHostClientToo = isHostClientToo
                        this.mStaleTimeout = staleTimeout
                        this.mPort = port
                        this.mRegex = regex
                    }.sendEmptyMessage(LISTEN)
                }
            }
            handlerThread.start()
        }

        fun setListener(listener: UdpBroadcastListener) {
            handler?.updateListenersTo(listener)
        }

        fun stopListeningForBroadcasts() {
            handler?.stop()
            handler = null
        }
    }
}