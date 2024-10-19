package com.puj.acoustikiq.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Concert(
    var name: String,
    var date: Date,
    var location: Location,
    var venues: List<Venue>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        Date(parcel.readLong()),
        parcel.readParcelable(Location::class.java.classLoader)!!, // Location es Parcelable
        parcel.createTypedArrayList(Venue.CREATOR)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeLong(date.time)
        parcel.writeParcelable(location, flags)
        parcel.writeTypedList(venues)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Concert> {
        override fun createFromParcel(parcel: Parcel): Concert {
            return Concert(parcel)
        }

        override fun newArray(size: Int): Array<Concert?> {
            return arrayOfNulls(size)
        }
    }
}
