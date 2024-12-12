package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.firebase.database.FirebaseDatabase

class SignupActivity2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val signup = findViewById<AppCompatButton>(R.id.signup_bttn)
        val guest_first_name = findViewById<TextInputEditText>(R.id.guest_first_name)
        val guest_last_name = findViewById<TextInputEditText>(R.id.guest_last_name)
        val email = findViewById<TextInputEditText>(R.id.email)
        val password = findViewById<TextInputEditText>(R.id.password)
        val confirm_pass = findViewById<TextInputEditText>(R.id.confirm_password)
        val ProgressBar : ProgressBar = findViewById(R.id.ProgressBar)
        val loginButton = findViewById<TextView>(R.id.login_bttn)

        loginButton.setOnClickListener {
            val intent = Intent (this, GuestLoginActivity::class.java)
            startActivity(intent)
        }

        signup.setOnClickListener {
            val fName = guest_first_name.text.toString()
            val lname = guest_last_name.text.toString()
            val em = email.text.toString()
            val pass = password.text.toString()
            val cpass = confirm_pass.text.toString()
            val user_type = "Guest"

            ProgressBar.visibility = View.VISIBLE

            if (fName.isEmpty() || lname.isEmpty() || em.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
                if (fName.isEmpty()) {
                    guest_first_name.error = "First Name is required"
                }
                if (lname.isEmpty()) {
                    guest_last_name.error = "Last Name is required"
                }
                if (em.isEmpty()) {
                    email.error = "Email is required"
                }
                if (pass.isEmpty()) {
                    password.error = "Password is required"
                }
                if (cpass.isEmpty()) {
                    confirm_pass.error = "Confirm password is required"
                }
                Toast.makeText(this,"All fields are Required!", Toast.LENGTH_SHORT).show()
                ProgressBar.visibility = View.GONE
            } else if (!em.matches(emailPattern.toRegex())){
                ProgressBar.visibility = View.GONE
                email.error="Enter valid email address"
                Toast.makeText(this,"Enter valid email address", Toast.LENGTH_SHORT).show()
            } else if (pass.length < 6){
                ProgressBar.visibility = View.GONE
                password.error="Enter your password more than 6 characters"
                Toast.makeText(this,"Enter your password more than 6 characters", Toast.LENGTH_SHORT).show()
            } else if (pass != cpass){
                ProgressBar.visibility = View.GONE
                confirm_pass.error="Password not match"
                Toast.makeText(this,"Password not match", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(em, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                Toast.makeText(this, "Verification email sent. Please check your email to verify your account.", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        val databaseRef = database.reference.child("GuestTbl").child(auth.currentUser!!.uid)
                        val users: UsersDBStructure = UsersDBStructure(em, fName, lname, user_type, pass, auth.currentUser!!.uid)

                        databaseRef.setValue(users).addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                ProgressBar.visibility = View.GONE
                                val intent = Intent(this, GuestLoginActivity::class.java)
                                startActivity(intent)
                                Toast.makeText(this, "Sign Up Successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                ProgressBar.visibility = View.GONE
                                Log.d("Firebase", "Database write failed: ${dbTask.exception}")
                                Toast.makeText(this, "Sign Up Failed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        ProgressBar.visibility = View.GONE
                        Log.d("Firebase", "User creation failed: ${task.exception}")
                        Toast.makeText(this, "Do you have internet Connection? or Do you have already account?, try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}