package com.adroitandroid.near.connect.client

interface TcpClientListener {
    fun onSendSuccess(jobId: Long)
    fun onSendFailure(jobId: Long, e: Throwable?)
}