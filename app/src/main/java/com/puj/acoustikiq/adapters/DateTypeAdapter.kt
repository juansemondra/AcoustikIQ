package com.puj.acoustikiq.adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.Date

class DateTypeAdapter : TypeAdapter<Date>() {
    override fun write(out: JsonWriter, value: Date?) {
        value?.let { out.value(it.time) } ?: out.nullValue()
    }

    override fun read(input: JsonReader): Date? {
        return if (input.peek().toString() == "NULL") {
            input.nextNull()
            null
        } else {
            Date(input.nextLong())
        }
    }
}