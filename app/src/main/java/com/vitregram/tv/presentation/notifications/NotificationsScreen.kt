package com.vitregram.tv.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.ui.components.GlassCard
import com.vitregram.tv.ui.theme.InstagramPink
import com.vitregram.tv.ui.theme.ObsidianBackground
import com.vitregram.tv.ui.theme.TextPrimary
import com.vitregram.tv.ui.theme.TextSecondary

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier
) {
    val sampleNotifications = listOf(
        "Welcome to VitreGram Android TV client v1.0.0!",
        "Secure Keystore session encryption active.",
        "D-pad remote navigation ready."
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Activity & Notifications",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(sampleNotifications.size) { index ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Default.Notifications else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = InstagramPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = sampleNotifications[index],
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
