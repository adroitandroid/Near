package com.adroitandroid.near.connect.client

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.os.PowerManager.WakeLock
import com.adroitandroid.near.connect.server.TcpServerService
import com.adroitandroid.near.model.Host
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

class TcpClientService : Service() {
    private lateinit var mWakeLock: WakeLock
    override fun onBind(intent: Intent): IBinder? {
        return TcpClientBinder()
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpClientService")
    }

    private fun send(port: Int,
                     data: ByteArray,
                     destination: Host,
                     listener: TcpClientListener,
                     listenerLooper: Looper,
                     jobId: Long) {
        val destAddress: InetAddress
        var socket: Socket? = null
        mWakeLock.acquire(30 * 60 * 1000L)
        try {
            destAddress = InetAddress.getByName(destination.hostAddress)
            socket = Socket(destAddress, port)
            val dOut = DataOutputStream(socket.getOutputStream())
            dOut.writeInt(data.size)
            dOut.write(data)
            Handler(listenerLooper).post { listener.onSendSuccess(jobId) }
        } catch (e: IOException) {
            e.printStackTrace()
            Handler(listenerLooper).post { listener.onSendFailure(jobId, e) }
        } finally {
            mWakeLock.release()
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class TcpClientBinder : Binder() {
        private var mListener: TcpClientListener? = null
        private var mListenerLooper: Looper? = null
        private var mPort = TcpServerService.SERVER_PORT

        fun send(data: ByteArray, destination: Host, jobId: Long) {
            object : HandlerThread("TcpClientThread") {
                override fun onLooperPrepared() {
                    Handler(looper).post {
                        this@TcpClientService.send(mPort, data, destination, mListener!!, mListenerLooper!!, jobId)
                        looper.quitSafely()
                    }
                }
            }.start()
        }

        fun setListener(listener: TcpClientListener, looper: Looper) {
            mListener = listener
            mListenerLooper = looper
        }

        fun setPort(port: Int) {
            mPort = port
        }
    }
}