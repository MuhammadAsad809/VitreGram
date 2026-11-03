package com.vitregram.tv.data.remote.oauth

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Data model for Instagram OAuth 2.0 Access Token response.
 * Follows Meta Instagram Basic Display & Graph API OAuth specification.
 */
data class OAuthTokenResponse(
    val accessToken: String,
    val userId: String,
    val rawJson: String = ""
)

/**
 * Kotlin implementation of InstagramAuthHelper from xiandanin/instagram-login-android.
 * Manages OAuth 2.0 authorization URL construction, redirect code parsing, and token exchange.
 */
object InstagramOAuthHelper {

    const val URL_OAUTH = "https://api.instagram.com/oauth/authorize"
    const val URL_ACCESS_TOKEN = "https://api.instagram.com/oauth/access_token"

    // Default OAuth credentials (can be overridden via UI/Settings for custom Meta developer apps)
    const val DEFAULT_CLIENT_ID = "bab949224eab4d0da060f64c29a9c1f4"
    const val DEFAULT_CLIENT_SECRET = "287fb5238b944df69bb1951d16148940"
    const val DEFAULT_REDIRECT_URI = "https://www.instagram.com/"

    private val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    /**
     * Builds the authorization page URL.
     * Equivalent to InstagramAuthHelper.getRedirectOAuthUrl() from xiandanin/instagram-login-android.
     */
    fun getRedirectOAuthUrl(
        clientId: String,
        redirectUri: String,
        scope: String = "user_profile,user_media"
    ): String {
        return "$URL_OAUTH?client_id=$clientId&redirect_uri=$redirectUri&response_type=code&scope=$scope"
    }

    /**
     * Parses the authorization code from the redirect URL.
     * Equivalent to InstagramAuthHelper.parserCodeByUrl(url) from xiandanin/instagram-login-android.
     * Handles standard query strings as well as Instagram's trailing '#_' fragments.
     */
    fun parseCodeFromUrl(url: String?, redirectUri: String): String? {
        if (url.isNullOrBlank()) return null

        val cleanUrl = url.removeSuffix("#_")
        val cleanRedirectUri = redirectUri.removeSuffix("#_").removeSuffix("/")

        // Check if the current URL starts with or matches redirectUri
        val normalizedUrl = cleanUrl.removeSuffix("/")
        if (!normalizedUrl.startsWith(cleanRedirectUri) && !cleanUrl.contains("code=")) {
            return null
        }

        return try {
            val uri = URI(cleanUrl)
            val query = uri.query ?: uri.rawQuery ?: ""
            val params = queryStringToParams(query)
            params["code"]?.removeSuffix("#_")
        } catch (e: Exception) {
            // Fallback manual query parser
            if (cleanUrl.contains("code=")) {
                val substring = cleanUrl.substringAfter("code=").substringBefore("&")
                substring.removeSuffix("#_")
            } else {
                null
            }
        }
    }

    /**
     * Converts a URL query string to a key-value Map.
     * Equivalent to InstagramAuthHelper.queryString2Params() from xiandanin/instagram-login-android.
     */
    fun queryStringToParams(queryString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (queryString.isBlank()) return params

        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = if (idx < pair.length - 1) {
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                } else {
                    ""
                }
                params[key] = value
            }
        }
        return params
    }

    /**
     * Builds form parameters for exchanging authorization code for an Access Token.
     * Equivalent to InstagramAuthHelper.buildAccessTokenParams() from xiandanin/instagram-login-android.
     */
    fun buildAccessTokenParams(
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        code: String
    ): Map<String, String> {
        return mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri,
            "code" to code
        )
    }

    /**
     * Executes the HTTP POST to request the Access Token.
     * Implements InstagramRequest.requestAccessToken from xiandanin/instagram-login-android.
     */
    suspend fun requestAccessToken(
        tokenUrl: String = URL_ACCESS_TOKEN,
        params: Map<String, String>,
        client: OkHttpClient = defaultClient
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val formBodyBuilder = FormBody.Builder()
            for ((key, value) in params) {
                formBodyBuilder.add(key, value)
            }

            val request = Request.Builder()
                .url(tokenUrl)
                .post(formBodyBuilder.build())
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("OAuth token request failed (${response.code}): $bodyString")
                )
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
            val accessToken = jsonObject.get("access_token")?.asString
                ?: return@withContext Result.failure(Exception("Missing access_token in response: $bodyString"))

            val userId = jsonObject.get("user_id")?.asString
                ?: jsonObject.get("user")?.asJsonObject?.get("id")?.asString
                ?: "unknown"

            Result.success(
                OAuthTokenResponse(
                    accessToken = accessToken,
                    userId = userId,
                    rawJson = bodyString
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
