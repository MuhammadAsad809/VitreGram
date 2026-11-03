package com.vitregram.tv.presentation.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.vitregram.tv.ui.theme.*

/**
 * TV-Optimized Instagram Cookie Extractor with Desktop / Mobile view toggle.
 * Derived from DOWNLODER-FOR-CLIPPER architecture.
 * Automatically captures session cookies (sessionid, ds_user_id, csrftoken)
 * upon successful Instagram authentication using an Android TV remote.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramTvWebViewLogin(
    onCookiesExtracted: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Reference User-Agents from DOWNLODER-FOR-CLIPPER
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isDesktopMode by remember { mutableStateOf(true) }
    var extractionStatus by remember { mutableStateOf("Sign-In Required") }
    var isExtracted by remember { mutableStateOf(false) }
    var capturedCookieString by remember { mutableStateOf("") }
    var isLoadingPage by remember { mutableStateOf(true) }

    // TV Remote Back: navigate browser back or invoke onCancel
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCancel?.invoke()
        }
    }

    // Helper to inspect and validate cookies from Instagram domains
    fun inspectCookies(currentUrl: String?) {
        if (isExtracted) return
        val cookieManager = CookieManager.getInstance()
        val mainCookies = cookieManager.getCookie("https://www.instagram.com") ?: ""
        val apiCookies = cookieManager.getCookie("https://i.instagram.com") ?: ""
        val mergedCookies = when {
            mainCookies.isNotBlank() && apiCookies.isNotBlank() -> "$mainCookies; $apiCookies"
            mainCookies.isNotBlank() -> mainCookies
            else -> apiCookies
        }

        val hasSessionId = mergedCookies.contains("sessionid=")
        val hasUserId = mergedCookies.contains("ds_user_id=")
        val isStillOnLoginPage = currentUrl?.contains("/login") == true || currentUrl?.contains("/accounts/login") == true

        if (hasSessionId) {
            capturedCookieString = mergedCookies
            if (!isStillOnLoginPage || hasUserId) {
                isExtracted = true
                extractionStatus = "Session detected! Entering TV Mode..."
                cookieManager.flush()
                webViewInstance?.postDelayed({
                    onCookiesExtracted(mergedCookies)
                }, 600)
            } else {
                extractionStatus = "Session captured — ready to proceed"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation & Control Header for TV
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            contentDescription = "VitreGram Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "VitreGram Login",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Instagram Cookie Extractor",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Middle: Live Status Badge & Enter Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isExtracted || capturedCookieString.contains("sessionid=")) SuccessColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                if (isExtracted || capturedCookieString.contains("sessionid=")) SuccessColor.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isExtracted || capturedCookieString.contains("sessionid=")) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (isExtracted || capturedCookieString.contains("sessionid=")) SuccessColor else InstagramYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = extractionStatus,
                            color = if (isExtracted || capturedCookieString.contains("sessionid=")) SuccessColor else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Explicit Enter Button if cookies found
                    AnimatedVisibility(
                        visible = capturedCookieString.contains("sessionid="),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TvHeaderButton(
                            text = "Enter VitreGram",
                            icon = Icons.Default.ArrowForward,
                            highlight = true,
                            onClick = {
                                if (capturedCookieString.isNotBlank()) {
                                    onCookiesExtracted(capturedCookieString)
                                }
                            }
                        )
                    }
                }

                // Right: Controls (Desktop/Mobile Toggle, Reload, Clear Session)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Desktop / Mobile View Toggle Button
                    TvHeaderButton(
                        text = if (isDesktopMode) "🖥️ Desktop View" else "📱 Mobile View",
                        icon = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                        highlight = isDesktopMode,
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewInstance?.let { webView ->
                                webView.settings.userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
                                webView.settings.useWideViewPort = isDesktopMode
                                webView.settings.loadWithOverviewMode = isDesktopMode
                                webView.reload()
                            }
                        }
                    )

                    // Reload Page Button
                    TvHeaderButton(
                        text = "Reload",
                        icon = Icons.Default.Refresh,
                        onClick = { webViewInstance?.reload() }
                    )

                    // Clear Session / Reset Button
                    TvHeaderButton(
                        text = "Clear Session",
                        icon = Icons.Default.DeleteSweep,
                        onClick = {
                            val cm = CookieManager.getInstance()
                            cm.removeAllCookies(null)
                            cm.flush()
                            webViewInstance?.clearCache(true)
                            capturedCookieString = ""
                            isExtracted = false
                            extractionStatus = "Session cleared. Sign-In again."
                            webViewInstance?.loadUrl("https://www.instagram.com/accounts/login/")
                        }
                    )
                }
            }

            // Embedded Android TV Compatible WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Remote D-pad focusability
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()

                            // Web settings configured for TV screen readability & JS compatibility
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = isDesktopMode
                                loadWithOverviewMode = isDesktopMode
                                textZoom = if (isDesktopMode) 100 else 115
                                userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoadingPage = true
                                    inspectCookies(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoadingPage = false
                                    inspectCookies(url)

                                    // Inject TV focus indicator CSS & autofocus for couch viewing
                                    view?.evaluateJavascript("""
                                        (function() {
                                            if (document.getElementById('tv-focus-style')) return;
                                            var style = document.createElement('style');
                                            style.id = 'tv-focus-style';
                                            style.innerHTML = `
                                                input:focus, button:focus, [tabindex]:focus, a:focus {
                                                    outline: 3px solid #E1306C !important;
                                                    outline-offset: 3px !important;
                                                    box-shadow: 0 0 16px rgba(225, 48, 108, 0.8) !important;
                                                }
                                            `;
                                            document.head.appendChild(style);

                                            var userInput = document.querySelector('input[name="username"]');
                                            if (userInput && document.activeElement !== userInput) {
                                                userInput.focus();
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    inspectCookies(url)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Block intent://, instagram://, market://, android-app:// from disrupting TV
                                    if (url.startsWith("intent://") || url.startsWith("instagram://") ||
                                        url.startsWith("market://") || url.startsWith("android-app://") ||
                                        (!url.startsWith("http://") && !url.startsWith("https://"))
                                    ) {
                                        return true
                                    }
                                    inspectCookies(url)
                                    return false
                                }
                            }

                            // Load Instagram login directly
                            loadUrl("https://www.instagram.com/accounts/login/")
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Loading overlay while web page is fetching
                if (isLoadingPage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBackground.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = InstagramPink,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Bottom Remote Control Navigation Hint Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color.Black.copy(alpha = 0.9f))
                    .border(1.dp, GlassBorder)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎮 Remote: Use D-pad to navigate. Press [OK] to select and type. Toggle [Desktop/Mobile View] above for layout preference.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Auto-captures: sessionid · ds_user_id · csrftoken",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TvHeaderButton(
    text: String,
    icon: ImageVector,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isFocused -> if (highlight) InstagramPink else Color.White.copy(alpha = 0.25f)
        highlight -> InstagramPink.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    val borderColor = when {
        isFocused -> Color.White
        highlight -> InstagramPink.copy(alpha = 0.6f)
        else -> GlassBorder
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
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
            imageVector = icon,
            contentDescription = text,
            tint = if (isFocused || highlight) Color.White else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isFocused || highlight) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}
