package com.adroitandroid.near.discovery.server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import com.adroitandroid.near.model.Host;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

public class UdpServerService extends Service {
    public static final String BUNDLE_COMMAND = "bundle_command";
    public static final String BUNDLE_STALE_TIMEOUT = "bundle_stale_timeout";
    private static final String BUNDLE_IS_HOST_CLIENT = "bundle_host_is_client_too";
    public static final String COMMAND_START_SERVER = "start_server";
    public static final String COMMAND_STOP_SERVER = "stop_server";
    private UdpServerBinder mBinder;
    private ArrayMap<Host, StaleHostHandler> mHostHandlerMap = new ArrayMap<>();
    private Set<String> mCurrentHostIps = Collections.synchronizedSet(new ArraySet<String>());
    private ConnectivityChangeReceiver mConnectivityChangeReceiver;
    private long mStaleTimeout;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (COMMAND_START_SERVER.equals(intent.getStringExtra(BUNDLE_COMMAND))) {
                mStaleTimeout = intent.getLongExtra(BUNDLE_STALE_TIMEOUT, 10000);
                startBroadcastListening(intent.getBooleanExtra(BUNDLE_IS_HOST_CLIENT, false));
            } else if (COMMAND_STOP_SERVER.equals(intent.getStringExtra(BUNDLE_COMMAND))) {
                UdpBroadcastListeningHandler.stopListeningForBroadcasts();
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initCurrentDeviceIps();

        IntentFilter connectivityChangeFilter = new IntentFilter();
        connectivityChangeFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mConnectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(mConnectivityChangeReceiver, connectivityChangeFilter);
    }

    private void initCurrentDeviceIps() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            Set<String> updatedIps = new ArraySet<>();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                try {
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        updatedIps.add(inetAddress.getHostAddress());
                    }
                } catch (SocketException e) { e.printStackTrace(); }
            }
            mCurrentHostIps.retainAll(updatedIps);
            mCurrentHostIps.addAll(updatedIps);
        } catch (SocketException e) { e.printStackTrace(); }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectivityChangeReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new UdpServerBinder();
        }
        return mBinder;
    }

    private void startBroadcastListening(boolean isHostClientToo) {
        UdpBroadcastListeningHandler.startBroadcastListening(mHostHandlerMap, mCurrentHostIps,
                isHostClientToo, mStaleTimeout);
    }

    public class UdpServerBinder extends Binder {

        public void setBroadcastListener(UdpBroadcastListener listener) {
            UdpBroadcastListeningHandler.setListener(listener);
        }

        public void stopBroadcastListening() {
            UdpBroadcastListeningHandler.stopListeningForBroadcasts();
        }
    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            initCurrentDeviceIps();
        }
    }
}
