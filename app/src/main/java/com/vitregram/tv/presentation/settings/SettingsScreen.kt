package com.vitregram.tv.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.domain.repository.InstagramRepository
import com.vitregram.tv.ui.components.GlassCard
import com.vitregram.tv.ui.components.UnsupportedFeatureBanner
import com.vitregram.tv.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: InstagramRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isAutoplayAudio by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Settings & Device Preferences",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Section 1: Playback Preferences
        item {
            Text(
                text = "Media3 Video Playback",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SettingsCard(
                icon = Icons.Default.HighQuality,
                title = "Video Resolution",
                subtitle = "Adaptive up to 4K / 1080p 60 FPS hardware decoded"
            )
        }

        item {
            SettingsCard(
                icon = Icons.Default.VolumeUp,
                title = "Autoplay Sound",
                subtitle = if (isAutoplayAudio) "Enabled (Plays audio on Reel focus)" else "Muted by default",
                onClick = { isAutoplayAudio = !isAutoplayAudio }
            )
        }

        // Section 2: Integration Limitations & Feasibility Notices
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Instagram API Limitations (PRD Section 7)",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            UnsupportedFeatureBanner(
                featureName = "Live Broadcasting & Video Calls",
                reason = "Meta does not permit third-party TV clients to initiate RTMP video calls or live streaming."
            )
        }

        // Section 3: Session & Cache Actions
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Account & Privacy",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = {
                    coroutineScope.launch {
                        repository.logout()
                        onLogout()
                    }
                }
            ) { isFocused ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log Out",
                        tint = ErrorColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Disconnect Instagram Session",
                        color = if (isFocused) Color.White else ErrorColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.White else InstagramBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
