package com.adroitandroid.near.connect.server

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.os.PowerManager.WakeLock
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TcpServerService : Service() {
    private var mStarted = false
    private lateinit var mWakeLock: WakeLock
    private var mServerSocket: ServerSocket? = null

    override fun onBind(intent: Intent): IBinder? {
        return TcpServerBinder()
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpServerService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startServer(port: Int, listener: TcpServerListener) {
        mStarted = true
        mWakeLock.acquire(30 * 60 * 1000L)
        val myLooper = Looper.myLooper() ?: Looper.getMainLooper()

        object : HandlerThread("TcpServerThread") {
            override fun onLooperPrepared() {
                Handler(looper).post {
                    mServerSocket = null
                    try {
                        mServerSocket = ServerSocket()
                        mServerSocket!!.reuseAddress = true
                        mServerSocket!!.soTimeout = 0
                        mServerSocket!!.bind(InetSocketAddress(port))
                        while (mStarted) {
                            try {
                                val connectionSocket = mServerSocket!!.accept()
                                onNewReceive(connectionSocket, myLooper, listener)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        listener.onStartFailure(e)
                    } finally {
                        if (mServerSocket != null && !mServerSocket!!.isClosed) {
                            try {
                                mServerSocket!!.close()
                            } catch (e1: IOException) {
                                e1.printStackTrace()
                            }
                        }
                        looper.quitSafely()
                    }
                }
            }
        }.start()
    }

    private fun onNewReceive(connectionSocket: Socket,
                             myLooper: Looper,
                             listener: TcpServerListener) {

        object : HandlerThread("ClientServingThread") {
            override fun onLooperPrepared() {
                Handler(looper).post {
                    try {
                        val dataInputStream = DataInputStream(connectionSocket.getInputStream())
                        val length = dataInputStream.readInt()
                        if (length > 0) {
                            val bytes = ByteArray(length)
                            dataInputStream.readFully(bytes, 0, bytes.size)
                            onReceive(myLooper, listener, bytes, connectionSocket.inetAddress)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    looper.quitSafely()
                }
            }
        }.start()
    }

    private fun onReceive(looper: Looper,
                          listener: TcpServerListener,
                          bytes: ByteArray,
                          inetAddress: InetAddress) {
        Handler(looper).post { listener.onReceive(bytes, inetAddress) }
    }

    private fun stopServer() {
        mStarted = false
        mWakeLock.release()
        object : HandlerThread("ServerTerminator") {
            override fun onLooperPrepared() {
                Handler(looper).post {
                    try {
                        mServerSocket?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    inner class TcpServerBinder : Binder() {
        private var mListener: TcpServerListener? = null
        private var mPort: Int = SERVER_PORT

        fun setListener(listener: TcpServerListener) {
            mListener = listener
        }

        fun setPort(port: Int) {
            mPort = port
        }

        fun startServer() {
            this@TcpServerService.startServer(mPort, mListener!!)
        }

        fun stopServer() {
            this@TcpServerService.stopServer()
        }
    }

    companion object {
        const val SERVER_PORT = 6789
    }
}