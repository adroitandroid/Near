package com.adroitandroid.near.model;

/**
 * Created by pv on 20/06/17.
 */

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host implements Parcelable {

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

    protected Host(@NonNull Parcel in) {
        this.name = in.readString();
        this.inetAddress = getInetAddressFrom(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(inetAddress.getAddress().length);
        dest.writeByteArray(inetAddress.getAddress());
    }

    public static final Creator<Host> CREATOR = new Creator<Host>() {

        @NonNull
        @Contract("_ -> new")
        @Override
        public Host createFromParcel(Parcel in) {
            return new Host(in);
        }


        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public Host[] newArray(int size) {
            return new Host[size];
        }
    };


    @Nullable
    private InetAddress getInetAddressFrom(@NonNull Parcel in) {
        byte[] addr = new byte[in.readInt()];
        in.readByteArray(addr);
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHostAddress() {
        return inetAddress.getHostAddress();
    }

    public String getName() {
        return name;
    }

    @Contract(value = "null -> false", pure = true)
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
