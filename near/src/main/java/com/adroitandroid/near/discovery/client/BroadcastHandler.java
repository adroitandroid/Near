package com.adroitandroid.near.discovery.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class BroadcastHandler extends Handler {
    static final int STOP_BROADCAST = 1235;
    static final int REPEAT_BROADCAST = 2345;
    private final String mHostName;
    private final long mBroadcastInterval;
    private DatagramSocket mSocket;

    BroadcastHandler(Looper looper, String hostName, long broadcastInterval) {
        super(looper);
        mHostName = hostName;
        mBroadcastInterval = broadcastInterval;
        try {
            mSocket = new DatagramSocket();
            mSocket.setBroadcast(true);
        } catch (SocketException ignored) { }
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        if (msg.what == REPEAT_BROADCAST) {
            broadcast();
        } else if (msg.what == STOP_BROADCAST) {
            removeMessages(REPEAT_BROADCAST);
            mSocket.close();
        }
    }

    void broadcast() {
        if (mSocket != null) {
            try {
                byte[] sendData = mHostName.getBytes();
                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            InetAddress.getByName("255.255.255.255"), 8888);
                    mSocket.send(sendPacket);
                } catch (Exception ignored) { }

                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();

                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }

                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                            mSocket.send(sendPacket);
                        } catch (Exception ignored) { }
                    }
                }
                sendEmptyMessageDelayed(REPEAT_BROADCAST, mBroadcastInterval);
            } catch (IOException ignored) { }
        }
    }
}