package com.adroitandroid.near.connect.server;

import java.net.InetAddress;

public abstract class TcpServerListener {
    public void onStartFailure(Throwable e) {
        onServerStartFailed(e);
    }
    public abstract void onServerStartFailed(Throwable e);
    public abstract void onReceive(byte[] bytes, InetAddress inetAddress);
}
