package com.adroitandroid.near.model;

/**
 * Created by pv on 20/06/17.
 */

import java.net.InetAddress;

//TODO: make this parcelable
public class Host {

    private final InetAddress inetAddress;
    private final String name;

    private Host() {
        inetAddress = InetAddress.getLoopbackAddress();
        name = "dummy";
    }

    public Host(InetAddress address, String name) {
        this.inetAddress = address;
        this.name = name;
    }

    public String getHostAddress() {
        return inetAddress.getHostAddress();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Host)) return false;

        Host host = (Host) o;

        return inetAddress.getHostAddress().equals(host.inetAddress.getHostAddress());
    }

    @Override
    public int hashCode() {
        return inetAddress.hashCode();
    }

    @Override
    public String toString() {
        return "Host{" +
                "address=" + inetAddress.getHostAddress() +
                ", name='" + name + '\'' +
                '}';
    }
}
