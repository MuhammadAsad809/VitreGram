package com.vitregram.tv.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.model.Story
import com.vitregram.tv.domain.usecase.GetHomeFeedUseCase
import com.vitregram.tv.domain.usecase.GetStoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            val storiesResult = getStoriesUseCase()
            val feedResult = getHomeFeedUseCase()

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
        }
    }
}
