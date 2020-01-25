package com.adroitandroid.near.discovery.client

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.adroitandroid.near.model.Host

class UdpBroadcastService : Service() {
    private var broadcastThread: BroadcastThread? = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (ACTION_START_BROADCAST == intent.getStringExtra(BUNDLE_ACTION)) {
            broadcastThread?.stopBroadcast()
            broadcastThread = BroadcastThread(intent.getStringExtra(BUNDLE_NAME) ?: Host.DUMMY,
                    intent.getLongExtra(BUNDLE_INTERVAL, DEFAULT_BROADCAST_INTERVAL))
            broadcastThread!!.start()
        } else if (ACTION_STOP_BROADCAST == intent.getStringExtra(BUNDLE_ACTION)) {
            broadcastThread?.stopBroadcast()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val BUNDLE_NAME = "bundle_name"
        const val BUNDLE_ACTION = "bundle_action"
        const val BUNDLE_INTERVAL = "bundle_interval"
        const val ACTION_START_BROADCAST = "start_broadcast"
        const val ACTION_STOP_BROADCAST = "stop_broadcast"
        private const val DEFAULT_BROADCAST_INTERVAL: Long = 5000
    }
}