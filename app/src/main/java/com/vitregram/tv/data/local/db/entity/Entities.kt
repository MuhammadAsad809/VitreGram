package com.vitregram.tv.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vitregram.tv.domain.model.MediaItem
import com.vitregram.tv.domain.model.MediaType
import com.vitregram.tv.domain.model.Post
import com.vitregram.tv.domain.model.Reel
import com.vitregram.tv.domain.model.Story
import com.vitregram.tv.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val profilePictureUrl: String,
    val biography: String,
    val followersCount: Long,
    val followingCount: Long,
    val isVerified: Boolean
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val caption: String,
    val mediaUrl: String,
    val mediaType: String,
    val likeCount: Long,
    val commentCount: Long,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val createdAt: Long
)

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val audioTitle: String,
    val audioArtist: String,
    val likeCount: Long,
    val commentCount: Long,
    val isLiked: Boolean,
    val isSaved: Boolean
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val mediaUrl: String,
    val isVideo: Boolean,
    val createdAt: Long,
    val isSeen: Boolean
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val mediaId: String,
    val playbackProgressMs: Long,
    val lastWatchedTimestamp: Long
)

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    fullName = fullName,
    profilePictureUrl = profilePictureUrl,
    biography = biography,
    followersCount = followersCount,
    followingCount = followingCount,
    isVerified = isVerified
)

fun PostEntity.toDomain(): Post = Post(
    id = id,
    author = User(
        id = authorId,
        username = authorUsername,
        fullName = authorUsername,
        profilePictureUrl = authorAvatarUrl
    ),
    caption = caption,
    mediaItems = listOf(
        MediaItem(
            id = id,
            url = mediaUrl,
            type = try { MediaType.valueOf(mediaType) } catch (e: Exception) { MediaType.IMAGE }
        )
    ),
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isSaved = isSaved,
    createdAt = createdAt
)

fun ReelEntity.toDomain(): Reel = Reel(
    id = id,
    author = User(
        id = authorId,
        username = authorUsername,
        fullName = authorUsername,
        profilePictureUrl = authorAvatarUrl
    ),
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    caption = caption,
    audioTitle = audioTitle,
    audioArtist = audioArtist,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isSaved = isSaved
)

fun StoryEntity.toDomain(): Story = Story(
    id = id,
    author = User(
        id = authorId,
        username = authorUsername,
        fullName = authorUsername,
        profilePictureUrl = authorAvatarUrl
    ),
    mediaUrl = mediaUrl,
    isVideo = isVideo,
    durationSeconds = 15,
    createdAt = createdAt,
    isSeen = isSeen
)

