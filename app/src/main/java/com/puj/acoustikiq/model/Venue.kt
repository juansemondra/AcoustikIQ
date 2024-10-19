package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class Venue(
    var name: String,
    var venueLineArray: MutableList<LineArray>,
    var temperature: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.createTypedArrayList(LineArray.CREATOR)!!,
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeTypedList(venueLineArray)
        parcel.writeDouble(temperature)
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
