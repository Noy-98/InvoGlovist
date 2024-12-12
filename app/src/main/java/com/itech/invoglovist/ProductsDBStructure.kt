package com.itech.invoglovist

import java.io.Serializable

data class ProductsDBStructure(
    val product_id: String = "",
    val product_name: String = "",
    val product_price: String = "",
    val product_stock: String = "",
    val product_description: String = "",
    var product_qr_code_image: String? = null
): Serializable
