package com.vitregram.tv.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.repository.InstagramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(val posts: List<Post>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

class ExploreViewModel(
    private val repository: InstagramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadExplore()
    }

    fun loadExplore() {
        _uiState.value = ExploreUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getExplore()) {
                is NetworkResult.Success -> {
                    _uiState.value = ExploreUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = ExploreUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = ExploreUiState.Success(emptyList())
                }
            }
        }
    }
}
