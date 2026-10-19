package com.example.concertx.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "concertx_session"
        private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"

        // Pending OTP state during Login -> OTP verification
        private const val KEY_PENDING_USER_ID = "pending_user_id"
        private const val KEY_PENDING_PHONE = "pending_phone"
        private const val KEY_OTP_CODE = "otp_code"
        private const val KEY_OTP_SENT_TIME = "otp_sent_time"
        private const val KEY_RESEND_COUNT = "otp_resend_count"

        // 3 minutes expiration in milliseconds
        const val OTP_EXPIRATION_MS = 3 * 60 * 1000L
        // Maximum 3 resend attempts per login
        const val MAX_RESEND_ATTEMPTS = 3
    }

    fun savePendingOtpState(userId: Int, phoneNumber: String, otpCode: String) {
        prefs.edit()
            .putInt(KEY_PENDING_USER_ID, userId)
            .putString(KEY_PENDING_PHONE, phoneNumber)
            .putString(KEY_OTP_CODE, otpCode)
            .putLong(KEY_OTP_SENT_TIME, System.currentTimeMillis())
            .putInt(KEY_RESEND_COUNT, 0)
            .apply()
    }

    fun updateResentOtp(newOtpCode: String): Int {
        val newCount = getResendCount() + 1
        prefs.edit()
            .putString(KEY_OTP_CODE, newOtpCode)
            .putLong(KEY_OTP_SENT_TIME, System.currentTimeMillis())
            .putInt(KEY_RESEND_COUNT, newCount)
            .apply()
        return newCount
    }

    fun getPendingUserId(): Int = prefs.getInt(KEY_PENDING_USER_ID, -1)

    fun getPendingPhone(): String = prefs.getString(KEY_PENDING_PHONE, "") ?: ""

    fun getOtpCode(): String = prefs.getString(KEY_OTP_CODE, "") ?: ""

    fun getOtpSentTime(): Long = prefs.getLong(KEY_OTP_SENT_TIME, 0L)

    fun getResendCount(): Int = prefs.getInt(KEY_RESEND_COUNT, 0)

    fun isOtpExpired(): Boolean {
        val sentTime = getOtpSentTime()
        if (sentTime == 0L) return true
        return (System.currentTimeMillis() - sentTime) > OTP_EXPIRATION_MS
    }

    fun startUserSession(userId: Int) {
        prefs.edit()
            .putInt(KEY_LOGGED_IN_USER_ID, userId)
            .remove(KEY_PENDING_USER_ID)
            .remove(KEY_PENDING_PHONE)
            .remove(KEY_OTP_CODE)
            .remove(KEY_OTP_SENT_TIME)
            .remove(KEY_RESEND_COUNT)
            .apply()
    }

    fun getLoggedInUserId(): Int = prefs.getInt(KEY_LOGGED_IN_USER_ID, -1)

    fun isLoggedIn(): Boolean = getLoggedInUserId() != -1

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
