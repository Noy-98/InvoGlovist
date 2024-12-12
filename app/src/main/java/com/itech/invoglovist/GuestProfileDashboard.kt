package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GuestProfileDashboard : AppCompatActivity() {

    private lateinit var databaseReference: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_profile_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance()

        progressBar = findViewById(R.id.ProgressBar)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.profile
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

        val editProfileButton = findViewById<AppCompatButton>(R.id.change_profile_bttn)
        editProfileButton.setOnClickListener {
            updateProfile()
        }

        loadUsersProfile()
    }

    private fun updateProfile() {
        val fname = findViewById<TextInputEditText>(R.id.first_name).text.toString().trim()
        val lname = findViewById<TextInputEditText>(R.id.last_name).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.password).text.toString().trim()
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirm_password).text.toString().trim()

        progressBar.visibility = View.VISIBLE

        if (fname.isEmpty() || lname.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required to fill in", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val usersReference = databaseReference.getReference("GuestTbl/$uid")

            // Update password
            if (password.length >= 6) {
                if (password == confirmPassword) {

                    // Update other profile information
                    usersReference.child("first_name").setValue(fname)
                    usersReference.child("last_name").setValue(lname)

                    currentUser.updatePassword(password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            } else {
                                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadUsersProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            val uid = currentUser.uid
            val usersReference = FirebaseDatabase.getInstance().getReference("GuestTbl/$uid")

            usersReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val guest = snapshot.getValue(Guest::class.java)
                        if (guest != null) {

                            findViewById<TextView>(R.id.first_name).text = guest.first_name
                            findViewById<TextView>(R.id.last_name).text = guest.last_name
                            findViewById<TextView>(R.id.email).text = guest.email
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GuestProfileDashboard, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
}