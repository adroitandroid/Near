package com.adroitandroid.near.discovery.client;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
