package com.vitregram.tv.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.vitregram.tv.domain.model.Reel
import com.vitregram.tv.ui.theme.GlassBorderFocused
import com.vitregram.tv.ui.theme.InstagramPink
import com.vitregram.tv.ui.theme.TextPrimary
import com.vitregram.tv.ui.theme.TextSecondary

@Composable
fun ReelPlayerOverlay(
    reel: Reel,
    playerManager: VideoPlayerManager,
    onLikeClick: () -> Unit,
    onNextReel: () -> Unit,
    onPrevReel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLiked by remember(reel.id) { mutableStateOf(reel.isLiked) }
    var likeCount by remember(reel.id) { mutableStateOf(reel.likeCount) }

    LaunchedEffect(reel.videoUrl) {
        if (reel.videoUrl.isNotEmpty()) {
            playerManager.playUrl(reel.videoUrl)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090D))
    ) {
        // 1. Blurred Backdrop from Thumbnail (Atmospheric Glow)
        AsyncImage(
            model = reel.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // 2. Centered Vertical 9:16 Video Player
        Box(
            modifier = Modifier
                .fillMaxHeight(0.94f)
                .aspectRatio(9f / 16f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Left Info Section (Matching User Image 2)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 64.dp, bottom = 48.dp)
                .width(360.dp)
        ) {
            Text(
                text = "For you",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Author Username & Audio
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = reel.author.profilePictureUrl,
                    contentDescription = reel.author.username,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = reel.author.username,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reel.audioTitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption
            Text(
                text = reel.caption,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }

        // 4. Right Action Rail (Like, Comment, Share, Save)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Like Action
            ReelActionButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(likeCount),
                tint = if (isLiked) InstagramPink else Color.White,
                onClick = {
                    isLiked = !isLiked
                    likeCount += if (isLiked) 1 else -1
                    onLikeClick()
                }
            )

            // Comment Action
            ReelActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = formatCount(reel.commentCount),
                onClick = {}
            )

            // Share Action
            ReelActionButton(
                icon = Icons.Outlined.Send,
                label = formatCount(reel.shareCount),
                onClick = {}
            )

            // Save Action
            ReelActionButton(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Save",
                onClick = {}
            )

            // Next / Prev Remote Navigation Hints
            ReelActionButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "Next",
                onClick = onNextReel
            )
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) GlassBorderFocused else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.White else tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isFocused) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}
