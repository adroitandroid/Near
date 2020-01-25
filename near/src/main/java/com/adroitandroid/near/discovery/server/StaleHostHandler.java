package com.adroitandroid.near.discovery.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import com.adroitandroid.near.model.Host;

public class StaleHostHandler extends Handler {
    static final int STALE_HOST = 9284;
    private final Host mHost;
    private final ArrayMap<Host, StaleHostHandler> mMap;
    private UdpBroadcastListener mListener;

    StaleHostHandler(Host host, ArrayMap<Host, StaleHostHandler> hostHandlerMap,
                     UdpBroadcastListener listener) {
        mHost = host;
        mMap = hostHandlerMap;
        mListener = listener;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        if (msg.what == STALE_HOST) {
            mMap.remove(mHost);
            if (mListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onHostsUpdate(mMap.keySet());
                    }
                });
            }
        }
    }

    void setListener(UdpBroadcastListener listener) {
        this.mListener = listener;
    }
}
