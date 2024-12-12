package com.itech.invoglovist

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GuestProductDelete {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("GuestScanProductTbl")

    // Function to delete a product from Firebase
    fun deleteProduct(userId: String, productName: String, callback: (Boolean) -> Unit) {
        val productRef = database.child(userId).child(productName)
        productRef.removeValue()
            .addOnSuccessListener {
                callback(true) // Success callback
            }
            .addOnFailureListener {
                callback(false) // Failure callback
            }
    }
}
