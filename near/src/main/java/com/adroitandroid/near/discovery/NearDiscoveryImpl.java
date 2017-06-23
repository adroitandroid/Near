package com.adroitandroid.near.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.util.ArraySet;

import com.adroitandroid.near.model.Host;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by pv on 20/06/17.
 */

class NearDiscoveryImpl implements NearDiscovery {
    private final Listener mListener;
    private final long mPingInterval;
    private final long mDiscoveryTimeout;
    private final long mDiscoverableTimeout;
    private final Looper mListenerLooper;
    private final Context mContext;
    private boolean mDiscoverable;
    private boolean mDiscovering;
    private Disposable mDiscoverableDisposable;
    private Disposable mDiscoveryDisposable;
    private Set<Host> mCurrentPeers;

    NearDiscoveryImpl(long discoverableTimeout,
                      long discoveryTimeout,
                      long discoverablePingInterval,
                      Listener listener, Looper looper,
                      Context context) {
        mDiscoverableTimeout = discoverableTimeout;
        mDiscoveryTimeout = discoveryTimeout;
        mPingInterval = discoverablePingInterval;
        mListener = listener;
        mListenerLooper = looper;
        mContext = context;
        mCurrentPeers = new ArraySet<>();
    }

    @Override
    public void makeDiscoverable(String hostName) {
        if (!mDiscoverable) {
            beDiscoverable(hostName);
            mDiscoverableDisposable
                    = Observable.timer(mDiscoverableTimeout, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            if (mDiscoverable) {
                                stopBeingDiscoverable();
                                if (mListenerLooper != null && mListener != null) {
                                    new Handler(mListenerLooper).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mListener.onDiscoverableTimeout();
                                        }
                                    });
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void makeNonDiscoverable() {
        if (mDiscoverable) {
            stopBeingDiscoverable();
            mDiscoverableDisposable.dispose();
        }
    }

    private void beDiscoverable(String hostName) {
        Intent intent = new Intent(mContext.getApplicationContext(), UdpBroadcastService.class);
        intent.putExtra(UdpBroadcastService.BUNDLE_NAME, hostName);
        intent.putExtra(UdpBroadcastService.BUNDLE_ACTION, UdpBroadcastService.ACTION_START_BROADCAST);
        intent.putExtra(UdpBroadcastService.BUNDLE_INTERVAL, mPingInterval);
        mContext.startService(intent);
        mDiscoverable = true;
    }

    private void stopBeingDiscoverable() {
        Intent intent = new Intent(mContext.getApplicationContext(), UdpBroadcastService.class);
        intent.putExtra(UdpBroadcastService.BUNDLE_ACTION, UdpBroadcastService.ACTION_STOP_BROADCAST);
        mContext.startService(intent);
        mDiscoverable = false;
    }

    @Override
    public void startDiscovery() {
        if (!mDiscovering) {
            Intent intent = new Intent(mContext.getApplicationContext(), UdpServerService.class);
            intent.putExtra(UdpServerService.BUNDLE_COMMAND, UdpServerService.COMMAND_START_SERVER);
            intent.putExtra(UdpServerService.BUNDLE_STALE_TIMEOUT, mPingInterval * 2);
            mContext.startService(intent);
            mDiscovering = true;

            mContext.bindService(intent, mServerConnection, Context.BIND_AUTO_CREATE);

            mDiscoveryDisposable = Observable.timer(mDiscoveryTimeout, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            if (mDiscovering) {
                                stopDiscovery();
                                if (mListener != null && mListenerLooper != null) {
                                    new Handler(mListenerLooper).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mListener.onDiscoveryTimeout();
                                        }
                                    });
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void stopDiscovery() {
        if (mDiscovering) {
            mContext.unbindService(mServerConnection);

            Intent intent = new Intent(mContext.getApplicationContext(), UdpServerService.class);
            intent.putExtra(UdpServerService.BUNDLE_COMMAND, UdpServerService.COMMAND_STOP_SERVER);
            mContext.startService(intent);
            mDiscovering = false;

            mDiscoveryDisposable.dispose();
        }
    }

    private ServiceConnection mServerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UdpServerService.UdpServerBinder binder = (UdpServerService.UdpServerBinder) service;
            binder.setBroadcastListener(new UdpServerService.UdpBroadcastListener() {
                @Override
                public void onServerSetupFailed(final Throwable e) {
                    if (mListener != null && mListenerLooper != null) {
                        new Handler(mListenerLooper).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onDiscoveryFailure(e);
                            }
                        });
                    }
                }

                @Override
                public void onReceiveFailed() {

                }

                @Override
                public void onHostsUpdate(Set<Host> currentHosts) {
                    mCurrentPeers.retainAll(currentHosts);
                    mCurrentPeers.addAll(currentHosts);
                    if (mListener != null && mListenerLooper != null) {
                        new Handler(mListenerLooper).post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onPeersUpdate(mCurrentPeers);
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public Set<Host> getAllAvailablePeers() {
        return mCurrentPeers;
    }

    @Override
    public boolean isDiscoverable() {
        return mDiscoverable;
    }

    @Override
    public boolean isDiscovering() {
        return mDiscovering;
    }
}
