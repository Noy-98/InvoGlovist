package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CashierProductsViewDetailsDashboard : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productsData: ProductsDBStructure

    private lateinit var currentUsersId: String
    private lateinit var currentPId: String

    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productStock: TextView
    private lateinit var productDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cashier_products_view_details_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        productsData = intent.getSerializableExtra("productsData") as ProductsDBStructure
        currentPId = intent.getStringExtra("product_id") ?: ""

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference

        val currentUser = auth.currentUser
        currentUsersId = currentUser?.uid ?: ""

        productName = findViewById(R.id.product_name)
        productPrice = findViewById(R.id.product_price)
        productStock = findViewById(R.id.product_stock)
        productDescription = findViewById(R.id.product_description)

        val backButton = findViewById<FloatingActionButton>(R.id.go_back_bttn)
        val deleteButton = findViewById<AppCompatButton>(R.id.delete_bttn)

        populateFields()

        deleteButton.setOnClickListener {
            deleteproductsData()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, CashierProductListDashboard::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deleteproductsData() {
        val ProductsRef = FirebaseDatabase.getInstance().getReference("ProductsTbl").child(currentPId)

        // Create deletion tasks
        val DeleteTask = ProductsRef.removeValue()

        // Run both deletion tasks in parallel and wait for them to finish
        Tasks.whenAll(DeleteTask).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // If both deletions were successful, navigate back to the dashboard
                val intent = Intent(this, CashierProductListDashboard::class.java)
                startActivity(intent)
                Toast.makeText(this, "Products deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Handle the failure of either task
                Toast.makeText(this, "Failed to delete Products: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("DeleteProducts", "Error deleting data", task.exception)
            }
        }
    }

    private fun populateFields() {

        val prodictsImageView =
            findViewById<ShapeableImageView>(R.id.product_qr_code_image)
        Glide.with(this@CashierProductsViewDetailsDashboard)
            .load(productsData.product_qr_code_image)
            .into(prodictsImageView)

        productName.setText(productsData.product_name)
        productPrice.setText(productsData.product_price)
        productStock.setText(productsData.product_stock)
        productDescription.setText(productsData.product_description)
    }
}