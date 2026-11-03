package com.vitregram.tv.presentation.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vitregram.tv.ui.theme.*

/**
 * TV-Optimized Instagram WebView that automatically extracts session cookies
 * (sessionid, ds_user_id, csrftoken) when the user logs in using their remote.
 * Completely eliminates the need to manually enter cookies on a television.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramTvWebViewLogin(
    onCookiesExtracted: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var extractionStatus by remember { mutableStateOf("Ready to sign in with Remote") }
    var isExtracted by remember { mutableStateOf(false) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Remote Back Handler: Goes back in web history if possible, else cancels back to options
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCancel()
        }
    }

    // Authentic Tablet User-Agent matching Android platform to avoid bot detection & captcha loops
    val tabletUserAgent = "Mozilla/5.0 (Linux; Android 14; Pixel Tablet Build/UQ1A.240205.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.102 Safari/537.36"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top TV Header Bar with Remote-Navigable Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (D-pad focusable)
                TvHeaderButton(
                    text = "Back",
                    icon = Icons.Default.ArrowBack,
                    onClick = onCancel
                )

                // Live Extraction Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExtracted) SuccessColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (isExtracted) SuccessColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    if (isExtracted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = InstagramYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = extractionStatus,
                        color = if (isExtracted) SuccessColor else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Action Buttons on Right (Clear & Reload)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvHeaderButton(
                        text = "Clear Cache",
                        icon = Icons.Default.DeleteSweep,
                        onClick = {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            webViewInstance?.clearCache(true)
                            webViewInstance?.loadUrl("https://www.instagram.com/accounts/login/")
                        }
                    )
                    TvHeaderButton(
                        text = "Reload",
                        icon = Icons.Default.Refresh,
                        onClick = { webViewInstance?.reload() }
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

                            // TV Remote and D-pad Focusability
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()

                            // Web settings configured for TV screen readability & JS compatibility
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                textZoom = 110
                                userAgentString = tabletUserAgent
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Helper function to detect successful Instagram authentication cookies
                            fun inspectCookies(currentUrl: String?) {
                                if (isExtracted) return
                                val cookies = cookieManager.getCookie("https://www.instagram.com") ?: ""
                                if (cookies.contains("sessionid=")) {
                                    isExtracted = true
                                    extractionStatus = "Session detected! Logging in to TV..."
                                    cookieManager.flush()
                                    postDelayed({
                                        onCookiesExtracted(cookies)
                                    }, 800)
                                }
                            }

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
                                                    box-shadow: 0 0 15px rgba(225, 48, 108, 0.8) !important;
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
                                    // Block non-HTTP schemes (intent://, market://) from breaking the TV experience
                                    return if (url.startsWith("http://") || url.startsWith("https://")) {
                                        inspectCookies(url)
                                        false
                                    } else {
                                        true
                                    }
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
                            .background(ObsidianBackground.copy(alpha = 0.5f)),
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
                    .height(36.dp)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎮 Remote: Use D-pad arrows to select Username/Password. Press [OK] to type. Session cookies are captured automatically.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TvHeaderButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            imageVector = icon,
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
