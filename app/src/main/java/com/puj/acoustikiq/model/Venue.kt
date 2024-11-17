package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson

data class Venue(
    var id: String = "",
    var name: String = "",
    var venueLineArray: HashMap<String, LineArray> = hashMapOf(),
    var position: Position = Position(),
    var temperature: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        venueLineArray = Gson().fromJson(
            parcel.readString(),
            HashMap::class.java
        ) as HashMap<String, LineArray>,
        position = parcel.readParcelable(Position::class.java.classLoader) ?: Position(),
        temperature = parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(Gson().toJson(venueLineArray))
        parcel.writeParcelable(position, flags)
        parcel.writeDouble(temperature)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Venue> {
        override fun createFromParcel(parcel: Parcel): Venue {
            return Venue(parcel)
        }

        override fun newArray(size: Int): Array<Venue?> {
            return arrayOfNulls(size)
        }
    }
}