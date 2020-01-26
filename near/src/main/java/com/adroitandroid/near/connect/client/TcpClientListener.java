package com.adroitandroid.near.connect.client;

public interface TcpClientListener {
    void onSendSuccess(long jobId);
    void onSendFailure(long jobId, Throwable e);
}
