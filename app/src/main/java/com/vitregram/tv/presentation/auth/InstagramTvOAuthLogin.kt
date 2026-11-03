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
import com.vitregram.tv.data.remote.oauth.InstagramOAuthHelper
import com.vitregram.tv.data.remote.oauth.OAuthTokenResponse
import com.vitregram.tv.ui.theme.*
import kotlinx.coroutines.launch

/**
 * TV-Optimized OAuth 2.0 Screen & Dialog adapting xiandanin/instagram-login-android for Android TV.
 *
 * Implements:
 * 1. OAuth URL construction with redirect_uri, client_id, and response_type=code
 * 2. WebViewClient code interception on redirect
 * 3. Token exchange (InstagramRequest.requestAccessToken) via OkHttp POST
 * 4. Custom TV toolbar with Back, Clear Auth/Change Account, and Progress bar
 * 5. Full Android TV remote D-pad navigation compatibility
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramTvOAuthLogin(
    clientId: String = InstagramOAuthHelper.DEFAULT_CLIENT_ID,
    clientSecret: String = InstagramOAuthHelper.DEFAULT_CLIENT_SECRET,
    redirectUri: String = InstagramOAuthHelper.DEFAULT_REDIRECT_URI,
    onOAuthSuccess: (OAuthTokenResponse, Map<String, String>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var authStatus by remember { mutableStateOf("Ready to authorize via OAuth 2.0") }
    var isExchangingToken by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Handle Android TV remote Back button
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onCancel()
        }
    }

    val oauthUrl = remember(clientId, redirectUri) {
        InstagramOAuthHelper.getRedirectOAuthUrl(clientId, redirectUri)
    }

    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Top Toolbar (inspired by InstagramAuthDialog.setupToolbar)
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
                // Back Button
                TvOAuthToolbarButton(
                    text = "Back",
                    icon = Icons.Default.ArrowBack,
                    onClick = onCancel
                )

                // Live OAuth Status Indicator Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSuccess) SuccessColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (isSuccess) SuccessColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isSuccess) SuccessColor else InstagramYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = authStatus,
                        color = if (isSuccess) SuccessColor else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Toolbar Actions: Clear Auth (Change Account) & Reload
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Equivalent to clearAuthCookie() in InstagramAuthDialog
                    TvOAuthToolbarButton(
                        text = "Change Account",
                        icon = Icons.Default.DeleteSweep,
                        onClick = {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.removeAllCookies(null)
                            cookieManager.flush()
                            webViewInstance?.clearCache(true)
                            authStatus = "Cookies cleared. Reloading OAuth..."
                            webViewInstance?.loadUrl(oauthUrl)
                        }
                    )
                    TvOAuthToolbarButton(
                        text = "Reload",
                        icon = Icons.Default.Refresh,
                        onClick = { webViewInstance?.reload() }
                    )
                }
            }

            // Web Loading Progress Bar (equivalent to pb_dialog_instagram)
            if (webProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { webProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = InstagramPink,
                    trackColor = Color.Transparent
                )
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

                            // TV Focusability
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                textZoom = 110
                                userAgentString = desktopUserAgent
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Helper function to extract any cookies
                            fun extractCookies(): Map<String, String> {
                                val cookieStr = cookieManager.getCookie("https://www.instagram.com") ?: ""
                                val map = mutableMapOf<String, String>()
                                cookieStr.split(";").forEach { part ->
                                    val trimmed = part.trim()
                                    val idx = trimmed.indexOf("=")
                                    if (idx > 0) {
                                        map[trimmed.substring(0, idx)] = trimmed.substring(idx + 1)
                                    }
                                }
                                return map
                            }

                            // Intercepts OAuth redirect code (from InstagramAuthDialog.requestAccessTokenByCode)
                            fun handleUrlCheck(url: String?): Boolean {
                                if (isExchangingToken || isSuccess || url.isNullOrBlank()) return false

                                val code = InstagramOAuthHelper.parseCodeFromUrl(url, redirectUri)
                                if (!code.isNullOrBlank()) {
                                    isExchangingToken = true
                                    authStatus = "OAuth code intercepted! Exchanging for token..."

                                    coroutineScope.launch {
                                        val params = InstagramOAuthHelper.buildAccessTokenParams(
                                            clientId = clientId,
                                            clientSecret = clientSecret,
                                            redirectUri = redirectUri,
                                            code = code
                                        )

                                        val result = InstagramOAuthHelper.requestAccessToken(
                                            tokenUrl = InstagramOAuthHelper.URL_ACCESS_TOKEN,
                                            params = params
                                        )

                                        result.onSuccess { tokenResponse ->
                                            isSuccess = true
                                            authStatus = "Access Token granted! Entering VitreGram TV..."
                                            cookieManager.flush()
                                            val cookies = extractCookies()
                                            postDelayed({
                                                onOAuthSuccess(tokenResponse, cookies)
                                            }, 700)
                                        }.onFailure { ex ->
                                            isExchangingToken = false
                                            errorMessage = ex.message ?: "Failed to exchange OAuth token"
                                            authStatus = "Token Exchange Failed"
                                        }
                                    }
                                    return true
                                }
                                return false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    webProgress = newProgress
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    handleUrlCheck(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    handleUrlCheck(url)

                                    // Inject TV focus outline CSS
                                    view?.evaluateJavascript("""
                                        (function() {
                                            if (document.getElementById('tv-oauth-focus')) return;
                                            var style = document.createElement('style');
                                            style.id = 'tv-oauth-focus';
                                            style.innerHTML = `
                                                input:focus, button:focus, [tabindex]:focus, a:focus {
                                                    outline: 3px solid #E1306C !important;
                                                    outline-offset: 3px !important;
                                                    box-shadow: 0 0 15px rgba(225, 48, 108, 0.8) !important;
                                                }
                                            `;
                                            document.head.appendChild(style);
                                        })();
                                    """.trimIndent(), null)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (handleUrlCheck(url)) {
                                        return true
                                    }
                                    return if (url.startsWith("http://") || url.startsWith("https://")) {
                                        false
                                    } else {
                                        true
                                    }
                                }
                            }

                            loadUrl(oauthUrl)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay when exchanging token or loading
                if (isExchangingToken) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBackground.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = InstagramPink,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Exchanging Authorization Code for Access Token...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Error display banner if exchange fails
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorColor.copy(alpha = 0.2f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚠️ $errorMessage",
                        color = ErrorColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Dismiss",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { errorMessage = null }
                            .padding(horizontal = 8.dp)
                    )
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
                    text = "🎮 Remote: Use D-Pad arrows to navigate fields • [OK] to select / type • [Back] to return • OAuth code exchanges automatically",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TvOAuthToolbarButton(
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
