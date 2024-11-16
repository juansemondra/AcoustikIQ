package com.puj.acoustikiq.model

import java.util.Date

data class UserProfile(
    var name: String,
    var phone: String,
    var photoUrl: String
) {
    constructor() : this("", "", "")

    val createdAt: Long = Date().time
}