package com.example.concertx.utils

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import kotlin.random.Random

object OtpHelper {

    /**
     * Generates a random 6-digit numeric OTP code (000000 - 999999).
     */
    fun generateSixDigitOtp(): String {
        val randomNumber = Random.nextInt(0, 1_000_000)
        return String.format("%06d", randomNumber)
    }

    /**
     * Sends the OTP code to the user's registered phone number using Android SMS Manager API.
     */
    @Suppress("DEPRECATION")
    fun sendOtpSms(context: Context, phoneNumber: String, otpCode: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            } else {
                SmsManager.getDefault()
            }
            val message = "Your ConcertX verification OTP code is: $otpCode (valid for 3 minutes)."
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
