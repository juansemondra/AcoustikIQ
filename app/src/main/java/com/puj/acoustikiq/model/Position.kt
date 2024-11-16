package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class Position(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var isOnline: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeByte(if (isOnline) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Position> {
        override fun createFromParcel(parcel: Parcel): Position {
            return Position(parcel)
        }

        override fun newArray(size: Int): Array<Position?> {
            return arrayOfNulls(size)
        }
    }
}