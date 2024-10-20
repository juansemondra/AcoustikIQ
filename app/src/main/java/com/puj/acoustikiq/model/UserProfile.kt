package com.puj.acoustikiq.model

import java.util.Date

data class UserProfile(var name: String, var phone: String) {
    constructor() : this("", "")

    val createdAt = Date().time
}
