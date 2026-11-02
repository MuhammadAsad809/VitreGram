package com.vitregram.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.navigation.Screen
import com.vitregram.tv.ui.theme.*

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

@Composable
fun GlassSidebar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarFocused) 180.dp else 68.dp,
        animationSpec = tween(durationMillis = 250),
        label = "sidebarWidth"
    )

    val navItems = listOf(
        NavItem(Screen.Home, "Home", Icons.Default.Home),
        NavItem(Screen.Search, "Search", Icons.Outlined.Search),
        NavItem(Screen.Reels, "Reels", Icons.Default.PlayCircle),
        NavItem(Screen.Explore, "Explore", Icons.Default.Explore),
        NavItem(Screen.Notifications, "Activity", Icons.Outlined.FavoriteBorder),
        NavItem(Screen.Profile, "Profile", Icons.Default.Person)
    )

    Column(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(ObsidianBackground)
            .border(
                width = 1.dp,
                color = GlassBorder.copy(alpha = 0.2f),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .padding(vertical = 20.dp, horizontal = 10.dp)
            .onFocusChanged { isSidebarFocused = it.hasFocus },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Instagram Camera Logo
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(InstagramGradient)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(ObsidianBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Instagram",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = isSidebarFocused) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "VitreGram",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Navigation Items
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.screen.route
                    SidebarButton(
                        item = item,
                        isSelected = isSelected,
                        isExpanded = isSidebarFocused,
                        onClick = { onNavigate(item.screen) }
                    )
                }
            }
        }

        // Bottom Settings Button
        SidebarButton(
            item = NavItem(Screen.Settings, "Settings", Icons.Default.Settings),
            isSelected = currentRoute == Screen.Settings.route,
            isExpanded = isSidebarFocused,
            onClick = { onNavigate(Screen.Settings) }
        )
    }
}

@Composable
private fun SidebarButton(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isFocused -> Color.White.copy(alpha = 0.15f)
                    isSelected -> Color.White.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isFocused) {
                    Modifier.border(1.5.dp, GlassBorderFocused, RoundedCornerShape(14.dp))
                } else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left gradient indicator bar when selected/focused (matching Image 3 & 5!)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .then(
                    if (isSelected || isFocused) {
                        Modifier.background(InstagramGradient)
                    } else {
                        Modifier.background(Color.Transparent)
                    }
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = when {
                isFocused -> Color.White
                isSelected -> InstagramPink
                else -> TextSecondary
            },
            modifier = Modifier.size(24.dp)
        )

        AnimatedVisibility(visible = isExpanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.title,
                color = if (isFocused || isSelected) Color.White else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
