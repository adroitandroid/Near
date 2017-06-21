package com.adroitandroid.near.connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.adroitandroid.near.model.Host;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by pv on 21/06/17.
 */

class NearConnectImpl implements NearConnect {
    private static final int SERVER_STARTED = 1;
    private static final int SERVER_STOPPED = 2;
    private final Context mContext;
    private final Listener mListener;
    private final Looper mListenerLooper;
    private final Set<Host> mPeers;
    private int mServerState;
    private List<byte[]> sendDataQueue = new ArrayList<>();
    private List<Host> sendDestQueue = new ArrayList<>();

    NearConnectImpl(Context context, Listener listener, Looper looper, Set<Host> peers) {
        mContext = context;
        mListener = listener;
        mListenerLooper = looper;
        mPeers = peers;
    }

    private ServiceConnection mClientConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof TcpClientService.TcpClientBinder) {
                TcpClientService.TcpClientBinder binder = (TcpClientService.TcpClientBinder) service;
                binder.setListener(new TcpClientService.Listener() {
                    @Override
                    public void onSendSuccess() {
                        if (mListenerLooper != null && mListener != null) {
                            new Handler(mListenerLooper).post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onSendComplete();
                                }
                            });
                        }
                    }

                    @Override
                    public void onSendFailure(final Throwable e) {
                        if (mListenerLooper != null && mListener != null) {
                            new Handler(mListenerLooper).post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onSendFailure(e);
                                }
                            });
                        }
                    }
                }, Looper.myLooper());
                byte[] candidateData = null;
                Host candidateHost = null;
                while (sendDataQueue.size() > 0) {
                    synchronized (this) {
                        if (sendDataQueue.size() > 0) {
                            candidateData = sendDataQueue.remove(0);
                            candidateHost = sendDestQueue.remove(0);
                        }
                    }
                    binder.send(candidateData, candidateHost);
                }
                mContext.unbindService(this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void send(byte[] bytes, Host peer) {
        synchronized (this) {
            sendDataQueue.add(bytes);
            sendDestQueue.add(peer);
        }

        Intent intent = new Intent(mContext.getApplicationContext(), TcpClientService.class);
        mContext.startService(intent);

        mContext.bindService(intent, mClientConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof TcpServerService.TcpServerBinder) {
                TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
                switch (mServerState) {
                    case SERVER_STARTED:
                        binder.setListener(new TcpServerService.TcpServerListener() {

                            @Override
                            public void onServerStartFailed(final Throwable e) {
                                if (mListener != null && mListenerLooper != null) {
                                    new Handler(mListenerLooper).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mListener.onStartListenFailure(e);
                                        }
                                    });
                                }
                            }

                            @Override
                            void onReceive(final byte[] bytes, final InetAddress inetAddress) {
                                if (mListener != null && mListenerLooper != null) {
                                    new Handler(mListenerLooper).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (Host peer : mPeers) {
                                                if (peer.getHostAddress().equals(inetAddress.getHostAddress())) {
                                                    mListener.onReceive(bytes, peer);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        });
                        binder.startServer();
                        break;
                    case SERVER_STOPPED:
                        binder.stopServer();
                        mContext.unbindService(this);
                        break;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void startReceiving() {
        if (mServerState != SERVER_STARTED) {
            mServerState = SERVER_STARTED;
            Intent intent = new Intent(mContext.getApplicationContext(), TcpServerService.class);
            mContext.startService(intent);

            mContext.bindService(intent, mServerConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void stopReceiving(boolean abortCurrentTransfers) {
        if (mServerState != SERVER_STOPPED) {
            mServerState = SERVER_STOPPED;
            Intent intent = new Intent(mContext.getApplicationContext(), TcpServerService.class);
            mContext.startService(intent);
        }
    }

    @Override
    public Set<Host> getPeers() {
        return mPeers;
    }
}
