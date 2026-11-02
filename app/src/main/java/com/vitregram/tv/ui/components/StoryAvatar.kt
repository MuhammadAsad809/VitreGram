package com.vitregram.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitregram.tv.domain.model.Story
import com.vitregram.tv.ui.theme.InstagramGradient
import com.vitregram.tv.ui.theme.TextPrimary
import com.vitregram.tv.ui.theme.TextSecondary

@Composable
fun StoryAvatar(
    story: Story,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "storyAvatarScale"
    )

    val ringBrush = if (story.isSeen) {
        Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF4B5563)))
    } else {
        InstagramGradient
    }

    Column(
        modifier = modifier
            .width(84.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .then(
                    if (isFocused) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else Modifier
                )
                .padding(3.dp)
                .border(2.5.dp, ringBrush, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E222D)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = story.author.profilePictureUrl.ifEmpty { story.mediaUrl },
                contentDescription = story.author.username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = story.author.username,
            color = if (isFocused) Color.White else TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
