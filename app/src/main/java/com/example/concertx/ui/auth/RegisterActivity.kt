package com.example.concertx.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etRegUsername: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var etRegEmail: EditText
    private lateinit var etRegPhone: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        etRegUsername = findViewById(R.id.etRegUsername)
        etRegPassword = findViewById(R.id.etRegPassword)
        etRegEmail = findViewById(R.id.etRegEmail)
        etRegPhone = findViewById(R.id.etRegPhone)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            handleRegister()
        }

        tvGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun handleRegister() {
        val username = etRegUsername.text.toString().trim()
        val password = etRegPassword.text.toString()
        val email = etRegEmail.text.toString().trim()
        val phoneNumber = etRegPhone.text.toString().trim()

        // 1. Validate that all fields must be filled
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Validate that username must be unique and not registered by other users
        if (dbHelper.isUsernameTaken(username)) {
            Toast.makeText(
                this,
                "Username is already registered by another user",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 3. Validate that password's length is at least five characters
        if (password.length < 5) {
            Toast.makeText(
                this,
                "Password length must be at least 5 characters",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 4. Validate that the email is in a valid email format (contains '@' and a domain)
        if (!isValidEmail(email)) {
            Toast.makeText(
                this,
                "Invalid email format. Email must contain '@' and a valid domain",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 5. Validate that the phone number contains only digits, between 10-13 characters in length
        if (!phoneNumber.all { it.isDigit() } || phoneNumber.length !in 10..13) {
            Toast.makeText(
                this,
                "Phone number must contain only digits and be between 10-13 characters",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Store in the application's database (UserID auto-increments)
        val registered = dbHelper.registerUser(
            username = username,
            password = password,
            email = email,
            phoneNumber = phoneNumber
        )

        if (registered) {
            Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT)
                .show()
            navigateToLogin()
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val atIndex = email.indexOf('@')
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) return false
        val domainPart = email.substring(atIndex + 1)
        return domainPart.contains('.') && !domainPart.startsWith('.') && !domainPart.endsWith('.')
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
