package com.vitregram.tv.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.*
import com.vitregram.tv.domain.repository.InstagramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Checking : AuthUiState()
    object Unauthenticated : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object Authenticated : AuthUiState()
}

class AuthViewModel(
    private val repository: InstagramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            if (repository.checkActiveSession()) {
                _uiState.value = AuthUiState.Authenticated
            } else {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }

    fun loginWithCookies(cookieString: String) {
        if (cookieString.isBlank()) {
            _uiState.value = AuthUiState.Error("Cookie string cannot be empty")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = repository.loginWithSession(cookieString)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is AuthResult.ChallengeRequired -> {
                    _uiState.value = AuthUiState.Error("Instagram challenge required. Please log in on a phone browser first.")
                }
                is AuthResult.TwoFactorRequired -> {
                    _uiState.value = AuthUiState.Error("Two-Factor Authentication required.")
                }
            }
        }
    }

    fun loginWithOAuth(accessToken: String, userId: String, cookies: Map<String, String> = emptyMap()) {
        if (accessToken.isBlank()) {
            _uiState.value = AuthUiState.Error("Access token cannot be empty")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = repository.loginWithOAuthToken(accessToken, userId, cookies)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = AuthUiState.Error("OAuth authorization failed")
                }
            }
        }
    }

    fun loginWithDirectSession(session: AuthSession) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.loginWithOAuthToken(session.sessionToken, session.userId, session.cookies)
            _uiState.value = AuthUiState.Authenticated
        }
    }
}
