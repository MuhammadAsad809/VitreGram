package com.vitregram.tv.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitregram.tv.domain.model.NetworkResult
import com.vitregram.tv.domain.model.User
import com.vitregram.tv.domain.repository.InstagramRepository
import com.vitregram.tv.ui.components.GlassCard
import com.vitregram.tv.ui.components.LoadingView
import com.vitregram.tv.ui.theme.*

@Composable
fun ProfileScreen(
    repository: InstagramRepository,
    modifier: Modifier = Modifier
) {
    var userState by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val result = repository.getProfile("")) {
            is NetworkResult.Success -> {
                userState = result.data
                isLoading = false
            }
            else -> {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        LoadingView(message = "Loading Profile...")
        return
    }

    val user = userState ?: User(
        id = "0",
        username = "instagram_user",
        fullName = "VitreGram User",
        biography = "Instagram client on Android TV"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Profile Header Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image with Instagram Gradient Ring
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .border(3.dp, InstagramGradient, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profilePictureUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.profilePictureUrl,
                                contentDescription = user.username,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.username,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isVerified) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = InstagramBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (user.fullName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.fullName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (user.biography.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = user.biography,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Row
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            StatItem(count = user.postsCount, label = "posts")
                            StatItem(count = user.followersCount, label = "followers")
                            StatItem(count = user.followingCount, label = "following")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(count: Long, label: String) {
    Column {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
