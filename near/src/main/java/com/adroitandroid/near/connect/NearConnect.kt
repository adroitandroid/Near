package com.adroitandroid.near.connect

import android.content.Context
import android.os.Looper
import com.adroitandroid.near.discovery.NearDiscovery
import com.adroitandroid.near.model.Host

interface NearConnect {
    fun send(bytes: ByteArray, peer: Host): Long
    fun startReceiving()
    fun stopReceiving(abortCurrentTransfers: Boolean)
    val peers: Set<Host>
    val isReceiving: Boolean

    class Builder {
        private lateinit var mContext: Context
        private lateinit var mListener: Listener
        private lateinit var mListenerLooper: Looper
        private lateinit var mPeers: Set<Host>
        fun setContext(context: Context): Builder {
            mContext = context
            return this
        }

        fun setListener(listener: Listener, listenerLooper: Looper): Builder {
            mListener = listener
            mListenerLooper = listenerLooper
            return this
        }

        fun fromDiscovery(discovery: NearDiscovery): Builder {
            mPeers = discovery.allAvailablePeers
            return this
        }

        fun forPeers(peers: Set<Host>): Builder {
            mPeers = peers
            return this
        }

        fun build(): NearConnect {
            return NearConnectImpl(mContext, mListener, mListenerLooper, mPeers)
        }
    }

    interface Listener {
        fun onReceive(bytes: ByteArray, sender: Host)
        fun onSendComplete(jobId: Long)
        fun onSendFailure(e: Throwable?, jobId: Long)
        fun onStartListenFailure(e: Throwable?)
    }
}