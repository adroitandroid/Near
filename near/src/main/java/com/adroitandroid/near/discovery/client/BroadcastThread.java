package com.adroitandroid.near.discovery.client;

import android.os.Handler;
import android.os.HandlerThread;

public class BroadcastThread extends HandlerThread {
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