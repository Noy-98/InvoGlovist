package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
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

class CashierProductListDashboard : AppCompatActivity() {

    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productList: MutableList<ProductsDBStructure>
    private lateinit var searchBox: EditText
    private lateinit var noPostText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cashier_product_list_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.product_list
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            if (item.itemId == R.id.home) {
                startActivity(Intent(applicationContext, CashierHomeDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.invoice) {
                startActivity(Intent(applicationContext, CashierInvoiceDashboard::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return@setOnItemSelectedListener true
            } else if (item.itemId == R.id.product_list) {
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

        // Initialize RecyclerView and Adapter
        val recyclerView: RecyclerView = findViewById(R.id.productList)
        productList = mutableListOf()
        productsAdapter = ProductsAdapter(this, productList)
        recyclerView.adapter = productsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchBox = findViewById(R.id.search_box)
        noPostText = findViewById(R.id.no_post_text)

        loadProductData()

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(charSequence.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
    }

    private fun loadProductData() {
        databaseReference = FirebaseDatabase.getInstance().getReference("ProductsTbl")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                if (snapshot.exists()) {
                    for (pSnapshot in snapshot.children) {
                        val product = pSnapshot.getValue(ProductsDBStructure::class.java)
                        if (product != null) {
                            productList.add(product)
                        }
                    }
                    productsAdapter.notifyDataSetChanged()
                }
                noPostText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun filterProducts(query: String) {
        val filteredList = ArrayList<ProductsDBStructure>()

        for (products in productList) {
            val search = "${products.product_name?.orEmpty()}".toLowerCase()
            if (search.contains(query.toLowerCase())) {
                filteredList.add(products)
            }
        }

        productsAdapter.filterList(filteredList)
    }
}