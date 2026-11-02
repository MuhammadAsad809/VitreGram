package com.vitregram.tv.domain.model

data class User(
    val id: String,
    val username: String,
    val fullName: String = "",
    val profilePictureUrl: String = "",
    val biography: String = "",
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val postsCount: Long = 0,
    val isVerified: Boolean = false,
    val isFollowing: Boolean = false,
    val isPrivate: Boolean = false
)

enum class MediaType {
    IMAGE,
    VIDEO,
    CAROUSEL
}

data class MediaItem(
    val id: String,
    val url: String,
    val type: MediaType,
    val width: Int = 1080,
    val height: Int = 1350,
    val videoDurationSeconds: Double = 0.0
)

data class Post(
    val id: String,
    val author: User,
    val caption: String = "",
    val mediaItems: List<MediaItem>,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Reel(
    val id: String,
    val author: User,
    val videoUrl: String,
    val thumbnailUrl: String = "",
    val caption: String = "",
    val audioTitle: String = "Original audio",
    val audioArtist: String = "",
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)

data class Story(
    val id: String,
    val author: User,
    val mediaUrl: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val author: User,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val likeCount: Long = 0,
    val isLiked: Boolean = false
)

data class Message(
    val id: String,
    val conversationId: String,
    val sender: User,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

data class AuthSession(
    val userId: String,
    val username: String,
    val sessionToken: String,
    val csrfToken: String? = null,
    val cookies: Map<String, String> = emptyMap(),
    val expirationTimestamp: Long = Long.MAX_VALUE
)
