package com.itech.invoglovist

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GuestDashboard : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var isScanning: Boolean = false
    private var isCameraRunning: Boolean = false // To track the camera state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.home
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
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

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        showScannerChoiceDialog()

        previewView = findViewById(R.id.previewView)
        val scanFab: FloatingActionButton = findViewById(R.id.scan_qr_code_fab)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                toggleCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        scanFab.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showScannerChoiceDialog() {
        // Create AlertDialog
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Use Mobile Phone Qr Code Scanner?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                // If user clicks "Yes", stay in this activity
                dialog.dismiss() // or you can leave it empty to proceed
            }
            .setNegativeButton("No") { dialog, id ->

                val intent = Intent(this, GuestDeviceScanQrCodeDashboard::class.java)
                startActivity(intent)
                finish() // Close the current activity
            }

        // Create and show the AlertDialog
        val alert = dialogBuilder.create()
        alert.setTitle("Scanner Choice")
        alert.show()
    }

    private fun toggleCamera() {
        if (isCameraRunning) {
            stopCamera() // Stop the camera
        } else {
            startCamera() // Start the camera
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                })
            }

            try {
                cameraProvider.unbindAll()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                isCameraRunning = true // Mark camera as running
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // Unbind all use cases to stop the camera
            isCameraRunning = false // Mark camera as stopped
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanning) { // Check if not already scanning
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val qrCodeValue = barcode.rawValue

                        if (!isScanning && qrCodeValue != null) { // Process only if not scanning
                            isScanning = true // Prevent further scans
                            val byteArray = convertImageToByteArray(mediaImage)
                            validateQRCode(qrCodeValue, byteArray)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("QRScanError", "Error scanning QR code", it)
                    isScanning = false // Reset the flag on failure
                }
                .addOnCompleteListener {
                    imageProxy.close() // Close imageProxy after processing
                }
        } else {
            imageProxy.close()
        }
    }

    private fun convertImageToByteArray(mediaImage: android.media.Image): ByteArray {
        val buffer = mediaImage.planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return byteArray
    }

    private fun validateQRCode(qrCodeValue: String?, imageByteArray: ByteArray) {
        if (qrCodeValue == null) {
            isScanning = false // Reset flag if QR code is invalid
            return
        }

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

                    saveGuestScanProduct(productID, productName, productPrice, totalPrice, imageByteArray)
                } else {
                    // QR code not found
                    Toast.makeText(this@GuestDashboard, "Scan Product is not available", Toast.LENGTH_SHORT).show()
                    isScanning = false // Reset flag if product is not found
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseError", "Error validating QR code", databaseError.toException())
                isScanning = false // Reset flag on failure
            }
        })
    }

    private fun saveGuestScanProduct(productID: String, productName: String, productPrice: String, totalPrice: String, imageByteArray: ByteArray) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Prepare data to be saved in the GuestScanProductTbl
        val scanProductData = mapOf(
            "uid" to uid,
            "product_id" to productID,
            "product_name" to productName,
            "product_quantity" to "1", // Default quantity
            "product_price" to productPrice,
            "total_price" to totalPrice
        )

        // Save QR code image to Firebase Storage
        val qrCodeImageRef = storage.reference.child("guest_scan_qr_codes/$productName.png")

        val uploadTask = qrCodeImageRef.putBytes(imageByteArray)
        uploadTask.addOnSuccessListener {
            qrCodeImageRef.downloadUrl.addOnSuccessListener { uri ->
                // Save the access token (image URL) in the database
                val scanProductWithImage = scanProductData + ("product_scan_qr_code_image" to uri.toString())
                database.child("GuestScanProductTbl").child(uid).child(productName).setValue(scanProductWithImage)
                    .addOnSuccessListener {
                        Toast.makeText(this@GuestDashboard, "Product scanned successfully", Toast.LENGTH_SHORT).show()
                        isScanning = false // Reset scanning after saving to Firebase
                    }
                    .addOnFailureListener {
                        Log.e("FirebaseError", "Error saving scan product", it)
                        isScanning = false // Reset flag on failure
                    }
            }
        }.addOnFailureListener {
            Log.e("StorageError", "Error uploading QR code image", it)
            isScanning = false // Reset flag on failure
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}