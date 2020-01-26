package com.adroitandroid.near.discovery.client

import android.os.Handler
import android.os.HandlerThread
import org.json.JSONObject

class BroadcastThread internal constructor(private val mHostJson: JSONObject,
                                           private val mBroadcastInterval: Long,
                                           private val port: Int) : HandlerThread("Broadcaster") {
    private lateinit var mHandler: Handler
    override fun onLooperPrepared() {
        mHandler = BroadcastHandler(looper, mHostJson, mBroadcastInterval, port)
        broadcast()
    }

    fun broadcast() {
        mHandler.sendEmptyMessage(BroadcastHandler.REPEAT_BROADCAST)
    }

    fun stopBroadcast() {
        if (isAlive) {
            mHandler.sendEmptyMessage(BroadcastHandler.STOP_BROADCAST)
            quitSafely()
        }
    }

}