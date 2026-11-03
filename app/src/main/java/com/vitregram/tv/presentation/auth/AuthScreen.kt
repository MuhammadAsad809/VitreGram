package com.vitregram.tv.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.domain.model.*
import com.vitregram.tv.ui.components.LoadingView
import com.vitregram.tv.ui.theme.*

/**
 * VitreGram Authentication Screen.
 * Exclusively provides the TV-Optimized Instagram Cookie Extractor with
 * live Desktop / Mobile View switching (from DOWNLODER-FOR-CLIPPER architecture).
 * All legacy OAuth, QR pairing, and manual inputs have been removed.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthenticated()
        }
    }

    if (uiState is AuthUiState.Loading || uiState is AuthUiState.Checking) {
        LoadingView(message = "Verifying Instagram session...")
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Primary and only authentication system: TV Cookie Extractor with Desktop/Mobile toggle
        InstagramTvWebViewLogin(
            onCookiesExtracted = { cookies ->
                viewModel.loginWithCookies(cookies)
            }
        )

        // Error Banner overlay if login encounters an error
        AnimatedVisibility(
            visible = uiState is AuthUiState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
        ) {
            val errorMessage = (uiState as? AuthUiState.Error)?.message ?: "Authentication error"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ErrorColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = ErrorColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { viewModel.clearError() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
