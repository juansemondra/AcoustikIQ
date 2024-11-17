package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class Venue(
    var id: String = "",
    var name: String = "",
    var venueLineArray: MutableList<LineArray> = mutableListOf(),
    var temperature: Double = 0.0,
    var position: Position = Position()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(LineArray.CREATOR)!!,
        parcel.readDouble(),
        parcel.readParcelable(Position::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeTypedList(venueLineArray)
        parcel.writeDouble(temperature)
        parcel.writeParcelable(position, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Venue> {
        override fun createFromParcel(parcel: Parcel): Venue {
            return Venue(parcel)
        }

        override fun newArray(size: Int): Array<Venue?> {
            return arrayOfNulls(size)
        }
    }
}