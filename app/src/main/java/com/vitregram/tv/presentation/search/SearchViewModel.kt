package com.vitregram.tv.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Reel
import com.vitregram.tv.domain.model.User
import com.vitregram.tv.domain.usecase.SearchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val users: List<User>, val reels: List<Reel>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val searchUseCase: SearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.value = SearchUiState.Loading
            when (val result = searchUseCase(query)) {
                is NetworkResult.Success -> {
                    _uiState.value = SearchUiState.Success(
                        users = result.data.first,
                        reels = result.data.second
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = SearchUiState.Error(result.message)
                }
                is NetworkResult.Unsupported -> {
                    _uiState.value = SearchUiState.Error(result.reason)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
