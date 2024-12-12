package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CashierInvoiceDashboard : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    private lateinit var invoiceRecyclerView: RecyclerView
    private lateinit var invoiceAdapter: InvoiceAdapter
    private lateinit var invoices: MutableList<Invoice>
    private lateinit var noPostText: TextView
    private lateinit var mainTotal: TextView
    private val invoiceDelete = CashierInvoiceDelete()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cashier_invoice_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.invoice
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                startActivity(Intent(applicationContext, CashierHomeDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.invoice) {
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.product_list) {
                startActivity(Intent(applicationContext, CashierProductListDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.profile) {
                startActivity(Intent(applicationContext, CashierProfileDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            }  else if (item.itemId == R.id.logout) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, CashierLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@setOnItemSelectedListener true
            }
            false
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        invoiceRecyclerView = findViewById(R.id.invoiceScanList)
        invoices = mutableListOf()
        invoiceAdapter = InvoiceAdapter(invoices) { invoiceId ->
            // Handle delete action here
            invoiceDelete.deleteInvoice(invoiceId) // Call delete method
            fetchInvoices() // Refresh the invoice list after deletion
        }

        invoiceRecyclerView.layoutManager = LinearLayoutManager(this)
        invoiceRecyclerView.adapter = invoiceAdapter

        noPostText = findViewById(R.id.no_post_text)
        mainTotal = findViewById(R.id.main_total)

        // Set up the update button
        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.update).setOnClickListener {
            Log.d("Update Button", "Update button clicked") // Debug log
            updateProductStock() // Call the function when the update button is clicked
        }

        // Fetch data
        fetchInvoices()
    }

    private fun updateProductStock() {
        Log.d("Update Button", "Update button clicked")

        // Fetch the invoice details from the CashierScanInvoiceTbl first
        database.child("CashierScanInvoiceTbl").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (invoiceSnapshot in snapshot.children) {
                    for (productSnapshot in invoiceSnapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java) ?: continue

                        // Get product details
                        val productId = product.product_id
                        val productQuantity = product.product_quantity.toInt() // Assuming product_quantity is in String format

                        // Fetch the current product stock
                        database.child("ProductsTbl").child(productId).child("product_stock").get().addOnCompleteListener { stockTask ->
                            if (stockTask.isSuccessful) {
                                val currentStock = stockTask.result?.getValue(String::class.java)?.toInt() ?: 0

                                // Calculate new stock
                                val newStock = currentStock - productQuantity

                                if (newStock >= 0) {
                                    // Update the stock in the database, keeping the stock as a String
                                    database.child("ProductsTbl").child(productId).child("product_stock").setValue(newStock.toString()).addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            Log.d("Update Stock", "Stock updated successfully for product ID: $productId. New stock: $newStock")

                                            Toast.makeText(this@CashierInvoiceDashboard, "Product Stock updated successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.e("Update Stock", "Failed to update stock for product ID: $productId", updateTask.exception)
                                            Toast.makeText(this@CashierInvoiceDashboard, "Product Stock update failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Log.w("Update Stock", "Insufficient stock for product ID: $productId. Current stock: $currentStock, Quantity: $productQuantity")
                                }
                            } else {
                                Log.e("Fetch Stock", "Failed to fetch stock for product ID: $productId", stockTask.exception)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Fetch Invoices", "Error fetching invoices", error.toException())
            }
        })
    }


    private fun fetchInvoices() {
        database.child("CashierScanInvoiceTbl").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                invoices.clear()
                var totalSum = 0.0 // Variable to hold the total sum of all product prices

                for (invoiceSnapshot in snapshot.children) {
                    val invoiceId = invoiceSnapshot.key ?: continue
                    val productList = mutableListOf<Product>() // List to hold products for the invoice

                    for (productSnapshot in invoiceSnapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null) {
                            productList.add(product) // Add product to the list

                            // Ensure total_price is a Double
                            val price = product.total_price?.toDoubleOrNull() ?: 0.0 // Convert total_price to Double
                            totalSum += price // Add the price to the total sum
                        }
                    }

                    // Create an invoice object with the product list
                    invoices.add(Invoice(
                        invoice_id = invoiceId,
                        products = productList // Assign the product list here
                    ))
                }

                // Sort invoices by invoice_id numerically
                invoices.sortBy { it.invoice_id.toIntOrNull() ?: Int.MAX_VALUE }

                // Update the total sum in the TextView
                mainTotal.text = String.format("%.2f", totalSum) // Format to 2 decimal places

                invoiceAdapter.notifyDataSetChanged() // Notify the adapter of data changes
                noPostText.visibility = if (invoices.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", "Error fetching invoices", error.toException())
            }
        })
    }
}