package com.vitregram.tv.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.model.Story
import com.vitregram.tv.ui.components.*
import com.vitregram.tv.ui.theme.ObsidianBackground
import com.vitregram.tv.ui.theme.TextPrimary
import com.vitregram.tv.ui.theme.TextSecondary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPost: (Post) -> Unit,
    onOpenStory: (Story) -> Unit,
    onRelogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            LoadingView(message = "Loading Instagram Home Feed...")
        }
        is HomeUiState.Error -> {
            val isAuthError = state.message.contains("login", ignoreCase = true) ||
                    state.message.contains("auth", ignoreCase = true) ||
                    state.message.contains("session", ignoreCase = true)

            ErrorView(
                message = if (isAuthError) "Session expired or authentication required. Please sign in again." else state.message,
                onRetry = {
                    if (isAuthError && onRelogin != null) {
                        onRelogin()
                    } else {
                        viewModel.loadFeed()
                    }
                }
            )
        }
        is HomeUiState.Success -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianBackground)
                    .padding(start = 28.dp, top = 20.dp, end = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top Header: Instagram Logo Text (Matching Image 1, 3, 5)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instagram",
                            color = Color.White,
                            fontFamily = FontFamily.Cursive,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stories Row (Matching Image 1 & 3)
                if (state.stories.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Stories",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(state.stories, key = { it.id }) { story ->
                                    StoryAvatar(
                                        story = story,
                                        onClick = { onOpenStory(story) }
                                    )
                                }
                            }
                        }
                    }
                }

                // "For You" Media Shelf (Matching Image 1, 3, 5)
                item {
                    Column {
                        Text(
                            text = "For you",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(state.forYouPosts, key = { it.id }) { post ->
                                MediaCard(
                                    post = post,
                                    onClick = { onOpenPost(post) }
                                )
                            }
                        }
                    }
                }

                // "Popular with Friends" Media Shelf (Matching Image 5)
                if (state.popularPosts.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 32.dp)) {
                            Text(
                                text = "Popular with Friends",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(state.popularPosts, key = { "pop_${it.id}" }) { post ->
                                    MediaCard(
                                        post = post,
                                        onClick = { onOpenPost(post) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty State UI if no posts are returned
                if (state.forYouPosts.isEmpty() && state.popularPosts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your feed is currently empty or loading new content.",
                                color = TextSecondary,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            GlassCard(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = Color.White.copy(alpha = 0.12f),
                                focusedBorderColor = Color.White,
                                onClick = { viewModel.loadFeed() }
                            ) { isFocused ->
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = if (isFocused) Color.White else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Refresh Feed",
                                        color = if (isFocused) Color.White else TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
