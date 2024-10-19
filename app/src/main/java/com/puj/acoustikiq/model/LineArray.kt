package com.puj.acoustikiq.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

data class LineArray(
    var type: String,
    var system: Speaker,
    var quantity: Int,
    var location: Location,
    var delay: Float,
    var calibratedFreq: Boolean,
    var calibratedPhase: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable(Speaker::class.java.classLoader)!!,
        parcel.readInt(),
        parcel.readParcelable(Location::class.java.classLoader)!!,
        parcel.readFloat(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeParcelable(system, flags)
        parcel.writeInt(quantity)
        parcel.writeParcelable(location, flags)
        parcel.writeFloat(delay)
        parcel.writeByte(if (calibratedFreq) 1 else 0)
        parcel.writeByte(if (calibratedPhase) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LineArray> {
        override fun createFromParcel(parcel: Parcel): LineArray {
            return LineArray(parcel)
        }

        override fun newArray(size: Int): Array<LineArray?> {
            return arrayOfNulls(size)
        }
    }
}

