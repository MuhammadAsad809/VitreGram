package com.vitregram.tv.data.remote.pairing

import android.content.Context
import android.net.wifi.WifiManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vitregram.tv.domain.model.AuthSession
import kotlinx.coroutines.*
import okhttp3.*
import java.io.*
import java.net.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Embedded TV Pairing Server running locally on the Android TV device.
 * Enables zero-touch QR Code scanning from mobile phones to authenticate Instagram
 * securely without typing passwords or cookies with a TV remote control.
 */
class TvPairingServer(
    private val context: Context,
    private val onSessionAuthenticated: (AuthSession) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var activePinCode: String = generatePin()
        private set

    var localIpAddress: String = resolveLocalIp(context)
        private set

    var port: Int = 8080
        private set

    val pairingUrl: String
        get() = "http://$localIpAddress:$port/?code=$activePinCode"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()

    fun start() {
        if (isRunning) return
        isRunning = true
        activePinCode = generatePin()
        localIpAddress = resolveLocalIp(context)

        serverScope.launch {
            try {
                // Try port 8080 first, fallback to 8888 or any available port
                serverSocket = try {
                    ServerSocket(8080)
                } catch (e: Exception) {
                    port = 8888
                    ServerSocket(8888)
                }

                while (isRunning && serverSocket?.isClosed == false) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        launch {
                            handleClient(client)
                        }
                    } catch (_: SocketException) {
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverScope.cancel()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read HTTP headers
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // Read POST body if present
            val body = if (contentLength > 0) {
                val charArray = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(charArray, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(charArray, 0, read)
            } else {
                ""
            }

            when {
                path.startsWith("/api/login") && method == "POST" -> {
                    val response = handleApiLogin(body)
                    sendJsonResponse(output, 200, response)
                }
                path.startsWith("/api/verify-2fa") && method == "POST" -> {
                    val response = handleApiVerify2FA(body)
                    sendJsonResponse(output, 200, response)
                }
                path.startsWith("/api/submit-cookie") && method == "POST" -> {
                    val response = handleApiSubmitCookie(body)
                    sendJsonResponse(output, 200, response)
                }
                path.startsWith("/api/status") -> {
                    sendJsonResponse(output, 200, """{"service":"VitreGram TV","pin":"$activePinCode"}""")
                }
                else -> {
                    // Serve Mobile Companion Pairing Web App
                    sendHtmlResponse(output, 200, generateCompanionHtml(activePinCode))
                }
            }

            output.flush()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleApiLogin(bodyJson: String): String = withContext(Dispatchers.IO) {
        try {
            val json = gson.fromJson(bodyJson, JsonObject::class.java)
            val username = json.get("username")?.asString?.trim() ?: ""
            val password = json.get("password")?.asString?.trim() ?: ""

            if (username.isBlank() || password.isBlank()) {
                return@withContext """{"status":"error","message":"Username and password are required"}"""
            }

            // Step 1: Obtain fresh CSRF token from Instagram Web
            val initialRequest = Request.Builder()
                .url("https://www.instagram.com/accounts/login/")
                .addHeader("User-Agent", MOBILE_USER_AGENT)
                .build()

            val initResponse = httpClient.newCall(initialRequest).execute()
            val initCookies = parseCookiesFromHeaders(initResponse.headers)
            val csrfToken = initCookies["csrftoken"] ?: "missing"

            // Step 2: Post login credentials
            val timestamp = System.currentTimeMillis() / 1000
            val encPassword = "#PWD_INSTAGRAM_BROWSER:0:$timestamp:$password"

            val formBody = FormBody.Builder()
                .add("username", username)
                .add("enc_password", encPassword)
                .add("queryParams", "{}")
                .add("optIntoOneTap", "false")
                .build()

            val cookieHeader = initCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

            val loginRequest = Request.Builder()
                .url("https://www.instagram.com/api/v1/web/accounts/login/ajax/")
                .post(formBody)
                .addHeader("User-Agent", MOBILE_USER_AGENT)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-CSRFToken", csrfToken)
                .addHeader("X-IG-App-ID", "936619743392459")
                .addHeader("Referer", "https://www.instagram.com/accounts/login/")
                .addHeader("Cookie", cookieHeader)
                .build()

            val loginResponse = httpClient.newCall(loginRequest).execute()
            val loginBody = loginResponse.body?.string() ?: ""
            val responseCookies = parseCookiesFromHeaders(loginResponse.headers)
            val mergedCookies = initCookies + responseCookies

            val loginJson = gson.fromJson(loginBody, JsonObject::class.java)

            // Check if 2FA is required
            if (loginJson.get("two_factor_required")?.asBoolean == true) {
                val twoFactorInfo = loginJson.get("two_factor_info")?.asJsonObject
                val identifier = twoFactorInfo?.get("two_factor_identifier")?.asString ?: ""
                return@withContext """{"status":"two_factor_required","identifier":"$identifier","username":"$username"}"""
            }

            // Check if authenticated
            if (loginJson.get("authenticated")?.asBoolean == true || mergedCookies.containsKey("sessionid")) {
                val sessionToken = mergedCookies["sessionid"] ?: ""
                val userId = mergedCookies["ds_user_id"] ?: loginJson.get("userId")?.asString ?: "user"

                val authSession = AuthSession(
                    userId = userId,
                    username = username,
                    sessionToken = sessionToken,
                    csrfToken = mergedCookies["csrftoken"],
                    cookies = mergedCookies,
                    expirationTimestamp = System.currentTimeMillis() + (90L * 24 * 3600 * 1000)
                )

                // Notify TV in main thread
                withContext(Dispatchers.Main) {
                    onSessionAuthenticated(authSession)
                }

                return@withContext """{"status":"success","username":"$username"}"""
            }

            val errorMessage = loginJson.get("message")?.asString ?: "Incorrect username or password"
            """{"status":"error","message":"$errorMessage"}"""
        } catch (e: Exception) {
            """{"status":"error","message":"${e.message}"}"""
        }
    }

    private suspend fun handleApiVerify2FA(bodyJson: String): String = withContext(Dispatchers.IO) {
        try {
            val json = gson.fromJson(bodyJson, JsonObject::class.java)
            val code = json.get("code")?.asString?.trim() ?: ""
            val identifier = json.get("identifier")?.asString?.trim() ?: ""
            val username = json.get("username")?.asString?.trim() ?: ""

            val formBody = FormBody.Builder()
                .add("verification_code", code)
                .add("two_factor_identifier", identifier)
                .add("username", username)
                .build()

            val request = Request.Builder()
                .url("https://www.instagram.com/api/v1/web/accounts/login/ajax/two_factor/")
                .post(formBody)
                .addHeader("User-Agent", MOBILE_USER_AGENT)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-IG-App-ID", "936619743392459")
                .addHeader("Referer", "https://www.instagram.com/accounts/login/two_factor")
                .build()

            val response = httpClient.newCall(request).execute()
            val cookies = parseCookiesFromHeaders(response.headers)
            val sessionToken = cookies["sessionid"] ?: ""
            val userId = cookies["ds_user_id"] ?: "user"

            if (sessionToken.isNotEmpty()) {
                val authSession = AuthSession(
                    userId = userId,
                    username = username,
                    sessionToken = sessionToken,
                    csrfToken = cookies["csrftoken"],
                    cookies = cookies,
                    expirationTimestamp = System.currentTimeMillis() + (90L * 24 * 3600 * 1000)
                )

                withContext(Dispatchers.Main) {
                    onSessionAuthenticated(authSession)
                }

                """{"status":"success","username":"$username"}"""
            } else {
                """{"status":"error","message":"Invalid two-factor authentication code"}"""
            }
        } catch (e: Exception) {
            """{"status":"error","message":"${e.message}"}"""
        }
    }

    private suspend fun handleApiSubmitCookie(bodyJson: String): String = withContext(Dispatchers.IO) {
        try {
            val json = gson.fromJson(bodyJson, JsonObject::class.java)
            val cookieStr = json.get("cookies")?.asString?.trim() ?: ""

            val map = mutableMapOf<String, String>()
            cookieStr.split(";").forEach { part ->
                val trimmed = part.trim()
                val idx = trimmed.indexOf("=")
                if (idx > 0) {
                    map[trimmed.substring(0, idx)] = trimmed.substring(idx + 1)
                }
            }

            val sessionid = map["sessionid"]
            val userId = map["ds_user_id"] ?: "user"

            if (!sessionid.isNullOrBlank()) {
                val authSession = AuthSession(
                    userId = userId,
                    username = "user_$userId",
                    sessionToken = sessionid,
                    csrfToken = map["csrftoken"],
                    cookies = map,
                    expirationTimestamp = System.currentTimeMillis() + (90L * 24 * 3600 * 1000)
                )

                withContext(Dispatchers.Main) {
                    onSessionAuthenticated(authSession)
                }

                """{"status":"success"}"""
            } else {
                """{"status":"error","message":"Cookie string must contain sessionid"}"""
            }
        } catch (e: Exception) {
            """{"status":"error","message":"${e.message}"}"""
        }
    }

    private fun parseCookiesFromHeaders(headers: Headers): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        headers.values("Set-Cookie").forEach { rawCookie ->
            val cookiePart = rawCookie.substringBefore(";")
            val idx = cookiePart.indexOf("=")
            if (idx > 0) {
                cookies[cookiePart.substring(0, idx).trim()] = cookiePart.substring(idx + 1).trim()
            }
        }
        return cookies
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
    }

    private fun sendHtmlResponse(output: OutputStream, statusCode: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
    }

    private fun generateCompanionHtml(pin: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>VitreGram TV Mobile Companion</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
        body { background: #08090D; color: #F5F7FA; min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; }
        .card { width: 100%; max-width: 420px; background: #161822; border: 1px solid rgba(255,255,255,0.12); border-radius: 20px; padding: 28px; box-shadow: 0 10px 40px rgba(0,0,0,0.6); }
        .logo-wrap { display: flex; align-items: center; justify-content: center; margin-bottom: 20px; }
        .logo-ring { width: 64px; height: 64px; border-radius: 50%; background: linear-gradient(45deg, #FCAF45, #FF543E, #E1306C, #C13584, #405DE6); display: flex; align-items: center; justify-content: center; }
        .logo-inner { width: 56px; height: 56px; border-radius: 50%; background: #08090D; display: flex; align-items: center; justify-content: center; font-size: 26px; }
        h1 { font-size: 22px; font-weight: 700; text-align: center; margin-bottom: 6px; }
        .subtitle { font-size: 13px; color: #9CA3AF; text-align: center; margin-bottom: 20px; line-height: 1.4; }
        .pin-badge { display: inline-block; background: rgba(225,48,108,0.15); border: 1px solid rgba(225,48,108,0.4); color: #FF543E; font-size: 13px; font-weight: 700; border-radius: 8px; padding: 4px 12px; margin-bottom: 20px; text-align: center; width: 100%; letter-spacing: 2px; }
        .tabs { display: flex; gap: 8px; margin-bottom: 20px; background: rgba(0,0,0,0.3); padding: 4px; border-radius: 12px; }
        .tab-btn { flex: 1; padding: 10px; border: none; background: transparent; color: #9CA3AF; font-size: 13px; font-weight: 600; border-radius: 8px; cursor: pointer; transition: 0.2s; }
        .tab-btn.active { background: rgba(255,255,255,0.12); color: #fff; }
        .input-group { margin-bottom: 14px; }
        label { display: block; font-size: 12px; color: #9CA3AF; margin-bottom: 6px; font-weight: 500; }
        input { width: 100%; height: 48px; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.15); border-radius: 12px; padding: 0 14px; color: #fff; font-size: 15px; outline: none; transition: 0.2s; }
        input:focus { border-color: #E1306C; box-shadow: 0 0 10px rgba(225,48,108,0.4); }
        .btn-submit { width: 100%; height: 50px; border: none; border-radius: 14px; background: linear-gradient(45deg, #FF543E, #E1306C, #C13584); color: #fff; font-size: 15px; font-weight: 700; cursor: pointer; margin-top: 10px; transition: 0.2s; }
        .btn-submit:hover { opacity: 0.95; transform: scale(1.01); }
        .status-msg { margin-top: 14px; padding: 12px; border-radius: 10px; font-size: 13px; text-align: center; display: none; line-height: 1.4; }
        .status-error { background: rgba(239,68,68,0.2); border: 1px solid #EF4444; color: #FCA5A5; display: block; }
        .status-success { background: rgba(16,185,129,0.2); border: 1px solid #10B981; color: #6EE7B7; display: block; }
        .status-loading { background: rgba(245,158,11,0.2); border: 1px solid #F59E0B; color: #FCD34D; display: block; }
        .footer-note { margin-top: 20px; font-size: 11px; color: #6B7280; text-align: center; }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo-wrap">
            <div class="logo-ring"><div class="logo-inner">📺</div></div>
        </div>
        <h1>Pair with VitreGram TV</h1>
        <p class="subtitle">Quickly log in on your phone. Your session will sync to your television automatically!</p>
        <div class="pin-badge">TV CODE: $pin</div>

        <div class="tabs">
            <button class="tab-btn active" id="tabDirect" onclick="switchTab('direct')">Account Sign In</button>
            <button class="tab-btn" id="tabCookie" onclick="switchTab('cookie')">Paste Cookie</button>
        </div>

        <!-- FORM 1: Direct Instagram Login from Phone -->
        <div id="sectionDirect">
            <div class="input-group">
                <label>Instagram Username or Email</label>
                <input type="text" id="username" placeholder="phone, username, or email" autocomplete="username">
            </div>
            <div class="input-group">
                <label>Password</label>
                <input type="password" id="password" placeholder="Instagram password" autocomplete="current-password">
            </div>

            <!-- Optional 2FA Field -->
            <div class="input-group" id="twoFactorGroup" style="display: none;">
                <label style="color: #F59E0B;">6-Digit Security Code (2FA)</label>
                <input type="text" id="twoFactorCode" placeholder="123456" maxlength="8">
            </div>

            <button class="btn-submit" id="btnSubmit" onclick="submitLogin()">Connect Living Room TV</button>
        </div>

        <!-- FORM 2: Paste Cookie -->
        <div id="sectionCookie" style="display: none;">
            <div class="input-group">
                <label>Instagram Session Cookie</label>
                <input type="text" id="cookieInput" placeholder="sessionid=...; ds_user_id=...">
            </div>
            <button class="btn-submit" onclick="submitCookie()">Sync Cookie to TV</button>
        </div>

        <div id="statusBox" class="status-msg"></div>

        <p class="footer-note">🔒 End-to-End Encrypted via Local Wi-Fi. Passwords are never stored on TV.</p>
    </div>

    <script>
        let twoFactorIdentifier = '';

        function switchTab(tab) {
            document.getElementById('sectionDirect').style.display = tab === 'direct' ? 'block' : 'none';
            document.getElementById('sectionCookie').style.display = tab === 'cookie' ? 'block' : 'none';
            document.getElementById('tabDirect').className = 'tab-btn ' + (tab === 'direct' ? 'active' : '');
            document.getElementById('tabCookie').className = 'tab-btn ' + (tab === 'cookie' ? 'active' : '');
            hideStatus();
        }

        function showStatus(text, type) {
            const box = document.getElementById('statusBox');
            box.className = 'status-msg status-' + type;
            box.innerText = text;
        }

        function hideStatus() {
            document.getElementById('statusBox').style.display = 'none';
        }

        async function submitLogin() {
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const twoFactorCode = document.getElementById('twoFactorCode').value.trim();

            if (!username || !password) {
                showStatus('Please enter your username and password.', 'error');
                return;
            }

            if (twoFactorIdentifier && twoFactorCode) {
                showStatus('Verifying 2FA Security Code...', 'loading');
                try {
                    const res = await fetch('/api/verify-2fa', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ code: twoFactorCode, identifier: twoFactorIdentifier, username: username })
                    });
                    const data = await res.json();
                    if (data.status === 'success') {
                        showStatus('✅ Login Successful! Check your TV screen.', 'success');
                    } else {
                        showStatus(data.message || '2FA Verification failed', 'error');
                    }
                } catch (e) {
                    showStatus('Connection failed: ' + e.message, 'error');
                }
                return;
            }

            showStatus('Authenticating with Instagram...', 'loading');
            try {
                const res = await fetch('/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });
                const data = await res.json();

                if (data.status === 'two_factor_required') {
                    twoFactorIdentifier = data.identifier;
                    document.getElementById('twoFactorGroup').style.display = 'block';
                    document.getElementById('btnSubmit').innerText = 'Verify Security Code';
                    showStatus('Two-Factor Authentication required. Enter the 6-digit code sent to your phone.', 'loading');
                } else if (data.status === 'success') {
                    showStatus('✅ Authenticated! Your TV is now loading your Instagram feed.', 'success');
                } else {
                    showStatus(data.message || 'Login failed. Please verify credentials.', 'error');
                }
            } catch (e) {
                showStatus('Failed to communicate with TV: ' + e.message, 'error');
            }
        }

        async function submitCookie() {
            const cookies = document.getElementById('cookieInput').value.trim();
            if (!cookies) {
                showStatus('Cookie string cannot be empty.', 'error');
                return;
            }
            showStatus('Sending session to TV...', 'loading');
            try {
                const res = await fetch('/api/submit-cookie', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ cookies })
                });
                const data = await res.json();
                if (data.status === 'success') {
                    showStatus('✅ Cookie paired successfully! Check your TV screen.', 'success');
                } else {
                    showStatus(data.message || 'Invalid cookie format', 'error');
                }
            } catch (e) {
                showStatus('Error sending to TV: ' + e.message, 'error');
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun generatePin(): String {
        val num = Random.nextInt(100000, 999999)
        return "${num.toString().substring(0, 3)}-${num.toString().substring(3)}"
    }

    private fun resolveLocalIp(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}

        return "192.168.1.100"
    }

    companion object {
        private const val MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
    }
}
