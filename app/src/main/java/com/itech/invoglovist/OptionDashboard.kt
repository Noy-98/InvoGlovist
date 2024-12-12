package com.itech.invoglovist

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OptionDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_option_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cashierButton = findViewById<TextView>(R.id.login_cashier_bttn)
        val guestButton = findViewById<TextView>(R.id.guest_bttn)

        cashierButton.setOnClickListener {
            val intent = Intent (this, CashierLoginActivity::class.java)
            startActivity(intent)
        }

        guestButton.setOnClickListener {
            val intent = Intent (this, GuestLoginActivity::class.java)
            startActivity(intent)
        }
    }
}