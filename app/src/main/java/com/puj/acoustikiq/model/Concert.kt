package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class Concert(
    var id: String = "",
    var name: String = "",
    var date: Long = 0L,
    var location: Position = Position(),
    var venues: MutableList<Venue> = mutableListOf()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readParcelable(Position::class.java.classLoader) ?: Position(),
        parcel.createTypedArrayList(Venue.CREATOR) ?: mutableListOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeLong(date)
        parcel.writeParcelable(location, flags)
        parcel.writeTypedList(venues)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Concert> {
        override fun createFromParcel(parcel: Parcel): Concert {
            return Concert(parcel)
        }

        override fun newArray(size: Int): Array<Concert?> {
            return arrayOfNulls(size)
        }
    }
}