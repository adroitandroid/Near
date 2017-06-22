package com.adroitandroid.near.connect;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by pv on 21/06/17.
 */

public class TcpServerService extends Service {
    static final int SERVER_PORT = 6789;
    private boolean mStarted;
    private PowerManager.WakeLock mWakeLock;
    private ServerSocket mServerSocket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new TcpServerBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpServerService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Dummy, main implementation is in the binder
        return START_STICKY;
    }

    private void startServer(final TcpServerListener listener) {
        mStarted = true;
        mWakeLock.acquire();
        final Looper myLooper = Looper.myLooper();

        new HandlerThread("TcpServerThread") {
            @Override
            protected void onLooperPrepared() {
                new Handler(getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mServerSocket = null;
                        try {
                            mServerSocket = new ServerSocket();
                            mServerSocket.setReuseAddress(true);
                            mServerSocket.setSoTimeout(0);
                            mServerSocket.bind(new InetSocketAddress(SERVER_PORT));

                            while (mStarted) {
                                try {
                                    Socket connectionSocket = mServerSocket.accept();
                                    onNewReceive(connectionSocket, myLooper, listener);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            listener.onStartFailure(e);
                        } finally {
                            if (mServerSocket != null && !mServerSocket.isClosed()) {
                                try {
                                    mServerSocket.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            getLooper().quitSafely();
                        }
                    }
                });
            }
        }.start();
    }

    private void onNewReceive(final Socket connectionSocket,
                              final Looper myLooper,
                              final TcpServerListener listener) {
        new HandlerThread("ClientServingThread") {
            @Override
            protected void onLooperPrepared() {
                new Handler(getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        byte[] bytes = null;
                        try {
                            DataInputStream dataInputStream = new DataInputStream(connectionSocket.getInputStream());

                            int length = dataInputStream.readInt(); // read length of incoming message
                            if (length > 0) {
                                bytes = new byte[length];
                                dataInputStream.readFully(bytes, 0, bytes.length); // read the message
                            }
                            onReceive(myLooper, listener, bytes, connectionSocket.getInetAddress());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getLooper().quitSafely();
                    }
                });
            }
        }.start();
    }

    private void onReceive(Looper looper,
                           final TcpServerListener listener,
                           final byte[] bytes,
                           final InetAddress inetAddress) {
        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                listener.onReceive(bytes, inetAddress);
            }
        });
    }

    private void stopServer() {
        mStarted = false;
        mWakeLock.release();
        new HandlerThread("ServerTerminator") {
            @Override
            protected void onLooperPrepared() {
                new Handler(getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mServerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();
    }

    static abstract class TcpServerListener {

        void onStartFailure(Throwable e) {
            onServerStartFailed(e);
        }

        abstract void onServerStartFailed(Throwable e);

        abstract void onReceive(byte[] bytes, InetAddress inetAddress);
    }

    class TcpServerBinder extends Binder {
        private TcpServerListener mListener;

        void setListener(TcpServerListener listener) {
            mListener = listener;
        }

        void startServer() {
            TcpServerService.this.startServer(mListener);
        }

        void stopServer() {
            TcpServerService.this.stopServer();
        }
    }
}
