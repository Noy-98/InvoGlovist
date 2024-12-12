package com.itech.invoglovist

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class CashierCameraScanQRCodeDashboard : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var isScanning: Boolean = false
    private var isCameraRunning: Boolean = false // To track the camera state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cashier_camera_scan_qr_code_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Show AlertDialog when the activity is opened
        //showScannerChoiceDialog()

        val back = findViewById<FloatingActionButton>(R.id.go_back_bttn)

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

        back.setOnClickListener {
            val intent = Intent (this, CashierHomeDashboard::class.java)
            startActivity(intent)
        }
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

    private fun validateQRCode(qrCodeValue: String?, imageByteArray: ByteArray) {
        if (qrCodeValue == null) {
            isScanning = false // Reset flag if QR code is invalid
            return
        }

        // Log the QR code value for debugging
        Log.d("QRCodeValidation", "Scanned QR Code: $qrCodeValue")

        // Assuming the QR code contains the invoice_id (e.g., "624835")
        val invoiceRef = database.child("GuestInvoice").child(qrCodeValue)

        invoiceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Loop through all products under the invoice
                    for (productSnapshot in dataSnapshot.children) {
                        val productID = productSnapshot.child("product_id").getValue(String::class.java)
                        val productName = productSnapshot.child("product_name").getValue(String::class.java) ?: "Unknown"
                        val productPrice = productSnapshot.child("product_price").getValue(String::class.java) ?: "0"
                        val productQuantity = productSnapshot.child("product_quantity").getValue(String::class.java) ?: "1"
                        val totalPrice = productSnapshot.child("total_price").getValue(String::class.java) ?: "0"

                        // Assuming the scanned QR code represents the invoice ID
                        Log.d("QRCodeValidation", "Product found: $productName, ID: $productID")

                        // Call save method
                        saveGuestScanProduct(qrCodeValue, productID ?: "", productName, productPrice, productQuantity, totalPrice, imageByteArray)
                    }
                } else {
                    // Invoice not found
                    Toast.makeText(this@CashierCameraScanQRCodeDashboard, "Invoice not found", Toast.LENGTH_SHORT).show()
                    Log.d("QRCodeValidation", "No invoice found for QR code: $qrCodeValue")
                    isScanning = false // Reset flag if invoice not found
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseError", "Error validating QR code", databaseError.toException())
                isScanning = false // Reset flag on failure
            }
        })
    }



    private fun saveGuestScanProduct(invoiceID: String, productID: String, productName: String, productPrice: String, productQuantity: String, totalPrice: String, imageByteArray: ByteArray) {

        // Prepare data to be saved in the GuestScanProductTbl
        val scanProductData = mapOf(
            "invoice_id" to invoiceID,
            "product_id" to productID,
            "product_name" to productName,
            "product_quantity" to productQuantity,
            "product_price" to productPrice,
            "total_price" to totalPrice
        )

        // Save QR code image to Firebase Storage
        val qrCodeImageRef = storage.reference.child("cashier_scan_qr_codes/$invoiceID.png")

        val uploadTask = qrCodeImageRef.putBytes(imageByteArray)
        uploadTask.addOnSuccessListener {
            qrCodeImageRef.downloadUrl.addOnSuccessListener { uri ->
                // Save the access token (image URL) in the database
                val scanProductWithImage = scanProductData + ("product_scan_qr_code_image" to uri.toString())
                database.child("CashierScanInvoiceTbl").child(invoiceID).child(productID).setValue(scanProductWithImage)
                    .addOnSuccessListener {
                        Toast.makeText(this@CashierCameraScanQRCodeDashboard, "Invoice scanned successfully", Toast.LENGTH_SHORT).show()
                        isScanning = false // Reset scanning after saving to Firebase
                    }
                    .addOnFailureListener {
                        Log.e("FirebaseError", "Error saving scan invoice", it)
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

  /*  private fun showScannerChoiceDialog() {
        // Create AlertDialog
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Use Mobile Phone Qr Code Scanner?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                // If user clicks "Yes", stay in this activity
                dialog.dismiss() // or you can leave it empty to proceed
            }
            .setNegativeButton("No") { dialog, id ->
                // If user clicks "No", navigate to CashierDeviceScanBarcodeDashboard
                val intent = Intent(this, CashierDeviceScanBarcodeDashboard::class.java)
                startActivity(intent)
                finish() // Close the current activity
            }

        // Create and show the AlertDialog
        val alert = dialogBuilder.create()
        alert.setTitle("Scanner Choice")
        alert.show()
    } */
}