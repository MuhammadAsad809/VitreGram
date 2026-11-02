package com.vitregram.tv.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vitregram.tv.domain.model.AuthSession

/**
 * Android Keystore-backed encrypted storage for session cookies and tokens.
 * Complies with strict security requirements: zero plain-text passwords stored.
 */
class KeystoreManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "vitregram_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(session: AuthSession) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_SESSION_TOKEN, session.sessionToken)
            .putString(KEY_CSRF_TOKEN, session.csrfToken)
            .putLong(KEY_EXPIRATION, session.expirationTimestamp)
            .apply()

        // Save raw cookie strings securely
        session.cookies.forEach { (name, value) ->
            sharedPreferences.edit().putString("cookie_$name", value).apply()
        }
    }

    fun getSession(): AuthSession? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        val username = sharedPreferences.getString(KEY_USERNAME, "") ?: ""
        val sessionToken = sharedPreferences.getString(KEY_SESSION_TOKEN, "") ?: ""
        val csrfToken = sharedPreferences.getString(KEY_CSRF_TOKEN, null)
        val expiration = sharedPreferences.getLong(KEY_EXPIRATION, Long.MAX_VALUE)

        val cookies = mutableMapOf<String, String>()
        sharedPreferences.all.forEach { (key, value) ->
            if (key.startsWith("cookie_") && value is String) {
                cookies[key.removePrefix("cookie_")] = value
            }
        }

        return AuthSession(
            userId = userId,
            username = username,
            sessionToken = sessionToken,
            csrfToken = csrfToken,
            cookies = cookies,
            expirationTimestamp = expiration
        )
    }

    fun hasValidSession(): Boolean {
        val session = getSession() ?: return false
        return session.sessionToken.isNotEmpty() && System.currentTimeMillis() < session.expirationTimestamp
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_SESSION_TOKEN = "auth_session_token"
        private const val KEY_CSRF_TOKEN = "auth_csrf_token"
        private const val KEY_EXPIRATION = "auth_expiration_timestamp"
    }
}
