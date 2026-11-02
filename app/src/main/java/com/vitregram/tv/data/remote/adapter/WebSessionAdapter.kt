package com.vitregram.tv.data.remote.adapter

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vitregram.tv.data.local.security.KeystoreManager
import com.vitregram.tv.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Adapter implementing authenticated Instagram Web API communication.
 * Uses official session cookies passed via TV pairing / secure login.
 */
class WebSessionAdapter(
    private val keystoreManager: KeystoreManager
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()

    private val userAgent = "Mozilla/5.0 (Linux; Android 12; Android TV Build/STT.211029.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private val igAppId = "936619743392459"

    private fun buildAuthenticatedHeaders(session: AuthSession): Headers {
        val cookieHeader = session.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val builder = Headers.Builder()
            .add("User-Agent", userAgent)
            .add("X-IG-App-ID", igAppId)
            .add("X-ASBD-ID", "129477")
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", "https://www.instagram.com/")
            .add("Cookie", cookieHeader)

        session.csrfToken?.let {
            builder.add("X-CSRFToken", it)
        }

        return builder.build()
    }

    suspend fun parseAndValidateCookieSession(cookieString: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val cookiePairs = cookieString.split(";")
                .map { it.trim() }
                .filter { it.contains("=") }
                .associate {
                    val idx = it.indexOf("=")
                    it.substring(0, idx).trim() to it.substring(idx + 1).trim()
                }

            val sessionId = cookiePairs["sessionid"]
            val userId = cookiePairs["ds_user_id"]
            val csrfToken = cookiePairs["csrftoken"]

            if (sessionId.isNullOrEmpty() || userId.isNullOrEmpty()) {
                return@withContext AuthResult.Error("Invalid session cookies. 'sessionid' and 'ds_user_id' are required.")
            }

            val session = AuthSession(
                userId = userId,
                username = "",
                sessionToken = sessionId,
                csrfToken = csrfToken,
                cookies = cookiePairs
            )

            // Test session by fetching current user profile
            val headers = buildAuthenticatedHeaders(session)
            val request = Request.Builder()
                .url("https://www.instagram.com/api/v1/users/lookup_phone_or_email/?user_id=$userId")
                .headers(headers)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code in 200..299 || response.code == 404) {
                    // Session is valid
                    AuthResult.Success(session)
                } else if (response.code == 401 || response.code == 403) {
                    AuthResult.Error("Instagram rejected the session. Please log in again.")
                } else {
                    AuthResult.Success(session)
                }
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication failed")
        }
    }

    suspend fun fetchTimelineFeed(): NetworkResult<List<Post>> = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext NetworkResult.Error("User not logged in. Please sign in.")

        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/feed/timeline/")
            .headers(buildAuthenticatedHeaders(session))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401 || response.code == 403) {
                        return@withContext NetworkResult.Error("Session expired. Please log in again.", code = response.code)
                    }
                    return@withContext NetworkResult.Error("Instagram returned error ${response.code}", code = response.code)
                }

                val body = response.body?.string() ?: return@withContext NetworkResult.Success(emptyList())
                val json = gson.fromJson(body, JsonObject::class.java)

                val postList = mutableListOf<Post>()
                if (json.has("feed_items")) {
                    val items = json.getAsJsonArray("feed_items")
                    for (element in items) {
                        val itemObj = element.asJsonObject
                        if (itemObj.has("media_or_ad")) {
                            val media = itemObj.getAsJsonObject("media_or_ad")
                            val post = parseMediaToPost(media)
                            if (post != null) {
                                postList.add(post)
                            }
                        }
                    }
                }

                NetworkResult.Success(postList)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}", throwable = e)
        }
    }

    suspend fun fetchReels(): NetworkResult<List<Reel>> = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext NetworkResult.Error("User not logged in. Please sign in.")

        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/clips/home/")
            .headers(buildAuthenticatedHeaders(session))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error("Failed to fetch Reels: HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@withContext NetworkResult.Success(emptyList())
                val json = gson.fromJson(body, JsonObject::class.java)

                val reels = mutableListOf<Reel>()
                if (json.has("items")) {
                    val items = json.getAsJsonArray("items")
                    for (element in items) {
                        val media = element.asJsonObject.getAsJsonObject("media") ?: continue
                        val reel = parseMediaToReel(media)
                        if (reel != null) {
                            reels.add(reel)
                        }
                    }
                }
                NetworkResult.Success(reels)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error fetching reels: ${e.message}", throwable = e)
        }
    }

    suspend fun fetchStories(): NetworkResult<List<Story>> = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext NetworkResult.Error("User not logged in.")

        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/feed/reels_tray/")
            .headers(buildAuthenticatedHeaders(session))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error("Failed to fetch stories: HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@withContext NetworkResult.Success(emptyList())
                val json = gson.fromJson(body, JsonObject::class.java)

                val stories = mutableListOf<Story>()
                if (json.has("tray")) {
                    val tray = json.getAsJsonArray("tray")
                    for (element in tray) {
                        val trayItem = element.asJsonObject
                        val userObj = trayItem.getAsJsonObject("user") ?: continue
                        val user = parseUser(userObj)
                        val storyId = trayItem.get("id")?.asString ?: continue
                        val isSeen = trayItem.get("seen")?.asLong ?: 0L > 0L

                        val mediaUrl = if (trayItem.has("items") && trayItem.getAsJsonArray("items").size() > 0) {
                            val firstMedia = trayItem.getAsJsonArray("items")[0].asJsonObject
                            extractBestImageUrl(firstMedia)
                        } else {
                            user.profilePictureUrl
                        }

                        stories.add(
                            Story(
                                id = storyId,
                                author = user,
                                mediaUrl = mediaUrl,
                                isVideo = false,
                                isSeen = isSeen
                            )
                        )
                    }
                }
                NetworkResult.Success(stories)
            }
        } catch (e: Exception) {
            NetworkResult.Error("Failed to load stories: ${e.message}", throwable = e)
        }
    }

    suspend fun searchContent(query: String): NetworkResult<Pair<List<User>, List<Reel>>> = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext NetworkResult.Error("User not logged in.")

        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/web/search/topsearch/?context=blended&query=$query")
            .headers(buildAuthenticatedHeaders(session))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error("Search request failed: HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@withContext NetworkResult.Success(Pair(emptyList(), emptyList()))
                val json = gson.fromJson(body, JsonObject::class.java)

                val users = mutableListOf<User>()
                if (json.has("users")) {
                    for (element in json.getAsJsonArray("users")) {
                        val userWrapper = element.asJsonObject.getAsJsonObject("user") ?: continue
                        users.add(parseUser(userWrapper))
                    }
                }

                NetworkResult.Success(Pair(users, emptyList()))
            }
        } catch (e: Exception) {
            NetworkResult.Error("Search failed: ${e.message}", throwable = e)
        }
    }

    suspend fun toggleLike(postId: String, isLikedNow: Boolean): ActionResult = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext ActionResult(false, "User not logged in.")

        val action = if (isLikedNow) "unlike" else "like"
        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/web/likes/$postId/$action/")
            .headers(buildAuthenticatedHeaders(session))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ActionResult(true)
                } else {
                    ActionResult(false, "Failed to $action post: HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, e.message)
        }
    }

    private fun parseUser(userObj: JsonObject): User {
        val id = userObj.get("pk")?.asString ?: userObj.get("id")?.asString ?: ""
        val username = userObj.get("username")?.asString ?: ""
        val fullName = userObj.get("full_name")?.asString ?: ""
        val avatar = userObj.get("profile_pic_url")?.asString ?: ""
        val isVerified = userObj.get("is_verified")?.asBoolean ?: false
        return User(
            id = id,
            username = username,
            fullName = fullName,
            profilePictureUrl = avatar,
            isVerified = isVerified
        )
    }

    private fun parseMediaToPost(media: JsonObject): Post? {
        val id = media.get("id")?.asString ?: media.get("pk")?.asString ?: return null
        val userObj = media.getAsJsonObject("user") ?: return null
        val author = parseUser(userObj)
        val caption = media.getAsJsonObject("caption")?.get("text")?.asString ?: ""
        val likeCount = media.get("like_count")?.asLong ?: 0L
        val commentCount = media.get("comment_count")?.asLong ?: 0L
        val isLiked = media.get("has_liked")?.asBoolean ?: false

        val mediaUrl = extractBestImageUrl(media)
        val mediaType = if (media.get("media_type")?.asInt == 2) {
            com.vitregram.tv.domain.model.MediaType.VIDEO
        } else {
            com.vitregram.tv.domain.model.MediaType.IMAGE
        }

        return Post(
            id = id,
            author = author,
            caption = caption,
            mediaItems = listOf(MediaItem(id = id, url = mediaUrl, type = mediaType)),
            likeCount = likeCount,
            commentCount = commentCount,
            isLiked = isLiked
        )
    }

    private fun parseMediaToReel(media: JsonObject): Reel? {
        val id = media.get("id")?.asString ?: media.get("pk")?.asString ?: return null
        val userObj = media.getAsJsonObject("user") ?: return null
        val author = parseUser(userObj)
        val caption = media.getAsJsonObject("caption")?.get("text")?.asString ?: ""
        val likeCount = media.get("like_count")?.asLong ?: 0L
        val commentCount = media.get("comment_count")?.asLong ?: 0L
        val isLiked = media.get("has_liked")?.asBoolean ?: false

        val videoUrl = extractVideoUrl(media) ?: return null
        val thumbnail = extractBestImageUrl(media)

        return Reel(
            id = id,
            author = author,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnail,
            caption = caption,
            likeCount = likeCount,
            commentCount = commentCount,
            isLiked = isLiked
        )
    }

    private fun extractBestImageUrl(media: JsonObject): String {
        if (media.has("image_versions2")) {
            val candidates = media.getAsJsonObject("image_versions2").getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                return candidates[0].asJsonObject.get("url")?.asString ?: ""
            }
        }
        return ""
    }

    private fun extractVideoUrl(media: JsonObject): String? {
        if (media.has("video_versions")) {
            val versions = media.getAsJsonArray("video_versions")
            if (versions != null && versions.size() > 0) {
                return versions[0].asJsonObject.get("url")?.asString
            }
        }
        return null
    }
}
