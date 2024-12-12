package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GuestDeviceScanQrCodeDashboard : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_device_scan_qr_code_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.home
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                startActivity(Intent(applicationContext, GuestDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.receipt) {
                startActivity(Intent(applicationContext, GuestReceiptListDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.profile) {
                startActivity(Intent(applicationContext, GuestProfileDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            }  else if (item.itemId == R.id.logout) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, GuestLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@setOnItemSelectedListener true
            }
            false
        }

        // Setup submit button for QR code validation
        findViewById<Button>(R.id.submit).setOnClickListener {
            val qrCodeValue = findViewById<TextView>(R.id.qrCodeValue).text.toString()
            if (qrCodeValue.isNotEmpty()) {
                validateQRCode(qrCodeValue)
            } else {
                Toast.makeText(this, "No QR code scanned", Toast.LENGTH_SHORT).show()
            }
        }

        loadQrData()
    }

    private fun validateQRCode(qrCodeValue: String) {
        // Query the product in the database by QR code
        val query = database.child("ProductsTbl").orderByChild("product_id").equalTo(qrCodeValue)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // QR code found, extract product info
                    val product = dataSnapshot.children.first()
                    val productID = product.child("product_id").value.toString()
                    val productName = product.child("product_name").value.toString()
                    val productPrice = product.child("product_price").value.toString()
                    val totalPrice = product.child("product_price").value.toString()

                    saveGuestScanProduct(productID, productName, productPrice, totalPrice)
                } else {
                    // QR code not found
                    Toast.makeText(this@GuestDeviceScanQrCodeDashboard, "Product not available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@GuestDeviceScanQrCodeDashboard, "Error validating QR code", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveGuestScanProduct(
        productID: String,
        productName: String,
        productPrice: String,
        totalPrice: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        // Prepare data to be saved in the GuestScanProductTbl
        val scanProductData = mapOf(
            "uid" to uid,
            "product_id" to productID,
            "product_name" to productName,
            "product_quantity" to "1", // Default quantity
            "product_price" to productPrice,
            "total_price" to totalPrice
        )

        database.child("GuestScanProductTbl").child(uid).child(productName).setValue(scanProductData)
            .addOnSuccessListener {
                Toast.makeText(this, "Product scanned successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving scan product", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadQrData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            val qrReference = FirebaseDatabase.getInstance().getReference("GuestScanProductTbl/Scanner1")

            qrReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val qr = snapshot.getValue(QrCodeDeviceScan::class.java)
                        if (qr != null) {

                            findViewById<TextView>(R.id.status).text = qr.scanning_status
                            findViewById<TextView>(R.id.qrCodeValue).text = qr.qr_code_result
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GuestDeviceScanQrCodeDashboard, "Failed to load Qr value", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
}