package com.adroitandroid.near.discovery;

import android.content.Context;
import android.os.Looper;

import com.adroitandroid.near.model.Host;

import java.util.Set;

/**
 * Created by pv on 20/06/17.
 */

public interface NearDiscovery {

    void makeDiscoverable(String hostName);

    void makeNonDiscoverable();

    void startDiscovery();

    void stopDiscovery();

    Set<Host> getAllAvailablePeers();

    boolean isDiscoverable();

    boolean isDiscovering();

    class Builder {
        private long mDiscoverableTimeout = 60000;
        private long mDiscoveryTimeout = 60000;
        private long mDiscoverablePingInterval = 5000;
        private Listener mListener;
        private Looper mLooper;
        private Context mContext;

        public Builder setDiscoverableTimeoutMillis(long discoverableTimeout) {
            this.mDiscoverableTimeout = discoverableTimeout;
            return this;
        }

        public Builder setDiscoveryTimeoutMillis(long discoveryTimeout) {
            this.mDiscoveryTimeout = discoveryTimeout;
            return this;
        }

        public Builder setDiscoverablePingIntervalMillis(long discoverablePingInterval) {
            this.mDiscoverablePingInterval = discoverablePingInterval;
            return this;
        }

        public Builder setDiscoveryListener(Listener listener, Looper looper) {
            this.mListener = listener;
            this.mLooper = looper;
            return this;
        }

        public Builder setContext(Context context) {
            this.mContext = context;
            return this;
        }

        public NearDiscovery build() {
            return new NearDiscoveryImpl(mDiscoverableTimeout, mDiscoveryTimeout,
                    mDiscoverablePingInterval, mListener, mLooper, mContext);
        }
    }

    interface Listener {
        void onPeersUpdate(Set<Host> host);

        void onDiscoveryTimeout();

        void onDiscoveryFailure(Throwable e);

        void onDiscoverableTimeout();
    }
}
