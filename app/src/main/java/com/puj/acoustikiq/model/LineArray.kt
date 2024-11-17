package com.puj.acoustikiq.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

data class LineArray(
    var id: String? = "",
    var type: String = "",
    var system: Speaker = Speaker(),
    var quantity: Int = 0,
    var location: Position = Position(),
    var delay: Float = 0f,
    var calibratedFreq: Boolean = false,
    var calibratedPhase: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()!!,
        parcel.readParcelable(Speaker::class.java.classLoader)!!,
        parcel.readInt(),
        parcel.readParcelable(Position::class.java.classLoader)!!,
        parcel.readFloat(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(type)
        parcel.writeParcelable(system, flags)
        parcel.writeInt(quantity)
        parcel.writeParcelable(location, flags)
        parcel.writeFloat(delay)
        parcel.writeByte(if (calibratedFreq) 1 else 0)
        parcel.writeByte(if (calibratedPhase) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LineArray> {
        override fun createFromParcel(parcel: Parcel): LineArray = LineArray(parcel)
        override fun newArray(size: Int): Array<LineArray?> = arrayOfNulls(size)
    }
}