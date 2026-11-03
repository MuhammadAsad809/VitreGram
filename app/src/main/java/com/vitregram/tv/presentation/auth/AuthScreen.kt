package com.vitregram.tv.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.data.remote.oauth.InstagramOAuthHelper
import com.vitregram.tv.domain.model.*
import com.vitregram.tv.ui.components.GlassCard
import com.vitregram.tv.ui.components.LoadingView
import com.vitregram.tv.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQrPairing by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }
    var showOAuthLogin by remember { mutableStateOf(false) }
    var showOAuthSettings by remember { mutableStateOf(false) }
    var showManualCookieInput by remember { mutableStateOf(false) }

    // Custom OAuth Credentials (if user has their own Meta developer app)
    var clientIdInput by remember { mutableStateOf(InstagramOAuthHelper.DEFAULT_CLIENT_ID) }
    var clientSecretInput by remember { mutableStateOf(InstagramOAuthHelper.DEFAULT_CLIENT_SECRET) }
    var redirectUriInput by remember { mutableStateOf(InstagramOAuthHelper.DEFAULT_REDIRECT_URI) }
    var cookieInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthenticated()
        }
    }

    // 1. FULL-SCREEN QR CODE PHONE PAIRING (Instant & 100% Reliable)
    if (showQrPairing) {
        QrPairingScreen(
            onSessionAuthenticated = { session ->
                showQrPairing = false
                viewModel.loginWithDirectSession(session)
            },
            onCancel = {
                showQrPairing = false
            }
        )
        return
    }

    // 2. Full-Screen TV-Optimized WebView Auto-Cookie Extractor
    if (showWebViewLogin) {
        InstagramTvWebViewLogin(
            onCookiesExtracted = { cookies ->
                showWebViewLogin = false
                viewModel.loginWithCookies(cookies)
            },
            onCancel = {
                showWebViewLogin = false
            }
        )
        return
    }

    // 3. Full-Screen TV OAuth 2.0 Screen (Custom Meta developer app)
    if (showOAuthLogin) {
        InstagramTvOAuthLogin(
            clientId = clientIdInput.ifBlank { InstagramOAuthHelper.DEFAULT_CLIENT_ID },
            clientSecret = clientSecretInput.ifBlank { InstagramOAuthHelper.DEFAULT_CLIENT_SECRET },
            redirectUri = redirectUriInput.ifBlank { InstagramOAuthHelper.DEFAULT_REDIRECT_URI },
            onOAuthSuccess = { tokenResponse, cookies ->
                showOAuthLogin = false
                viewModel.loginWithOAuth(tokenResponse.accessToken, tokenResponse.userId, cookies)
            },
            onCancel = {
                showOAuthLogin = false
            }
        )
        return
    }

    if (uiState is AuthUiState.Loading || uiState is AuthUiState.Checking) {
        LoadingView(message = "Verifying Instagram session...")
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: TV Experience Showcase & Architecture
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(InstagramGradient)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(ObsidianBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "VitreGram Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "VitreGram for TV",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Experience your real Instagram feed, vertical reels, and stories natively formatted for your television screen.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TV Feature Highlights
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureBullet(
                    icon = Icons.Default.QrCode2,
                    title = "Instant Phone QR Pairing",
                    description = "Scan with your phone camera. Zero remote typing."
                )
                FeatureBullet(
                    icon = Icons.Default.Tv,
                    title = "Remote-First Navigation",
                    description = "Full D-pad directional control. Zero touch required."
                )
                FeatureBullet(
                    icon = Icons.Default.Security,
                    title = "Encrypted Keystore Storage",
                    description = "AES-256 GCM encryption. No plain passwords stored."
                )
            }
        }

        // Right Column: TV Authentication Options Hub
        GlassCard(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign In to Instagram",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Select your preferred authentication method for TV.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // METHOD 1: SCAN QR CODE WITH PHONE (THE #1 RECOMMENDED METHOD)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = InstagramPink.copy(alpha = 0.22f),
                    focusedBorderColor = InstagramPink,
                    onClick = {
                        showQrPairing = true
                    }
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(InstagramGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Scan QR Code with Phone",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "RECOMMENDED",
                                        color = InstagramYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(InstagramYellow.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Scan with your phone camera. Enter credentials on your phone. No captcha errors, no remote typing!",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Start",
                            tint = if (isFocused) Color.White else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // METHOD 2: TV BROWSER AUTO-CONNECT
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    focusedBorderColor = Color.White,
                    onClick = {
                        showWebViewLogin = true
                    }
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = InstagramOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "TV Web Browser Auto-Connect",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Web login directly on TV with D-pad navigation.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Start",
                            tint = if (isFocused) Color.White else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // METHOD 3: MANUAL COOKIE ENTRY
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White.copy(alpha = 0.03f),
                    focusedBorderColor = Color.White,
                    onClick = {
                        showManualCookieInput = !showManualCookieInput
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Manual Cookie / Advanced",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (showManualCookieInput) "Hide ▲" else "Show ▼",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        AnimatedVisibility(visible = showManualCookieInput) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                TextField(
                                    value = cookieInput,
                                    onValueChange = { cookieInput = it },
                                    placeholder = {
                                        Text(
                                            text = "sessionid=...; ds_user_id=...",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp)),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { viewModel.loginWithCookies(cookieInput) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = InstagramPurple
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Authenticate with Cookie",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // ADVANCED SETTINGS (Custom Meta Developer App OAuth)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showOAuthSettings = !showOAuthSettings }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showOAuthSettings) "Hide Developer Settings" else "Custom Meta Developer App",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                AnimatedVisibility(visible = showOAuthSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Meta / Instagram Developer App Settings",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = clientIdInput,
                            onValueChange = { clientIdInput = it },
                            label = { Text("Client ID", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = clientSecretInput,
                            onValueChange = { clientSecretInput = it },
                            label = { Text("Client Secret", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = redirectUriInput,
                            onValueChange = { redirectUriInput = it },
                            label = { Text("Redirect URI", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showOAuthLogin = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = InstagramPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Launch Custom OAuth", fontSize = 12.sp)
                        }
                    }
                }

                if (uiState is AuthUiState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = ErrorColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InstagramYellow,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
