package com.itech.invoglovist

data class UsersDBStructure(
    var email: String? = null,
    var first_name: String? = null,
    var last_name: String? = null,
    var user_type: String? = "",
    var password: String? = null,
    var uid: String? = null
)
