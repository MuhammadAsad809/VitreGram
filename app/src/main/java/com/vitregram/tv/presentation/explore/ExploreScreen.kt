package com.vitregram.tv.presentation.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.repository.InstagramRepository
import com.vitregram.tv.ui.components.ErrorView
import com.vitregram.tv.ui.components.LoadingView
import com.vitregram.tv.ui.components.MediaCard
import com.vitregram.tv.ui.theme.ObsidianBackground
import com.vitregram.tv.ui.theme.TextPrimary

@Composable
fun ExploreScreen(
    repository: InstagramRepository,
    onOpenPost: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadExplore() {
        isLoading = true
        errorMessage = null
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launchWhenStarted {
            when (val result = repository.getExplore()) {
                is NetworkResult.Success -> {
                    posts = result.data
                    isLoading = false
                }
                is NetworkResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val result = repository.getExplore()) {
            is NetworkResult.Success -> {
                posts = result.data
                isLoading = false
            }
            is NetworkResult.Error -> {
                errorMessage = result.message
                isLoading = false
            }
            else -> {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        LoadingView(message = "Loading Explore grid...")
        return
    }

    if (errorMessage != null) {
        ErrorView(message = errorMessage ?: "Failed to load explore", onRetry = { loadExplore() })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Explore",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(posts, key = { it.id }) { post ->
                MediaCard(
                    post = post,
                    onClick = { onOpenPost(post) }
                )
            }
        }
    }
}
