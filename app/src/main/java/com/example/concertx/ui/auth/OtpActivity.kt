package com.example.concertx.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.concertx.R
import com.example.concertx.ui.main.MainActivity
import com.example.concertx.utils.OtpHelper
import com.example.concertx.utils.SessionManager

class OtpActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var etOtpCode: EditText
    private lateinit var btnSubmitOtp: Button
    private lateinit var tvResendCode: TextView
    private lateinit var tvOtpSubtitle: TextView

    private var resendCooldownTimer: CountDownTimer? = null
    private var isCooldownActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        sessionManager = SessionManager(this)
        val pendingUserId = sessionManager.getPendingUserId()
        val pendingPhone = sessionManager.getPendingPhone()

        if (pendingUserId == -1 || pendingPhone.isEmpty()) {
            finish()
            return
        }

        etOtpCode = findViewById(R.id.etOtpCode)
        btnSubmitOtp = findViewById(R.id.btnSubmitOtp)
        tvResendCode = findViewById(R.id.tvResendCode)
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle)

        tvOtpSubtitle.text =
            "Enter the 6-digit OTP code sent via SMS to $pendingPhone (expires in 3 minutes)"

        btnSubmitOtp.setOnClickListener {
            handleSubmitOtp()
        }

        tvResendCode.setOnClickListener {
            handleResendOtp()
        }
    }

    private fun handleSubmitOtp() {
        val enteredOtp = etOtpCode.text.toString().trim()

        // 1. Validate that the OTP EditText must be filled
        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "OTP Code must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        val expectedOtp = sessionManager.getOtpCode()

        // 2. Validate that the code typed matches the OTP Code sent via SMS
        if (enteredOtp != expectedOtp) {
            Toast.makeText(this, "Invalid OTP Code", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validate that the OTP Code has not expired (3 minutes after being sent)
        if (sessionManager.isOtpExpired()) {
            Toast.makeText(
                this,
                "OTP Code has expired (3 minutes limit exceeded). Please resend code.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Start the user's session and redirect to the Home Page
        val userId = sessionManager.getPendingUserId()
        sessionManager.startUserSession(userId)

        Toast.makeText(this, "OTP Verified! Welcome to ConcertX.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun handleResendOtp() {
        if (isCooldownActive) {
            return
        }

        // Resending is limited to a maximum of three attempts per login
        val currentResendCount = sessionManager.getResendCount()
        if (currentResendCount >= SessionManager.MAX_RESEND_ATTEMPTS) {
            Toast.makeText(
                this,
                "Maximum of 3 resend attempts reached for this login.",
                Toast.LENGTH_SHORT
            ).show()
            tvResendCode.isEnabled = false
            return
        }

        val newOtp = OtpHelper.generateSixDigitOtp()
        val phone = sessionManager.getPendingPhone()
        val updatedCount = sessionManager.updateResentOtp(newOtp)

        val smsSent = OtpHelper.sendOtpSms(this, phone, newOtp)
        if (smsSent) {
            Toast.makeText(
                this,
                "New OTP Code sent via SMS ($updatedCount/${SessionManager.MAX_RESEND_ATTEMPTS})",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "New OTP generated: $newOtp ($updatedCount/${SessionManager.MAX_RESEND_ATTEMPTS})",
                Toast.LENGTH_LONG
            ).show()
        }

        startThirtySecondCooldown()
    }

    private fun startThirtySecondCooldown() {
        isCooldownActive = true
        tvResendCode.isEnabled = false

        resendCooldownTimer?.cancel()
        resendCooldownTimer = object : CountDownTimer(30_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000L) + 1
                tvResendCode.text = "Resend Code (${secondsRemaining}s)"
            }

            override fun onFinish() {
                isCooldownActive = false
                if (sessionManager.getResendCount() >= SessionManager.MAX_RESEND_ATTEMPTS) {
                    tvResendCode.text = "Resend Code (Max attempts reached)"
                    tvResendCode.isEnabled = false
                } else {
                    tvResendCode.text = getString(R.string.resend_code)
                    tvResendCode.isEnabled = true
                }
            }
        }.start()
    }

    override fun onDestroy() {
        resendCooldownTimer?.cancel()
        super.onDestroy()
    }
}
