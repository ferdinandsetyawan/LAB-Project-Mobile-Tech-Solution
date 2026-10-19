package com.example.concertx.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.concertx.R
import com.example.concertx.database.DatabaseHelper
import com.example.concertx.model.User
import com.example.concertx.ui.main.MainActivity
import com.example.concertx.utils.OtpHelper
import com.example.concertx.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView

    private var authenticatedUserPendingSms: User? = null

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with OTP generation and delivery after permission prompt completes
        authenticatedUserPendingSms?.let { user ->
            sendOtpAndRedirect(user)
            authenticatedUserPendingSms = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)
        dbHelper = DatabaseHelper(this)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        // 1. Validate that username must be filled
        if (username.isEmpty()) {
            Toast.makeText(this, "Username must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Validate that password must be filled
        if (password.isEmpty()) {
            Toast.makeText(this, "Password must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validate that username and password are registered in the application's database
        val user = dbHelper.authenticateUser(username, password)
        if (user == null) {
            Toast.makeText(
                this,
                "Invalid username or password. Account is not registered.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Check runtime SEND_SMS permission before sending OTP via SMS Manager API
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            authenticatedUserPendingSms = user
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        } else {
            sendOtpAndRedirect(user)
        }
    }

    private fun sendOtpAndRedirect(user: User) {
        val otpCode = OtpHelper.generateSixDigitOtp()
        sessionManager.savePendingOtpState(
            userId = user.userId,
            phoneNumber = user.phoneNumber,
            otpCode = otpCode
        )

        val smsSent = OtpHelper.sendOtpSms(this, user.phoneNumber, otpCode)
        if (smsSent) {
            Toast.makeText(
                this,
                "OTP Code sent via SMS to ${user.phoneNumber}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "SMS permission required or failed. Generated OTP: $otpCode",
                Toast.LENGTH_LONG
            ).show()
        }

        val intent = Intent(this, OtpActivity::class.java)
        startActivity(intent)
    }
}
