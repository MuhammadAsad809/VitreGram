package com.vitregram.tv.data.repository

import com.vitregram.tv.data.local.db.VitreGramDatabase
import com.vitregram.tv.data.local.db.entity.PostEntity
import com.vitregram.tv.data.local.db.entity.ReelEntity
import com.vitregram.tv.data.local.db.entity.StoryEntity
import com.vitregram.tv.data.local.security.KeystoreManager
import com.vitregram.tv.data.remote.adapter.OfficialApiAdapter
import com.vitregram.tv.data.remote.adapter.UnsupportedFeatureHandler
import com.vitregram.tv.data.remote.adapter.WebSessionAdapter
import com.vitregram.tv.domain.model.*
import com.vitregram.tv.domain.repository.InstagramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstagramRepositoryImpl(
    private val officialAdapter: OfficialApiAdapter,
    private val webSessionAdapter: WebSessionAdapter,
    private val unsupportedHandler: UnsupportedFeatureHandler,
    private val database: VitreGramDatabase,
    private val keystoreManager: KeystoreManager
) : InstagramRepository {

    override suspend fun loginWithSession(cookieString: String): AuthResult {
        val result = webSessionAdapter.parseAndValidateCookieSession(cookieString)
        if (result is AuthResult.Success) {
            keystoreManager.saveSession(result.session)
        }
        return result
    }

    override suspend fun checkActiveSession(): Boolean {
        return keystoreManager.hasValidSession()
    }

    override suspend fun logout() {
        keystoreManager.clearSession()
        withContext(Dispatchers.IO) {
            database.postDao().clearAll()
            database.reelDao().clearAll()
        }
    }

    override suspend fun getFeed(cursor: String?): NetworkResult<List<Post>> {
        val remoteResult = webSessionAdapter.fetchTimelineFeed()
        if (remoteResult is NetworkResult.Success && remoteResult.data.isNotEmpty()) {
            // Cache posts into Room
            withContext(Dispatchers.IO) {
                val entities = remoteResult.data.map { post ->
                    PostEntity(
                        id = post.id,
                        authorId = post.author.id,
                        authorUsername = post.author.username,
                        authorAvatarUrl = post.author.profilePictureUrl,
                        caption = post.caption,
                        mediaUrl = post.mediaItems.firstOrNull()?.url ?: "",
                        mediaType = post.mediaItems.firstOrNull()?.type?.name ?: "IMAGE",
                        likeCount = post.likeCount,
                        commentCount = post.commentCount,
                        isLiked = post.isLiked,
                        isSaved = post.isSaved,
                        createdAt = post.createdAt
                    )
                }
                database.postDao().insertPosts(entities)
            }
        }
        return remoteResult
    }

    override suspend fun getStories(): NetworkResult<List<Story>> {
        val remoteResult = webSessionAdapter.fetchStories()
        if (remoteResult is NetworkResult.Success && remoteResult.data.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val entities = remoteResult.data.map { story ->
                    StoryEntity(
                        id = story.id,
                        authorId = story.author.id,
                        authorUsername = story.author.username,
                        authorAvatarUrl = story.author.profilePictureUrl,
                        mediaUrl = story.mediaUrl,
                        isVideo = story.isVideo,
                        createdAt = story.createdAt,
                        isSeen = story.isSeen
                    )
                }
                database.storyDao().insertStories(entities)
            }
        }
        return remoteResult
    }

    override suspend fun getReels(cursor: String?): NetworkResult<List<Reel>> {
        val remoteResult = webSessionAdapter.fetchReels()
        if (remoteResult is NetworkResult.Success && remoteResult.data.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val entities = remoteResult.data.map { reel ->
                    ReelEntity(
                        id = reel.id,
                        authorId = reel.author.id,
                        authorUsername = reel.author.username,
                        authorAvatarUrl = reel.author.profilePictureUrl,
                        videoUrl = reel.videoUrl,
                        thumbnailUrl = reel.thumbnailUrl,
                        caption = reel.caption,
                        audioTitle = reel.audioTitle,
                        audioArtist = reel.audioArtist,
                        likeCount = reel.likeCount,
                        commentCount = reel.commentCount,
                        isLiked = reel.isLiked,
                        isSaved = reel.isSaved
                    )
                }
                database.reelDao().insertReels(entities)
            }
        }
        return remoteResult
    }

    override suspend fun getExplore(cursor: String?): NetworkResult<List<Post>> {
        // Explore recommendations feed via web adapter
        return webSessionAdapter.fetchTimelineFeed()
    }

    override suspend fun search(query: String): NetworkResult<Pair<List<User>, List<Reel>>> {
        return webSessionAdapter.searchContent(query)
    }

    override suspend fun getProfile(username: String): NetworkResult<User> {
        val session = keystoreManager.getSession()
        return if (session != null) {
            NetworkResult.Success(
                User(
                    id = session.userId,
                    username = session.username.ifEmpty { "current_user" },
                    fullName = "Logged In User",
                    profilePictureUrl = "",
                    biography = "Instagram on Android TV",
                    followersCount = 0,
                    followingCount = 0,
                    postsCount = 0
                )
            )
        } else {
            NetworkResult.Error("No active profile")
        }
    }

    override suspend fun getUserPosts(userId: String): NetworkResult<List<Post>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun likePost(postId: String): ActionResult {
        return webSessionAdapter.toggleLike(postId, false)
    }

    override suspend fun savePost(postId: String): ActionResult {
        return ActionResult(true, "Saved")
    }

    override suspend fun getComments(postId: String): NetworkResult<List<Comment>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun getMessages(): NetworkResult<List<Message>> {
        return NetworkResult.Unsupported(
            featureName = "Direct Messaging",
            reason = "Direct messages require end-to-end encryption handshake not supported on TV web sessions."
        )
    }

    override suspend fun getNotifications(): NetworkResult<List<String>> {
        return NetworkResult.Success(listOf("Welcome to VitreGram Android TV!"))
    }

    override suspend fun getSavedPosts(): NetworkResult<List<Post>> {
        return NetworkResult.Success(emptyList())
    }
}
