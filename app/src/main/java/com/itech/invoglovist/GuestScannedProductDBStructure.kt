package com.itech.invoglovist

data class GuestScannedProductDBStructure(
    val product_id: String = "",
    val product_name: String = "",
    var product_quantity: String = "",
    var product_price: String = "",
    var total_price: String = "",
)
