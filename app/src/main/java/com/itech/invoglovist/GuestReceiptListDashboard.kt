package com.itech.invoglovist

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GuestReceiptListDashboard : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: GuestScanProductsAdapter
    private lateinit var productList: MutableList<GuestScannedProductDBStructure>
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var noPostText: TextView
    private lateinit var mainTotalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_receipt_list_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.receipt
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                startActivity(Intent(applicationContext, GuestDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.receipt) {
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

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.productScanList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        productList = mutableListOf()

        // Initialize Adapter with empty list initially
        productAdapter = GuestScanProductsAdapter(this, productList)
        recyclerView.adapter = productAdapter

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("GuestScanProductTbl")

        noPostText = findViewById(R.id.no_post_text)
        mainTotalText = findViewById(R.id.main_total)

        // Load the products
        loadProducts()

        val generateQrButton = findViewById<AppCompatButton>(R.id.generate_qr_code)
        generateQrButton.setOnClickListener {
            generateQRCodeAndShowDialog()
        }
    }

    private fun generateQRCodeAndShowDialog() {
        // Generate a 6-digit random invoice ID
        val invoiceId = (100000..999999).random().toString()

        // Create QR code bitmap
        val qrBitmap = generateQRBitmap(invoiceId)

        // Display the AlertDialog
        showQRCodeInAlertDialog(invoiceId, qrBitmap)

        // Save the invoice data to Firebase Realtime Database
        saveInvoiceToFirebase(invoiceId)
    }

    private fun showQRCodeInAlertDialog(invoiceId: String, qrBitmap: Bitmap?) {
        // Create ImageView to display QR code
        val qrImageView = ImageView(this)
        qrImageView.setImageBitmap(qrBitmap)

        // Create TextView to display the invoice ID
        val invoiceIdTextView = TextView(this)
        invoiceIdTextView.text = "Invoice ID: $invoiceId"
        invoiceIdTextView.setPadding(20, 20, 20, 20)

        // Create a layout to hold the ImageView and TextView
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.addView(invoiceIdTextView)
        layout.addView(qrImageView)

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("QR Code")
            .setView(layout)
            .setPositiveButton("Download") { _, _ ->
                downloadQRImage(invoiceId, qrBitmap)
            }
            .setNegativeButton("Close", null)
            .create()

        dialog.show()
    }

    private fun downloadQRImage(invoiceId: String, qrBitmap: Bitmap?) {
        if (qrBitmap == null) return

        // Define the file name and the collection (directory) in MediaStore
        val fileName = "QR_$invoiceId.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QRCodeImages") // Subfolder under Pictures
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver

        // Save the image in MediaStore, so it is accessible from the user's file manager
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            try {
                // Get output stream for the image
                val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                    Toast.makeText(this, "QR Code saved to Pictures/QRCodeImages", Toast.LENGTH_LONG).show()
                }

                // Mark the file as finished and ready to use
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save QR Code", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to create media file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRBitmap(data: String): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        return try {
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun saveInvoiceToFirebase(invoiceId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val invoiceRef = FirebaseDatabase.getInstance().getReference("GuestInvoice/$invoiceId")
            val scanRef = FirebaseDatabase.getInstance().getReference("GuestScanProductTbl/$userId")

            // Retrieve data from GuestScanProductTbl and copy to GuestInvoice with the correct structure
            scanRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (productSnapshot in snapshot.children) {
                        // Retrieve product_id from productSnapshot
                        val productId = productSnapshot.child("product_id").value.toString()

                        // Check if scan data is available
                        val scanData = productSnapshot.getValue(GuestScannedProductDBStructure::class.java)
                        if (scanData != null) {
                            // Create a reference to "GuestInvoice/$invoiceId/$productId"
                            val productInvoiceRef = invoiceRef.child(productId)

                            // Save the scan data under this path
                            productInvoiceRef.setValue(scanData).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@GuestReceiptListDashboard, "Invoice for product $productId saved successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@GuestReceiptListDashboard, "Failed to save invoice for product $productId", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GuestReceiptListDashboard, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun loadProducts() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear the existing product list
                    productList.clear()

                    var totalSum = 0.0 // Variable to hold the sum of total prices

                    // Loop through each child node under the current user ID
                    for (productSnapshot in snapshot.children) {
                        val product = productSnapshot.getValue(GuestScannedProductDBStructure::class.java)
                        if (product != null) {
                            // Add each product to the product list
                            productList.add(product)

                            // Debug log to check if product.total_price is retrieved correctly
                            val totalPriceString = product.total_price
                            if (!totalPriceString.isNullOrEmpty()) {
                                try {
                                    // Safely parse total_price to Double and accumulate
                                    val price = totalPriceString.toDouble()
                                    totalSum += price
                                } catch (e: NumberFormatException) {
                                    // Log error in case of a parsing issue
                                    e.printStackTrace()
                                }
                            } else {
                                // Handle missing or empty total_price case
                                totalSum += 0.0
                            }
                        }
                    }

                    // Debug log to check totalSum value
                    println("Total Sum: $totalSum")

                    // Update the main total TextView without formatting
                    mainTotalText.text = totalSum.toString()

                    // Notify adapter about the data change
                    productAdapter.notifyDataSetChanged()

                    // Show or hide 'no post' text based on whether the product list is empty
                    noPostText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors here (e.g., log or show a toast)
                }
            })
        }
    }

}