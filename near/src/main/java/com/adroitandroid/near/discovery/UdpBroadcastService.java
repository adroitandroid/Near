package com.adroitandroid.near.discovery;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by pv on 20/06/17.
 */

public class UdpBroadcastService extends Service {

    public static final String BUNDLE_NAME = "bundle_name";
    public static final String BUNDLE_ACTION = "bundle_action";
    public static final String BUNDLE_INTERVAL = "bundle_interval";
    public static final String ACTION_START_BROADCAST = "start_broadcast";
    public static final String ACTION_STOP_BROADCAST = "stop_broadcast";
    private static final long DEFAULT_BROADCAST_INTERVAL = 5000;
    private BroadcastThread broadcastThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START_BROADCAST.equals(intent.getStringExtra(BUNDLE_ACTION))) {
                if (broadcastThread != null) {
                    broadcastThread.stopBroadcast();
                }
                broadcastThread = new BroadcastThread(intent.getStringExtra(BUNDLE_NAME),
                        intent.getLongExtra(BUNDLE_INTERVAL, DEFAULT_BROADCAST_INTERVAL));
                broadcastThread.start();
            } else if (ACTION_STOP_BROADCAST.equals(intent.getStringExtra(BUNDLE_ACTION))) {
                if (broadcastThread != null) {
                    broadcastThread.stopBroadcast();
                }
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class BroadcastHandler extends Handler {
        static final int STOP_BROADCAST = 1235;
        private static final int REPEAT_BROADCAST = 2345;
        private final String mHostName;
        private final long mBroadcastInterval;
        private DatagramSocket mSocket;

        BroadcastHandler(Looper looper, String hostName, long broadcastInterval) {
            super(looper);
            mHostName = hostName;
            mBroadcastInterval = broadcastInterval;
            //Open a random port to send the package
            try {
                mSocket = new DatagramSocket();
                mSocket.setBroadcast(true);
            } catch (SocketException e) {
//                TODO: handle
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REPEAT_BROADCAST) {
                broadcast();
            } else if (msg.what == STOP_BROADCAST) {
                removeMessages(REPEAT_BROADCAST);
                mSocket.close();
            }
        }

        void broadcast() {
            if (mSocket != null) {
                // Find the server using UDP broadcast
                try {
                    byte[] sendData = mHostName.getBytes();

                    //Try the 255.255.255.255 first
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                InetAddress.getByName("255.255.255.255"), 8888);
                        mSocket.send(sendPacket);
                    } catch (Exception e) {
//                    TODO: handle
                    }

                    // Broadcast the message over all the network interfaces
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();

                        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                            continue; // Don't want to broadcast to the loopback interface
                        }

                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast == null) {
                                continue;
                            }

                            // Send the broadcast package!
                            try {
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                                mSocket.send(sendPacket);
                            } catch (Exception e) {
//                            TODO: handle
                            }
                        }
                    }
                    sendEmptyMessageDelayed(REPEAT_BROADCAST, mBroadcastInterval);
                } catch (IOException ex) {
//                TODO: handle
                }
            }
        }
    }

    private class BroadcastThread extends HandlerThread {
        private final String mHostName;
        private Handler mHandler;
        private final long mBroadcastInterval;

        BroadcastThread(String hostName, long broadcastInterval) {
            super("Broadcaster");
            mHostName = hostName;
            mBroadcastInterval = broadcastInterval;
        }

        @Override
        protected void onLooperPrepared() {
            mHandler = new BroadcastHandler(getLooper(), mHostName, mBroadcastInterval);
            broadcast();
        }

        void broadcast() {
            mHandler.sendEmptyMessage(BroadcastHandler.REPEAT_BROADCAST);
        }

        void stopBroadcast() {
            if (isAlive()) {
                mHandler.sendEmptyMessage(BroadcastHandler.STOP_BROADCAST);
                quitSafely();
            }
        }
    }
}
