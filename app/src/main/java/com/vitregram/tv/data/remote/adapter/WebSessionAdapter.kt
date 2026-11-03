package com.vitregram.tv.data.remote.adapter

import com.google.gson.Gson
import com.google.gson.JsonElement
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
 * Adapter implementing authenticated Instagram Web & Private API communication.
 * Uses official session cookies passed via in-app cookie extractor.
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

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private val igAppId = "936619743392459"

    private fun buildAuthenticatedHeaders(session: AuthSession, referer: String = "https://www.instagram.com/"): Headers {
        val cookieHeader = session.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val builder = Headers.Builder()
            .add("User-Agent", userAgent)
            .add("X-IG-App-ID", igAppId)
            .add("X-ASBD-ID", "129477")
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", referer)
            .add("Accept", "*/*")
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
                if (!body.trim().startsWith("{")) {
                    return@withContext NetworkResult.Success(emptyList())
                }
                val json = gson.fromJson(body, JsonObject::class.java)

                val postList = mutableListOf<Post>()
                val itemsArray = when {
                    json.has("feed_items") && json.get("feed_items").isJsonArray -> json.getAsJsonArray("feed_items")
                    json.has("items") && json.get("items").isJsonArray -> json.getAsJsonArray("items")
                    else -> null
                }

                if (itemsArray != null) {
                    for (element in itemsArray) {
                        if (!element.isJsonObject) continue
                        val itemObj = element.asJsonObject
                        val media = when {
                            itemObj.has("media_or_ad") && itemObj.get("media_or_ad").isJsonObject -> itemObj.getAsJsonObject("media_or_ad")
                            itemObj.has("media") && itemObj.get("media").isJsonObject -> itemObj.getAsJsonObject("media")
                            else -> itemObj
                        }
                        val post = parseMediaToPost(media)
                        if (post != null) {
                            postList.add(post)
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

        val headers = buildAuthenticatedHeaders(session, referer = "https://www.instagram.com/reels/")
        val candidateEndpoints = listOf(
            "https://www.instagram.com/api/v1/clips/discover/",
            "https://www.instagram.com/api/v1/clips/connected/",
            "https://www.instagram.com/api/v1/discover/topical_explore/",
            "https://www.instagram.com/api/v1/feed/timeline/"
        )

        val reels = mutableListOf<Reel>()

        for (endpoint in candidateEndpoints) {
            try {
                val isClipsPost = endpoint.contains("/clips/")
                val request = if (isClipsPost) {
                    val formBody = FormBody.Builder()
                        .add("container_module", if (endpoint.contains("connected")) "clips_viewer_connected" else "clips_viewer_clips_netego")
                        .build()
                    Request.Builder()
                        .url(endpoint)
                        .headers(headers)
                        .post(formBody)
                        .build()
                } else {
                    Request.Builder()
                        .url(endpoint)
                        .headers(headers)
                        .get()
                        .build()
                }

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use
                    }
                    val body = response.body?.string() ?: return@use
                    if (!body.trim().startsWith("{")) {
                        return@use
                    }
                    val json = gson.fromJson(body, JsonObject::class.java)

                    // Case 1: "items" array
                    if (json.has("items") && json.get("items").isJsonArray) {
                        val items = json.getAsJsonArray("items")
                        for (element in items) {
                            parseReelItem(element)?.let { reels.add(it) }
                        }
                    }

                    // Case 2: "feed_items" array
                    if (json.has("feed_items") && json.get("feed_items").isJsonArray) {
                        val feedItems = json.getAsJsonArray("feed_items")
                        for (element in feedItems) {
                            parseReelItem(element)?.let { reels.add(it) }
                        }
                    }

                    // Case 3: "sectional_items" (topical_explore)
                    if (json.has("sectional_items") && json.get("sectional_items").isJsonArray) {
                        val sections = json.getAsJsonArray("sectional_items")
                        for (sec in sections) {
                            if (sec.isJsonObject && sec.asJsonObject.has("layout_content")) {
                                val layout = sec.asJsonObject.getAsJsonObject("layout_content")
                                if (layout.has("medias") && layout.get("medias").isJsonArray) {
                                    for (m in layout.getAsJsonArray("medias")) {
                                        parseReelItem(m)?.let { reels.add(it) }
                                    }
                                }
                            }
                        }
                    }
                }

                if (reels.isNotEmpty()) {
                    return@withContext NetworkResult.Success(reels)
                }
            } catch (e: Exception) {
                // Try next endpoint candidate
            }
        }

        if (reels.isNotEmpty()) {
            NetworkResult.Success(reels)
        } else {
            // Fallback: extract video posts from timeline feed
            val timelineResult = fetchTimelineFeed()
            if (timelineResult is NetworkResult.Success && timelineResult.data.isNotEmpty()) {
                val videoPosts = timelineResult.data
                    .filter { it.mediaItems.any { item -> item.type == MediaType.VIDEO } }
                    .map { post ->
                        val videoItem = post.mediaItems.first { it.type == MediaType.VIDEO }
                        Reel(
                            id = post.id,
                            author = post.author,
                            videoUrl = videoItem.url,
                            thumbnailUrl = post.mediaItems.firstOrNull()?.url ?: "",
                            caption = post.caption,
                            likeCount = post.likeCount,
                            commentCount = post.commentCount,
                            isLiked = post.isLiked
                        )
                    }
                if (videoPosts.isNotEmpty()) {
                    return@withContext NetworkResult.Success(videoPosts)
                }
            }

            // Return clean empty list rather than hard 404 error
            NetworkResult.Success(emptyList())
        }
    }

    suspend fun fetchStories(): NetworkResult<List<Story>> = withContext(Dispatchers.IO) {
        val session = keystoreManager.getSession()
            ?: return@withContext NetworkResult.Error("User not logged in.")

        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/feed/reels_tray/")
            .headers(buildAuthenticatedHeaders(session, referer = "https://www.instagram.com/stories/"))
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error("Failed to fetch stories: HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@withContext NetworkResult.Success(emptyList())
                if (!body.trim().startsWith("{")) {
                    return@withContext NetworkResult.Success(emptyList())
                }
                val json = gson.fromJson(body, JsonObject::class.java)

                val stories = mutableListOf<Story>()
                if (json.has("tray") && json.get("tray").isJsonArray) {
                    val tray = json.getAsJsonArray("tray")
                    for (element in tray) {
                        if (!element.isJsonObject) continue
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
                if (!body.trim().startsWith("{")) {
                    return@withContext NetworkResult.Success(Pair(emptyList(), emptyList()))
                }
                val json = gson.fromJson(body, JsonObject::class.java)

                val users = mutableListOf<User>()
                if (json.has("users") && json.get("users").isJsonArray) {
                    for (element in json.getAsJsonArray("users")) {
                        if (!element.isJsonObject) continue
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
        val id = when {
            userObj.has("pk") && userObj.get("pk").isJsonPrimitive -> userObj.get("pk").asString
            userObj.has("id") && userObj.get("id").isJsonPrimitive -> userObj.get("id").asString
            else -> ""
        }
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
        val id = when {
            media.has("id") && media.get("id").isJsonPrimitive -> media.get("id").asString
            media.has("pk") && media.get("pk").isJsonPrimitive -> media.get("pk").asString
            else -> return null
        }
        val userObj = media.getAsJsonObject("user") ?: return null
        val author = parseUser(userObj)
        val caption = when {
            media.has("caption") && media.get("caption").isJsonObject -> {
                val cap = media.getAsJsonObject("caption")
                if (cap.has("text") && cap.get("text").isJsonPrimitive) cap.get("text").asString else ""
            }
            media.has("caption") && media.get("caption").isJsonPrimitive -> media.get("caption").asString
            media.has("caption_text") && media.get("caption_text").isJsonPrimitive -> media.get("caption_text").asString
            else -> ""
        }
        val likeCount = media.get("like_count")?.asLong ?: 0L
        val commentCount = media.get("comment_count")?.asLong ?: 0L
        val isLiked = media.get("has_liked")?.asBoolean ?: false

        val mediaUrl = extractBestImageUrl(media)
        val mediaType = if (media.get("media_type")?.asInt == 2) {
            MediaType.VIDEO
        } else {
            MediaType.IMAGE
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

    private fun parseReelItem(element: JsonElement): Reel? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val media = when {
            obj.has("media") && obj.get("media").isJsonObject -> obj.getAsJsonObject("media")
            obj.has("clip") && obj.get("clip").isJsonObject -> obj.getAsJsonObject("clip")
            obj.has("media_or_ad") && obj.get("media_or_ad").isJsonObject -> obj.getAsJsonObject("media_or_ad")
            else -> obj
        }
        return parseMediaToReel(media)
    }

    private fun parseMediaToReel(media: JsonObject): Reel? {
        val id = when {
            media.has("id") && media.get("id").isJsonPrimitive -> media.get("id").asString
            media.has("pk") && media.get("pk").isJsonPrimitive -> media.get("pk").asString
            else -> return null
        }
        val userObj = when {
            media.has("user") && media.get("user").isJsonObject -> media.getAsJsonObject("user")
            media.has("owner") && media.get("owner").isJsonObject -> media.getAsJsonObject("owner")
            else -> null
        }
        val author = if (userObj != null) parseUser(userObj) else User(id = "", username = "instagram_creator", fullName = "Creator", profilePictureUrl = "", isVerified = false)

        val caption = when {
            media.has("caption") && media.get("caption").isJsonObject -> {
                val cap = media.getAsJsonObject("caption")
                if (cap.has("text") && cap.get("text").isJsonPrimitive) cap.get("text").asString else ""
            }
            media.has("caption") && media.get("caption").isJsonPrimitive -> media.get("caption").asString
            media.has("caption_text") && media.get("caption_text").isJsonPrimitive -> media.get("caption_text").asString
            else -> ""
        }

        val likeCount = media.get("like_count")?.asLong ?: 0L
        val commentCount = media.get("comment_count")?.asLong ?: 0L
        val isLiked = media.get("has_liked")?.asBoolean ?: false

        val videoUrl = extractVideoUrl(media) ?: return null
        val thumbnail = extractBestImageUrl(media)
        val (audioTitle, audioArtist) = extractAudioInfo(media)

        return Reel(
            id = id,
            author = author,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnail,
            caption = caption,
            audioTitle = audioTitle,
            audioArtist = audioArtist,
            likeCount = likeCount,
            commentCount = commentCount,
            isLiked = isLiked
        )
    }

    private fun extractAudioInfo(media: JsonObject): Pair<String, String> {
        if (media.has("clips_metadata") && media.get("clips_metadata").isJsonObject) {
            val meta = media.getAsJsonObject("clips_metadata")
            if (meta.has("music_info") && meta.get("music_info").isJsonObject) {
                val musicInfo = meta.getAsJsonObject("music_info")
                if (musicInfo.has("music_asset_info") && musicInfo.get("music_asset_info").isJsonObject) {
                    val asset = musicInfo.getAsJsonObject("music_asset_info")
                    val title = asset.get("title")?.asString ?: "Original Audio"
                    val artist = asset.get("display_artist")?.asString ?: ""
                    return Pair(title, artist)
                }
            }
            if (meta.has("original_sound_info") && meta.get("original_sound_info").isJsonObject) {
                val orig = meta.getAsJsonObject("original_sound_info")
                val title = orig.get("original_audio_title")?.asString ?: "Original Audio"
                val artist = orig.getAsJsonObject("ig_artist")?.get("username")?.asString ?: ""
                return Pair(title, artist)
            }
        }
        return Pair("Original Audio", "")
    }

    private fun extractBestImageUrl(media: JsonObject): String {
        if (media.has("thumbnail_url") && media.get("thumbnail_url").isJsonPrimitive) {
            return media.get("thumbnail_url").asString
        }
        if (media.has("image_versions2") && media.get("image_versions2").isJsonObject) {
            val candidates = media.getAsJsonObject("image_versions2").getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0 && candidates[0].isJsonObject) {
                return candidates[0].asJsonObject.get("url")?.asString ?: ""
            }
        }
        return ""
    }

    private fun extractVideoUrl(media: JsonObject): String? {
        if (media.has("video_url") && media.get("video_url").isJsonPrimitive) {
            return media.get("video_url").asString
        }
        if (media.has("video_versions") && media.get("video_versions").isJsonArray) {
            val versions = media.getAsJsonArray("video_versions")
            if (versions.size() > 0 && versions[0].isJsonObject) {
                return versions[0].asJsonObject.get("url")?.asString
            }
        }
        if (media.has("carousel_media") && media.get("carousel_media").isJsonArray) {
            val carousel = media.getAsJsonArray("carousel_media")
            for (item in carousel) {
                if (item.isJsonObject) {
                    val url = extractVideoUrl(item.asJsonObject)
                    if (!url.isNullOrBlank()) return url
                }
            }
        }
        return null
    }
}
