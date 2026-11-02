package com.vitregram.tv.presentation.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Reel
import com.vitregram.tv.domain.usecase.GetReelsUseCase
import com.vitregram.tv.domain.usecase.LikePostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReelsUiState {
    object Loading : ReelsUiState()
    data class Success(val reels: List<Reel>, val currentIndex: Int = 0) : ReelsUiState()
    data class Error(val message: String) : ReelsUiState()
}

class ReelsViewModel(
    private val getReelsUseCase: GetReelsUseCase,
    private val likePostUseCase: LikePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReelsUiState>(ReelsUiState.Loading)
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        _uiState.value = ReelsUiState.Loading
        viewModelScope.launch {
            when (val result = getReelsUseCase()) {
                is NetworkResult.Success -> {
                    _uiState.value = ReelsUiState.Success(result.data, 0)
                }
                is NetworkResult.Error -> {
                    _uiState.value = ReelsUiState.Error(result.message)
                }
                is NetworkResult.Unsupported -> {
                    _uiState.value = ReelsUiState.Error(result.reason)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun nextReel() {
        val currentState = _uiState.value
        if (currentState is ReelsUiState.Success) {
            if (currentState.currentIndex < currentState.reels.size - 1) {
                _uiState.value = currentState.copy(currentIndex = currentState.currentIndex + 1)
            }
        }
    }

    fun prevReel() {
        val currentState = _uiState.value
        if (currentState is ReelsUiState.Success) {
            if (currentState.currentIndex > 0) {
                _uiState.value = currentState.copy(currentIndex = currentState.currentIndex - 1)
            }
        }
    }

    fun toggleLike(reelId: String) {
        viewModelScope.launch {
            likePostUseCase(reelId)
        }
    }
}
