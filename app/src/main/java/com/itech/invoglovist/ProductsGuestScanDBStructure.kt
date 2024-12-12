package com.itech.invoglovist

import java.io.Serializable

data class ProductsGuestScanDBStructure(
    val uid: String = "",
    val invoice_id: String = "",
    val product_name: String = "",
    val product_quantity: String = "0",
    val product_price: String = "",
    var product_scan_qr_code_image: String? = null
): Serializable
