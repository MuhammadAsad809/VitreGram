package com.vitregram.tv.domain.model

/**
 * Generic result wrapper handling Success, Error, and Unsupported states.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val code: Int? = null) : NetworkResult<Nothing>()
    data class Unsupported(val featureName: String, val reason: String) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

/**
 * Authentication result status.
 */
sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class ChallengeRequired(val challengeType: String, val challengeUrl: String?) : AuthResult()
    data class TwoFactorRequired(val twoFactorIdentifier: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Generic action result (likes, saves, follows).
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null
)
