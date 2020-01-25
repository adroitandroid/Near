package com.adroitandroid.near.model

import android.os.Parcel
import android.os.Parcelable
import java.net.InetAddress
import java.net.UnknownHostException

open class Host : Parcelable {
    private val inetAddress: InetAddress
    val name: String

    private constructor() {
        inetAddress = LOOPBACK_ADDRESS
        name = DUMMY
    }

    constructor(address: InetAddress, name: String) {
        inetAddress = address
        this.name = name
    }

    protected constructor(parcel: Parcel) {
        name = parcel.readString() ?: DUMMY
        inetAddress = getInetAddressFrom(parcel) ?: LOOPBACK_ADDRESS
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeInt(inetAddress.address.size)
        dest.writeByteArray(inetAddress.address)
    }

    private fun getInetAddressFrom(parcel: Parcel): InetAddress? {
        val addr = ByteArray(parcel.readInt())
        parcel.readByteArray(addr)
        return try {
            InetAddress.getByAddress(addr)
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            null
        }
    }

    val hostAddress: String
        get() = inetAddress.hostAddress

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Host) return false
        return inetAddress.hostAddress == other.inetAddress.hostAddress
    }

    override fun hashCode(): Int {
        return inetAddress.hashCode()
    }

    override fun toString(): String {
        return "Host{" +
                "address=" + inetAddress.hostAddress +
                ", name='" + name + '\'' +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<Host> {
        const val DUMMY = "dummy"
        val LOOPBACK_ADDRESS: InetAddress = InetAddress.getLoopbackAddress()

        override fun createFromParcel(parcel: Parcel): Host {
            return Host(parcel)
        }

        override fun newArray(size: Int): Array<Host?> {
            return arrayOfNulls(size)
        }
    }
}