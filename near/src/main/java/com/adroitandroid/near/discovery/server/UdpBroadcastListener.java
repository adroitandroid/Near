package com.adroitandroid.near.discovery.server;

import com.adroitandroid.near.model.Host;

import java.util.Set;

public interface UdpBroadcastListener {
    void onServerSetupFailed(Throwable e);

    void onReceiveFailed();

    void onHostsUpdate(Set<Host> currentHosts);
}
