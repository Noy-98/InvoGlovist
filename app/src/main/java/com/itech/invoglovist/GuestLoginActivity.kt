package com.itech.invoglovist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GuestLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val forgotPasswordButton = findViewById<TextView>(R.id.forgot_password_bttn)
        val signupButton = findViewById<TextView>(R.id.sign_up_bttn)
        val login : AppCompatButton = findViewById(R.id.login_bttn)
        val Email : TextInputEditText = findViewById(R.id.email)
        val Password : TextInputEditText = findViewById(R.id.password)
        val rememberMe : CheckBox = findViewById(R.id.remember_me_checkbox)
        val ProgressBar : ProgressBar = findViewById(R.id.ProgressBar)

        // Load saved email and password
        val savedEmail = sharedPreferences.getString("email", "")
        val savedPassword = sharedPreferences.getString("password", "")
        val isRemembered = sharedPreferences.getBoolean("rememberMe", false)

        Email.setText(savedEmail)
        Password.setText(savedPassword)
        rememberMe.isChecked = isRemembered

        login.setOnClickListener {
            ProgressBar.visibility = View.VISIBLE

            val email = Email.text.toString()
            val pass = Password.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                if (email.isEmpty()) {
                    Email.error = "Email is Required!"
                }
                if (pass.isEmpty()) {
                    Password.error = "Password is Required!"
                }
                Toast.makeText(this, "All fields are Required!", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else if (!email.matches(emailPattern.toRegex())) {
                Email.error = "Please Enter a Valid Email Address!"
                Toast.makeText(this, "Enter valid email address", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else {
                // Authenticate the user
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { loginTask ->
                    if (loginTask.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                // Retrieve user data from the Firebase Realtime Database
                                val userId = user.uid
                                database.child("GuestTbl").child(userId).get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        val userData =
                                            dataSnapshot.getValue(UsersDBStructure::class.java)

                                        if (userData != null && userData.user_type == "Guest") {
                                            // Save email and password if "Remember Me" is checked
                                            if (rememberMe.isChecked) {
                                                editor.putString("email", email)
                                                editor.putString("password", pass)
                                                editor.putBoolean("rememberMe", true)
                                                editor.apply()
                                            } else {
                                                editor.clear()
                                                editor.apply()
                                            }

                                            // Proceed to the dashboard
                                            ProgressBar.visibility = View.GONE
                                            val intent =
                                                Intent(this, GuestDashboard::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Prevent login if user_type is "Guest"
                                            ProgressBar.visibility = View.GONE
                                            Toast.makeText(this, "Cashier users are not allowed to login.", Toast.LENGTH_SHORT).show()
                                            auth.signOut() // Sign out if not allowed
                                        }
                                    }.addOnFailureListener {
                                        ProgressBar.visibility = View.GONE
                                        Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                ProgressBar.visibility = View.GONE
                                Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        ProgressBar.visibility = View.GONE
                        Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        forgotPasswordButton.setOnClickListener {
            val intent = Intent (this, ForgotPasswordActivity2::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            val intent = Intent (this, SignupActivity2::class.java)
            startActivity(intent)
        }
    }
}