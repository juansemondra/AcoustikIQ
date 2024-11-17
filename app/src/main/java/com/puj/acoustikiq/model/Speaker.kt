package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class Speaker(
    var model: String = "",
    var type: String = "",
    var maxSPL: Int = 0,
    var minFreq: Int = 0,
    var maxFreq: Int = 0,
    var horDirectivity: Int = 0,
    var verDirectivity: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(model)
        parcel.writeString(type)
        parcel.writeInt(maxSPL)
        parcel.writeInt(minFreq)
        parcel.writeInt(maxFreq)
        parcel.writeInt(horDirectivity)
        parcel.writeInt(verDirectivity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Speaker> {
        override fun createFromParcel(parcel: Parcel): Speaker = Speaker(parcel)
        override fun newArray(size: Int): Array<Speaker?> = arrayOfNulls(size)
    }
}