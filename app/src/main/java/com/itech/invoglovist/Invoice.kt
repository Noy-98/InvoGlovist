package com.itech.invoglovist

data class Invoice(
    val invoice_id: String = "",
    val product_id: String = "",
    val product_name: String = "",
    val product_quantity: String = "",
    val product_price: String = "",
    val total_price: String = "",
    val products: List<Product> = emptyList()
)
