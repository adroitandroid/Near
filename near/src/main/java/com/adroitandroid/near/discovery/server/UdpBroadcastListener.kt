package com.adroitandroid.near.discovery.server

import com.adroitandroid.near.model.Host

interface UdpBroadcastListener {
    fun onServerSetupFailed(e: Throwable)
    fun onReceiveFailed()
    fun onHostsUpdate(currentHosts: Set<Host>)
}