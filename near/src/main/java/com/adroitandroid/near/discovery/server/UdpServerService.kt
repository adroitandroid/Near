package com.adroitandroid.near.discovery.server

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import com.adroitandroid.near.model.Host
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class UdpServerService : Service() {
    private var mBinder: UdpServerBinder? = null
    private val mHostHandlerMap = ArrayMap<Host, StaleHostHandler>()
    private val mCurrentHostIps = Collections.synchronizedSet(ArraySet<String>())
    private lateinit var mConnectivityChangeReceiver: ConnectivityChangeReceiver
    private var mStaleTimeout: Long = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (COMMAND_START_SERVER == intent.getStringExtra(BUNDLE_COMMAND)) {
            mStaleTimeout = intent.getLongExtra(BUNDLE_STALE_TIMEOUT, 10000)
            startBroadcastListening(intent.getBooleanExtra(BUNDLE_IS_HOST_CLIENT, false),
                    intent.getIntExtra(BUNDLE_DISCOVERY_PORT, 8888))
        } else if (COMMAND_STOP_SERVER == intent.getStringExtra(BUNDLE_COMMAND)) {
            UdpBroadcastListeningHandler.stopListeningForBroadcasts()
            stopSelf()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        initCurrentDeviceIps()
        val connectivityChangeFilter = IntentFilter()
        connectivityChangeFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mConnectivityChangeReceiver = ConnectivityChangeReceiver()
        registerReceiver(mConnectivityChangeReceiver, connectivityChangeFilter)
    }

    private fun initCurrentDeviceIps() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            val updatedIps: MutableSet<String> = ArraySet()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                try {
                    if (networkInterface.isLoopback || !networkInterface.isUp) {
                        continue
                    }
                    val inetAddresses = networkInterface.inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val inetAddress = inetAddresses.nextElement()
                        updatedIps.add(inetAddress.hostAddress)
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
            }
            mCurrentHostIps.retainAll(updatedIps)
            mCurrentHostIps.addAll(updatedIps)
        } catch (e: SocketException) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mConnectivityChangeReceiver)
    }

    override fun onBind(intent: Intent): IBinder? {
        mBinder = mBinder ?: UdpServerBinder()
        return mBinder
    }

    private fun startBroadcastListening(isHostClientToo: Boolean, port: Int) {
        UdpBroadcastListeningHandler.startBroadcastListening(mHostHandlerMap, mCurrentHostIps,
                isHostClientToo, mStaleTimeout, port)
    }

    inner class UdpServerBinder : Binder() {
        fun setBroadcastListener(listener: UdpBroadcastListener) {
            UdpBroadcastListeningHandler.setListener(listener)
        }

        fun stopBroadcastListening() {
            UdpBroadcastListeningHandler.stopListeningForBroadcasts()
        }
    }

    private inner class ConnectivityChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            initCurrentDeviceIps()
        }
    }

    companion object {
        const val BUNDLE_COMMAND = "bundle_command"
        const val BUNDLE_STALE_TIMEOUT = "bundle_stale_timeout"
        const val BUNDLE_DISCOVERY_PORT = "bundle_discovery_port"
        private const val BUNDLE_IS_HOST_CLIENT = "bundle_host_is_client_too"
        const val COMMAND_START_SERVER = "start_server"
        const val COMMAND_STOP_SERVER = "stop_server"
    }
}