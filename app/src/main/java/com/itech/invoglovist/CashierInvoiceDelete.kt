package com.itech.invoglovist

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CashierInvoiceDelete {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun deleteInvoice(invoiceId: String) {
        // Delete the invoice from Firebase
        database.child("CashierScanInvoiceTbl").child(invoiceId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Handle success (e.g., show a toast, refresh data)
                    println("Invoice deleted successfully")
                } else {
                    // Handle failure (e.g., show an error message)
                    println("Failed to delete invoice: ${task.exception?.message}")
                }
            }
    }
}
