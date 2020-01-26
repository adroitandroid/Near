package com.adroitandroid.near.connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.adroitandroid.near.connect.client.TcpClientListener
import com.adroitandroid.near.connect.client.TcpClientService
import com.adroitandroid.near.connect.server.TcpServerListener
import com.adroitandroid.near.connect.server.TcpServerService
import com.adroitandroid.near.model.Host
import java.net.InetAddress

class NearConnectImpl(private val mContext: Context,
                      private val mListener: NearConnect.Listener,
                      private val mLooper: Looper,
                      private val mPeers: Set<Host>,
                      private val mPort: Int) : NearConnect {

    private var serverState = false
    private var sendDataQueue: MutableList<ByteArray> = mutableListOf()
    private var sendDestQueue: MutableList<Host> = mutableListOf()
    private var sendJobQueue: MutableList<Long> = mutableListOf()
    private var clientServiceListener: TcpClientListener? = null
    private var serverServiceListener: TcpServerListener? = null

    private val clientConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TcpClientService.TcpClientBinder) {
                service.setListener(getClientServiceListener(), Looper.myLooper() ?: Looper.getMainLooper())
                service.setPort(mPort)

                var candidateData: ByteArray? = null
                var candidateHost: Host? = null
                var jobId: Long = 0
                while (sendDataQueue.isNotEmpty()) {
                    synchronized(this@NearConnectImpl) {
                        if (sendDataQueue.isNotEmpty()) {
                            candidateData = sendDataQueue.removeAt(0)
                            candidateHost = sendDestQueue.removeAt(0)
                            jobId = sendJobQueue.removeAt(0)
                        }
                    }
                    service.send(candidateData!!, candidateHost!!, jobId)
                }
                mContext.unbindService(this)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    private fun getClientServiceListener(): TcpClientListener {
        if(clientServiceListener == null) {
            clientServiceListener = object: TcpClientListener {
                override fun onSendSuccess(jobId: Long) {
                    Handler(mLooper).post { mListener.onSendComplete(jobId) }
                }

                override fun onSendFailure(jobId: Long, e: Throwable?) {
                    Handler(mLooper).post { mListener.onSendFailure(e, jobId) }
                }
            }
        }
        return clientServiceListener!!
    }

    override fun send(bytes: ByteArray, peer: Host): Long {
        val jobId: Long = System.currentTimeMillis()
        synchronized(this@NearConnectImpl) {
            sendDataQueue.add(bytes)
            sendDestQueue.add(peer)
            sendJobQueue.add(jobId)
        }

        val intent = Intent(mContext.applicationContext, TcpClientService::class.java)
        mContext.startService(intent)
        mContext.bindService(intent, clientConnection, Context.BIND_AUTO_CREATE)
        return jobId
    }

    private val startServerConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if(service is TcpServerService.TcpServerBinder && serverState) {
                service.setListener(getServerServiceListener())
                service.setPort(mPort)
                service.startServer()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    private fun getServerServiceListener(): TcpServerListener {
        if(serverServiceListener == null) {
            serverServiceListener = object: TcpServerListener() {
                override fun onServerStartFailed(e: Throwable?) {
                    Handler(mLooper).post { mListener.onStartListenFailure(e) }
                }

                override fun onReceive(bytes: ByteArray, inetAddress: InetAddress) {
                    Handler(mLooper).post {
                        mPeers.forEach peerLoop@{
                            if (it.hostAddress == inetAddress.hostAddress) {
                                mListener.onReceive(bytes, it)
                                return@peerLoop
                            }
                        }
                    }
                }
            }
        }
        return serverServiceListener!!
    }

    private val stopServerConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TcpServerService.TcpServerBinder && !serverState) {
                service.stopServer()
                mContext.unbindService(this)
                mContext.unbindService(startServerConnection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    override fun startReceiving() {
        if (!serverState) {
            serverState = true
            val intent = Intent(mContext.applicationContext, TcpServerService::class.java)
            mContext.startService(intent)
            mContext.bindService(intent, startServerConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun stopReceiving(abortCurrentTransfers: Boolean) {
        //TODO("handle abort")
        if(serverState) {
            serverState = false
            val intent = Intent(mContext.applicationContext, TcpServerService::class.java)
            mContext.startService(intent)
            mContext.bindService(intent, stopServerConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override val peers: Set<Host> = mPeers
    override val isReceiving: Boolean = serverState
}