package com.adroitandroid.near.connect.client;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import com.adroitandroid.near.connect.server.TcpServerService;
import com.adroitandroid.near.model.Host;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClientService extends Service {

    private PowerManager.WakeLock mWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return new TcpClientBinder();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpClientService");
    }

    private void send(final byte[] data,
                      final Host destination,
                      final TcpClientListener listener,
                      final Looper listenerLooper,
                      final long jobId) {
        InetAddress destAddress;
        Socket socket = null;
        mWakeLock.acquire(30*60*1000L);

        try {
            destAddress = InetAddress.getByName(destination.getHostAddress());
            socket = new Socket(destAddress, TcpServerService.SERVER_PORT);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.writeInt(data.length);
            dOut.write(data);

            new Handler(listenerLooper).post(new Runnable() {
                @Override
                public void run() {
                    listener.onSendSuccess(jobId);
                }
            });
        } catch (final IOException e) {
            e.printStackTrace();

            new Handler(listenerLooper).post(new Runnable() {
                @Override
                public void run() {
                    listener.onSendFailure(jobId, e);
                }
            });
        } finally {
            mWakeLock.release();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class TcpClientBinder extends Binder {
        private TcpClientListener mListener;
        private Looper mListenerLooper;

        public void send(final byte[] data, final Host destination, final long jobId) {
            new HandlerThread("TcpClientThread") {
                @Override
                protected void onLooperPrepared() {
                    new Handler(getLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            TcpClientService.this.send(data, destination, mListener, mListenerLooper, jobId);
                            getLooper().quitSafely();
                        }
                    });
                }
            }.start();
        }

        public void setListener(TcpClientListener listener, Looper looper) {
            mListener = listener;
            mListenerLooper = looper;
        }
    }
}
