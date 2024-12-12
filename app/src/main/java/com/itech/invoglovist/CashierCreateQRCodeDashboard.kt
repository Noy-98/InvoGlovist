package com.itech.invoglovist

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.util.*

class CashierCreateQRCodeDashboard : AppCompatActivity() {

    private val storageReference = FirebaseStorage.getInstance().reference
    private val databaseReference = FirebaseDatabase.getInstance().reference.child("ProductsTbl")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cashier_create_qr_code_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val back = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        val createButton = findViewById<Button>(R.id.create_bttn)

        // Get references to the text input fields
        val productNameField = findViewById<TextInputEditText>(R.id.product_name)
        val productPriceField = findViewById<TextInputEditText>(R.id.product_price)
        val productStockField = findViewById<TextInputEditText>(R.id.product_stock)
        val productDescriptionField = findViewById<TextInputEditText>(R.id.product_description)

        back.setOnClickListener {
            val intent = Intent (this, CashierHomeDashboard::class.java)
            startActivity(intent)
            finish()  // Navigating back
        }

        createButton.setOnClickListener {
            val productName = productNameField.text.toString()
            val productPrice = productPriceField.text.toString()
            val productStock = productStockField.text.toString()
            val productDescription = productDescriptionField.text.toString()

            if (productName.isNotEmpty() && productPrice.isNotEmpty() && productStock.isNotEmpty() && productDescription.isNotEmpty()) {
                val productId = generateProductId() // Generate 6-character alphanumeric key
                generateQRCode(productId) { qrCodeBitmap ->
                    uploadQRCodeToFirebase(qrCodeBitmap, productId) { qrCodeUrl ->
                        saveProductToFirebase(productId, productName, productPrice, productStock, productDescription, qrCodeUrl)
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to generate a 6-character alphanumeric product ID
    private fun generateProductId(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        val random = Random()
        return "${letters[random.nextInt(letters.size)]}${numbers[random.nextInt(numbers.size)]}${letters[random.nextInt(letters.size)]}${numbers[random.nextInt(numbers.size)]}${letters[random.nextInt(letters.size)]}${numbers[random.nextInt(numbers.size)]}"
    }

    // Function to generate QR code from product ID
    private fun generateQRCode(data: String, callback: (Bitmap) -> Unit) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) -0x1000000 else -0x1)
                }
            }
            callback(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    // Function to upload QR code to Firebase Storage and get the download URL
    private fun uploadQRCodeToFirebase(qrCodeBitmap: Bitmap, productId: String, callback: (String) -> Unit) {
        val qrCodeRef = storageReference.child("products_qr_codes/$productId.png")
        val baos = ByteArrayOutputStream()
        qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = qrCodeRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            qrCodeRef.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get QR code URL", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "QR code upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to save product details to Firebase Realtime Database
    private fun saveProductToFirebase(productId: String, name: String, price: String, stock: String, description: String, qrCodeUrl: String) {
        val product = ProductsDBStructure(
            product_id = productId,
            product_name = name,
            product_price = price,
            product_stock = stock,
            product_description = description,
            product_qr_code_image = qrCodeUrl
        )

        databaseReference.child(productId).setValue(product)
            .addOnSuccessListener {
                val intent = Intent (this, CashierProductListDashboard::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show()
            }
    }
}
