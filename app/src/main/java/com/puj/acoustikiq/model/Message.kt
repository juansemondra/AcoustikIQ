package com.puj.acoustikiq.model

data class Message(
    val from: String,
    val fromId: String,
    val content: String,
    val timestamp: Long,
    var img: String = ""
) {
    constructor() : this("", "", "", 0)

    override fun equals(other: Any?): Boolean {
        if (other !is Message) return false
        if (from === other.from) return true
        return false
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + fromId.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + img.hashCode()
        return result
    }
}