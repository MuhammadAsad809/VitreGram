package com.vitregram.tv.presentation.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vitregram.tv.player.ReelPlayerOverlay
import com.vitregram.tv.player.VideoPlayerManager
import com.vitregram.tv.ui.components.ErrorView
import com.vitregram.tv.ui.components.LoadingView
import com.vitregram.tv.ui.theme.ObsidianBackground

@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val playerManager = remember { VideoPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
        }
    }

    when (val state = uiState) {
        is ReelsUiState.Loading -> {
            LoadingView(message = "Loading Instagram Reels...")
        }
        is ReelsUiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { viewModel.loadReels() }
            )
        }
        is ReelsUiState.Success -> {
            val reels = state.reels
            if (reels.isEmpty()) {
                ErrorView(
                    message = "No Reels available in your feed.",
                    onRetry = { viewModel.loadReels() }
                )
            } else {
                val currentReel = reels.getOrNull(state.currentIndex) ?: reels.first()
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(ObsidianBackground)
                ) {
                    ReelPlayerOverlay(
                        reel = currentReel,
                        playerManager = playerManager,
                        onLikeClick = { viewModel.toggleLike(currentReel.id) },
                        onNextReel = { viewModel.nextReel() },
                        onPrevReel = { viewModel.prevReel() }
                    )
                }
            }
        }
    }
}
