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

import com.adroitandroid.near.model.Host;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by pv on 21/06/17.
 */

public class TcpClientService extends Service {

    private PowerManager.WakeLock mWakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new TcpClientBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpClientService");
    }

    private void send(final byte[] data,
                      final Host destination,
                      final Listener listener,
                      final Looper listenerLooper,
                      final long jobId) {
        InetAddress destAddress;
        Socket socket = null;
        mWakeLock.acquire();

        try {
            destAddress = InetAddress.getByName(destination.getHostAddress());
            socket = new Socket(destAddress, TcpServerService.SERVER_PORT);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.writeInt(data.length); // write length of the message
            dOut.write(data);           // write the message

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

    class TcpClientBinder extends Binder {
        private Listener mListener;
        private Looper mListenerLooper;

        void send(final byte[] data, final Host destination, final long jobId) {
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

        void setListener(Listener listener, Looper looper) {
            mListener = listener;
            mListenerLooper = looper;
        }
    }

    interface Listener {
        void onSendSuccess(long jobId);

        void onSendFailure(long jobId, Throwable e);
    }
}
