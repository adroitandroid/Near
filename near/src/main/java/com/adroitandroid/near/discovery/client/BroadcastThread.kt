package com.adroitandroid.near.discovery.client

import android.os.Handler
import android.os.HandlerThread

class BroadcastThread internal constructor(private val mHostName: String, private val mBroadcastInterval: Long) : HandlerThread("Broadcaster") {
    private lateinit var mHandler: Handler
    override fun onLooperPrepared() {
        mHandler = BroadcastHandler(looper, mHostName, mBroadcastInterval)
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