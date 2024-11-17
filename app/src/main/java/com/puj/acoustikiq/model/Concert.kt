package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson

data class Concert(
    var id: String = "",
    var name: String = "",
    var date: Long = 0L,
    var location: Position = Position(),
    var venues: HashMap<String, Venue> = hashMapOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        date = parcel.readLong(),
        location = parcel.readParcelable(Position::class.java.classLoader) ?: Position(),
        venues = Gson().fromJson(
            parcel.readString(),
            HashMap::class.java
        ) as HashMap<String, Venue>
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeLong(date)
        parcel.writeParcelable(location, flags)
        parcel.writeString(Gson().toJson(venues))
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