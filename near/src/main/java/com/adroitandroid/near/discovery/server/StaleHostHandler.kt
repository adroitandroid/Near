package com.adroitandroid.near.discovery.server

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.collection.ArrayMap
import com.adroitandroid.near.model.Host

class StaleHostHandler internal constructor(private val mHost: Host, private val mMap: ArrayMap<Host, StaleHostHandler>,
                                            private var mListener: UdpBroadcastListener?) : Handler() {
    override fun handleMessage(msg: Message) {
        if (msg.what == STALE_HOST) {
            mMap.remove(mHost)
            Handler(Looper.getMainLooper()).post { mListener?.onHostsUpdate(mMap.keys) }
        }
    }

    fun setListener(listener: UdpBroadcastListener) {
        mListener = listener
    }

    companion object {
        const val STALE_HOST = 9284
    }

}