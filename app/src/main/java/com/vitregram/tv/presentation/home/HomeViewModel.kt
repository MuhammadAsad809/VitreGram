package com.vitregram.tv.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.model.Story
import com.vitregram.tv.domain.usecase.GetHomeFeedUseCase
import com.vitregram.tv.domain.usecase.GetStoriesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val stories: List<Story>,
        val forYouPosts: List<Post>,
        val popularPosts: List<Post>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val getStoriesUseCase: GetStoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                withTimeoutOrNull(15_000L) {
                    val storiesDeferred = async {
                        try {
                            getStoriesUseCase()
                        } catch (e: Exception) {
                            Log.w("HomeViewModel", "Stories fetch failed: ${e.message}")
                            NetworkResult.Success(emptyList())
                        }
                    }
                    val feedDeferred = async {
                        try {
                            getHomeFeedUseCase()
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Feed fetch failed: ${e.message}")
                            NetworkResult.Error(e.message ?: "Failed to load feed")
                        }
                    }

                    val storiesResult = storiesDeferred.await()
                    val feedResult = feedDeferred.await()

                    val stories = if (storiesResult is NetworkResult.Success) storiesResult.data else emptyList()

                    when (feedResult) {
                        is NetworkResult.Success -> {
                            val allPosts = feedResult.data
                            val forYou = allPosts.take(10)
                            val popular = if (allPosts.size > 10) allPosts.drop(10) else allPosts
                            _uiState.value = HomeUiState.Success(
                                stories = stories,
                                forYouPosts = forYou,
                                popularPosts = popular
                            )
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = HomeUiState.Error(feedResult.message)
                        }
                        is NetworkResult.Unsupported -> {
                            _uiState.value = HomeUiState.Error(feedResult.reason)
                        }
                        is NetworkResult.Loading -> {}
                    }
                } ?: run {
                    _uiState.value = HomeUiState.Error("Feed request timed out. Please check your internet connection and try again.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading feed", e)
                _uiState.value = HomeUiState.Error(e.message ?: "Unexpected error loading feed")
            }
        }
    }
}
