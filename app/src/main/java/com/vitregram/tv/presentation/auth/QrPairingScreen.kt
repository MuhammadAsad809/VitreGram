package com.vitregram.tv.presentation.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitregram.tv.data.remote.pairing.TvPairingServer
import com.vitregram.tv.domain.model.AuthSession
import com.vitregram.tv.ui.components.GlassCard
import com.vitregram.tv.ui.components.QrCodeGenerator
import com.vitregram.tv.ui.theme.*

/**
 * TV Screen displaying a local QR Code for instant Mobile Phone Companion Pairing.
 * Completely eliminates remote typing, captchas, and broken OAuth endpoints.
 */
@Composable
fun QrPairingScreen(
    onSessionAuthenticated: (AuthSession) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPaired by remember { mutableStateOf(false) }
    var authenticatedUsername by remember { mutableStateOf("") }

    val pairingServer = remember {
        TvPairingServer(context) { session ->
            authenticatedUsername = session.username
            isPaired = true
            onSessionAuthenticated(session)
        }
    }

    DisposableEffect(Unit) {
        pairingServer.start()
        onDispose {
            pairingServer.stop()
        }
    }

    BackHandler {
        onCancel()
    }

    val pairingUrl = pairingServer.pairingUrl
    val qrBitmap = remember(pairingUrl) {
        QrCodeGenerator.generateQrBitmap(pairingUrl, width = 600, height = 600)
    }

    // Radar pulse animation for waiting status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TvBackButton(
                    text = "Back to Options",
                    onClick = onCancel
                )

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPaired) SuccessColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (isPaired) SuccessColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    if (isPaired) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paired with @$authenticatedUsername! Launching TV...",
                            color = SuccessColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(InstagramYellow)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Waiting for Phone Scan...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Wi-Fi Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Same Wi-Fi Network",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Crisp QR Code Card
                GlassCard(
                    modifier = Modifier
                        .size(310.dp),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = ObsidianCard
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Pairing QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            CircularProgressIndicator(
                                color = InstagramPink,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                // Right Column: Instructions & Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = InstagramPink,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scan QR Code with Phone",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Open your smartphone camera (iPhone or Android) and scan the QR code to pair your Instagram account seamlessly.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 1
                    PairingStepRow(
                        stepNumber = "1",
                        title = "Point Camera at QR Code",
                        detail = "Tap the link that appears on your phone screen."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2
                    PairingStepRow(
                        stepNumber = "2",
                        title = "Enter Credentials on Phone",
                        detail = "Type easily using your phone's touch keyboard. Zero remote typing!"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Web URL fallback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Can't scan? Open phone browser to:",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "http://${pairingServer.localIpAddress}:${pairingServer.port}",
                                color = InstagramYellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TV PIN CODE",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pairingServer.activePinCode,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            // Bottom Remote Hint Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎮 Remote: Press [Back] on your remote to exit • Make sure phone is connected to the same Wi-Fi",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PairingStepRow(
    stepNumber: String,
    title: String,
    detail: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(InstagramGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TvBackButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else GlassBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = text,
            tint = if (isFocused) Color.White else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isFocused) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
