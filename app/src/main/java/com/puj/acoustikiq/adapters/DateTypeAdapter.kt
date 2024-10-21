package com.puj.acoustikiq.adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateTypeAdapter : TypeAdapter<Date>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())

    override fun write(out: JsonWriter, value: Date?) {
        if (value != null) {
            out.value(dateFormat.format(value))
        } else {
            out.nullValue()
        }
    }

    override fun read(input: JsonReader): Date? {
        return if (input.peek().toString() == "NULL") {
            input.nextNull()
            null
        } else {
            val dateStr = input.nextString()
            dateFormat.parse(dateStr)
        }
    }
}