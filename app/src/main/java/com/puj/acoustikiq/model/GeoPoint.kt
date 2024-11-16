package com.puj.acoustikiq.model

import android.os.Parcel
import android.os.Parcelable

data class GeoPoint(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GeoPoint> {
        override fun createFromParcel(parcel: Parcel): GeoPoint = GeoPoint(parcel)
        override fun newArray(size: Int): Array<GeoPoint?> = arrayOfNulls(size)
    }
}