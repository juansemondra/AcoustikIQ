package com.puj.acoustikiq.adapters

import android.location.Location
import com.google.gson.TypeAdapter
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class LocationAdapter : TypeAdapter<Location>() {

    override fun write(out: JsonWriter, location: Location) {
        out.beginObject()
        out.name("latitude").value(location.latitude)
        out.name("longitude").value(location.longitude)
        out.name("altitude").value(location.altitude)
        out.endObject()
    }

    override fun read(inReader: JsonReader): Location {
        val location = Location("")

        inReader.beginObject()
        while (inReader.hasNext()) {
            when (inReader.nextName()) {
                "latitude" -> location.latitude = inReader.nextDouble()
                "longitude" -> location.longitude = inReader.nextDouble()
                "altitude" -> location.altitude = inReader.nextDouble()
            }
        }
        inReader.endObject()

        return location
    }
}