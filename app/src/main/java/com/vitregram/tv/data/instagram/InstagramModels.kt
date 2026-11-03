package com.vitregram.tv.data.instagram

import com.google.gson.annotations.SerializedName
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.model.Reel
import com.vitregram.tv.domain.model.User

/**
 * Generic response wrapper returned by the Python Instagram service modules.
 */
data class PythonResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error_type") val errorType: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("extra") val extra: Map<String, Any>? = null
)

/**
 * Authentication response payload containing tokens, session cookies, and instagrapi settings dump.
 */
data class PythonAuthData(
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String = "",
    @SerializedName("sessionToken") val sessionToken: String,
    @SerializedName("csrfToken") val csrfToken: String? = null,
    @SerializedName("cookies") val cookies: Map<String, String> = emptyMap(),
    @SerializedName("settingsJson") val settingsJson: String? = null
)

/**
 * Timeline response including posts and pagination cursor.
 */
data class PythonTimelineData(
    @SerializedName("posts") val posts: List<Post> = emptyList(),
    @SerializedName("nextCursor") val nextCursor: String? = null
)

/**
 * Combined search response payload.
 */
data class PythonSearchData(
    @SerializedName("users") val users: List<User> = emptyList(),
    @SerializedName("media") val media: List<Reel> = emptyList()
)

/**
 * Hashtag search entity.
 */
data class PythonHashtagData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("mediaCount") val mediaCount: Long = 0
)
