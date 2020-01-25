package com.adroitandroid.near.discovery.server;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.collection.ArrayMap;

import com.adroitandroid.near.model.Host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;

public class UdpBroadcastListeningHandler extends Handler {
    private static final int LISTEN = 5678;
    private static UdpBroadcastListeningHandler handler;
    private DatagramSocket mSocket;
    private UdpBroadcastListener mListener;
    private ArrayMap<Host, StaleHostHandler> mHostHandlerMap;
    private Set<String> mCurrentIps;
    private boolean isHostClientToo;
    private long mStaleTimeout;

    UdpBroadcastListeningHandler(Looper looper) {
        super(looper);
    }

    static void startBroadcastListening(final ArrayMap<Host, StaleHostHandler> hostHandlerMap,
                                        final Set<String> currentHostIps,
                                        final boolean isHostClientToo, final long staleTimeout) {
        HandlerThread handlerThread = new HandlerThread("ServerService") {
            @Override
            protected void onLooperPrepared() {
                handler = new UdpBroadcastListeningHandler(getLooper());
                handler.mHostHandlerMap = hostHandlerMap;
                handler.mCurrentIps = currentHostIps;
                handler.isHostClientToo = isHostClientToo;
                handler.mStaleTimeout = staleTimeout;
                handler.sendEmptyMessage(UdpBroadcastListeningHandler.LISTEN);
            }
        };
        handlerThread.start();
    }

    static void setListener(UdpBroadcastListener listener) {
        if (handler != null) {
            handler.updateListenersTo(listener);
        }
    }

    private void updateListenersTo(UdpBroadcastListener listener) {
        mListener = listener;
        synchronized (UdpBroadcastListeningHandler.this) {
            for (StaleHostHandler hostHandler : mHostHandlerMap.values()) {
                hostHandler.setListener(listener);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == LISTEN) {
            try {
                if (mSocket == null) {
                    mSocket = new DatagramSocket(null);
                    InetSocketAddress socketAddress
                            = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 8888);
                    mSocket.setReuseAddress(true);
                    mSocket.setBroadcast(true);
                    mSocket.setSoTimeout(0);
                    mSocket.bind(socketAddress);
                }

                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                DatagramSocket socket = mSocket;
                socket.receive(packet);

                Host host = new Host(packet.getAddress(), new String(packet.getData()).trim());
                if (isHostClientToo || !mCurrentIps.contains(host.getHostAddress())) {
                    StaleHostHandler handler = mHostHandlerMap.get(host);
                    if (handler == null) {
                        handler = new StaleHostHandler(host, mHostHandlerMap, mListener);
                        synchronized (UdpBroadcastListeningHandler.this) {
                            mHostHandlerMap.put(host, handler);
                        }
                        if (mListener != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onHostsUpdate(mHostHandlerMap.keySet());
                                }
                            });
                        }
                    } else if (hostNameChanged(host, mHostHandlerMap)) {
                        if (mListener != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onHostsUpdate(mHostHandlerMap.keySet());
                                }
                            });
                        }
                    }
                    handler.removeMessages(StaleHostHandler.STALE_HOST);
                    handler.sendEmptyMessageDelayed(StaleHostHandler.STALE_HOST, mStaleTimeout);
                }
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (mListener != null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onReceiveFailed();
                        }
                    });
                }
            }
            if (getLooper().getThread().isAlive()) {
                sendEmptyMessage(LISTEN);
            }
        }
    }

    private boolean hostNameChanged(Host updatedHost, ArrayMap<Host, StaleHostHandler> hostHandlerMap) {
        for (Host host : hostHandlerMap.keySet()) {
            if (updatedHost.equals(host) && !updatedHost.getName().equals(host.getName())) {
                hostHandlerMap.put(updatedHost, hostHandlerMap.remove(host));
                return true;
            }
        }
        return false;
    }

    static void stopListeningForBroadcasts() {
        if (handler != null) {
            handler.stop();
            handler = null;
        }
    }

    private void stop() {
        for (StaleHostHandler handler : mHostHandlerMap.values()) {
            handler.removeMessages(StaleHostHandler.STALE_HOST);
        }
        mListener = null;
        mSocket.close();
        mSocket = null;
        removeMessages(UdpBroadcastListeningHandler.LISTEN);
        getLooper().quitSafely();
    }
}
