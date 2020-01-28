package com.adroitandroid.near.connect.server

import java.net.InetAddress

abstract class TcpServerListener {
    fun onStartFailure(e: Throwable?) {
        onServerStartFailed(e)
    }
    abstract fun onServerStartFailed(e: Throwable?)
    abstract fun onReceive(bytes: ByteArray, inetAddress: InetAddress)
}