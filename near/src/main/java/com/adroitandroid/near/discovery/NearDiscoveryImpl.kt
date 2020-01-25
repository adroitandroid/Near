package com.adroitandroid.near.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.collection.ArraySet
import com.adroitandroid.near.discovery.UdpServerService.UdpBroadcastListener
import com.adroitandroid.near.discovery.UdpServerService.UdpServerBinder
import com.adroitandroid.near.model.Host
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

internal class NearDiscoveryImpl(private val mDiscoverableTimeout: Long,
                                 private val mDiscoveryTimeout: Long,
                                 private val mPingInterval: Long,
                                 private val mListener: NearDiscovery.Listener,
                                 private val mListenerLooper: Looper,
                                 private val mContext: Context) : NearDiscovery {
    override var isDiscoverable: Boolean = false
        private set
    override var isDiscovering: Boolean = false
        private set

    private lateinit var mDiscoverableDisposable: Disposable
    private lateinit var mDiscoveryDisposable: Disposable
    private val mCurrentPeers: MutableSet<Host> = mutableSetOf()

    override fun makeDiscoverable(hostName: String) {
        if (!isDiscoverable) {
            beDiscoverable(hostName)
            mDiscoverableDisposable = Observable.timer(mDiscoverableTimeout, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe {
                        if (isDiscoverable) {
                            stopBeingDiscoverable()
                            Handler(mListenerLooper).post { mListener.onDiscoverableTimeout() }
                        }
                    }
        }
    }

    override fun makeNonDiscoverable() {
        if (isDiscoverable) {
            stopBeingDiscoverable()
            mDiscoverableDisposable.dispose()
        }
    }

    private fun beDiscoverable(hostName: String) {
        val intent = Intent(mContext.applicationContext, UdpBroadcastService::class.java)
        intent.putExtra(UdpBroadcastService.BUNDLE_NAME, hostName)
        intent.putExtra(UdpBroadcastService.BUNDLE_ACTION, UdpBroadcastService.ACTION_START_BROADCAST)
        intent.putExtra(UdpBroadcastService.BUNDLE_INTERVAL, mPingInterval)
        mContext.startService(intent)
        isDiscoverable = true
    }

    private fun stopBeingDiscoverable() {
        val intent = Intent(mContext.applicationContext, UdpBroadcastService::class.java)
        intent.putExtra(UdpBroadcastService.BUNDLE_ACTION, UdpBroadcastService.ACTION_STOP_BROADCAST)
        mContext.startService(intent)
        isDiscoverable = false
    }

    override fun startDiscovery() {
        if (!isDiscovering) {
            val intent = Intent(mContext.applicationContext, UdpServerService::class.java)
            intent.putExtra(UdpServerService.BUNDLE_COMMAND, UdpServerService.COMMAND_START_SERVER)
            intent.putExtra(UdpServerService.BUNDLE_STALE_TIMEOUT, mPingInterval * 2)
            mContext.startService(intent)
            isDiscovering = true
            mContext.bindService(intent, mServerConnection, Context.BIND_AUTO_CREATE)
            mDiscoveryDisposable = Observable.timer(mDiscoveryTimeout, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe {
                        if (isDiscovering) {
                            stopDiscovery()
                            Handler(mListenerLooper).post { mListener.onDiscoveryTimeout() }
                        }
                    }
        }
    }

    override fun stopDiscovery() {
        if (isDiscovering) {
            mContext.unbindService(mServerConnection)
            val intent = Intent(mContext.applicationContext, UdpServerService::class.java)
            intent.putExtra(UdpServerService.BUNDLE_COMMAND, UdpServerService.COMMAND_STOP_SERVER)
            mContext.startService(intent)
            isDiscovering = false
            mDiscoveryDisposable.dispose()
        }
    }

    private val mServerConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as UdpServerBinder
            binder.setBroadcastListener(object : UdpBroadcastListener {
                override fun onServerSetupFailed(e: Throwable) {
                    Handler(mListenerLooper).post { mListener.onDiscoveryFailure(e) }
                }

                override fun onReceiveFailed() {}
                override fun onHostsUpdate(currentHosts: Set<Host>) {
                    mCurrentPeers.retainAll(currentHosts)
                    mCurrentPeers.addAll(currentHosts)
                    Handler(mListenerLooper).post { mListener.onPeersUpdate(mCurrentPeers) }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override val allAvailablePeers: Set<Host>
        get() = mCurrentPeers
}