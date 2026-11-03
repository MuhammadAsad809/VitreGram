package com.vitregram.tv.data.instagram

import com.google.gson.Gson
import com.vitregram.tv.data.local.db.VitreGramDatabase
import com.vitregram.tv.data.local.db.entity.*
import com.vitregram.tv.data.local.security.KeystoreManager
import com.vitregram.tv.domain.model.*
import com.vitregram.tv.domain.repository.InstagramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Chaquopy instagrapi-backed repository implementation for VitreGram Android TV.
 * Integrates Room database offline caching and Android Keystore AES-256 session encryption.
 */
class ChaquopyInstagramRepository(
    private val dataSource: InstagramDataSource,
    private val database: VitreGramDatabase,
    private val keystoreManager: KeystoreManager,
    private val gson: Gson = Gson()
) : InstagramRepository {

    override suspend fun loginWithSession(cookieString: String): AuthResult {
        // Parse cookie string into map for instagrapi
        val cookieMap = mutableMapOf<String, String>()
        cookieString.split(";").forEach { pair ->
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                cookieMap[parts[0].trim()] = parts[1].trim()
            }
        }
        val cookiesJson = gson.toJson(cookieMap)
        val result = dataSource.loginWithCookies(cookiesJson)
        if (result is AuthResult.Success) {
            keystoreManager.saveSession(result.session)
            // Persist Python instagrapi settings in Android Keystore
            val settingsDump = dataSource.dumpSession()
            if (settingsDump.isNotEmpty() && settingsDump != "{}") {
                keystoreManager.savePythonSettings(settingsDump)
            }
        }
        return result
    }

    suspend fun loginWithCredentials(username: String, password: String, verificationCode: String = ""): AuthResult {
        val result = dataSource.login(username, password, verificationCode)
        if (result is AuthResult.Success) {
            keystoreManager.saveSession(result.session)
            val settingsDump = dataSource.dumpSession()
            if (settingsDump.isNotEmpty() && settingsDump != "{}") {
                keystoreManager.savePythonSettings(settingsDump)
            }
        }
        return result
    }

    override suspend fun checkActiveSession(): Boolean {
        // First check Keystore
        if (!keystoreManager.hasValidSession()) return false
        
        // Restore session into Python runtime if not already active
        val savedSettings = keystoreManager.getPythonSettings()
        if (!savedSettings.isNullOrEmpty()) {
            dataSource.restoreSession(savedSettings)
        } else {
            // Restore from cookies
            val session = keystoreManager.getSession()
            if (session != null && session.cookies.isNotEmpty()) {
                dataSource.loginWithCookies(gson.toJson(session.cookies))
            }
        }
        return dataSource.checkSession()
    }

    override suspend fun logout() {
        keystoreManager.clearSession()
        dataSource.logout()
        withContext(Dispatchers.IO) {
            database.postDao().clearAll()
            database.reelDao().clearAll()
            database.storyDao().clearAll()
        }
    }

    override suspend fun getFeed(cursor: String?): NetworkResult<List<Post>> {
        val result = dataSource.getTimeline(cursor)
        return when (result) {
            is NetworkResult.Success -> {
                val posts = result.data.posts
                if (posts.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val entities = posts.map { post ->
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
                NetworkResult.Success(posts)
            }
            is NetworkResult.Error -> {
                // Fallback to Room DB cache
                val cached = withContext(Dispatchers.IO) { database.postDao().getCachedPosts() }
                if (cached.isNotEmpty()) {
                    NetworkResult.Success(cached.map { it.toDomain() })
                } else {
                    result
                }
            }
            else -> result
        }
    }

    override suspend fun getStories(): NetworkResult<List<Story>> {
        val result = dataSource.getStoriesTray()
        return when (result) {
            is NetworkResult.Success -> {
                val stories = result.data
                if (stories.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val entities = stories.map { s ->
                            StoryEntity(
                                id = s.id,
                                authorId = s.author.id,
                                authorUsername = s.author.username,
                                authorAvatarUrl = s.author.profilePictureUrl,
                                mediaUrl = s.mediaUrl,
                                isVideo = s.isVideo,
                                createdAt = s.createdAt,
                                isSeen = s.isSeen
                            )
                        }
                        database.storyDao().insertStories(entities)
                    }
                }
                NetworkResult.Success(stories)
            }
            is NetworkResult.Error -> {
                val cached = withContext(Dispatchers.IO) { database.storyDao().getCachedStories() }
                if (cached.isNotEmpty()) {
                    NetworkResult.Success(cached.map { it.toDomain() })
                } else {
                    result
                }
            }
            else -> result
        }
    }

    override suspend fun getReels(cursor: String?): NetworkResult<List<Reel>> {
        val result = dataSource.getReels(amount = 15, lastMediaPk = cursor)
        return when (result) {
            is NetworkResult.Success -> {
                val reels = result.data
                if (reels.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val entities = reels.map { r ->
                            ReelEntity(
                                id = r.id,
                                authorId = r.author.id,
                                authorUsername = r.author.username,
                                authorAvatarUrl = r.author.profilePictureUrl,
                                videoUrl = r.videoUrl,
                                thumbnailUrl = r.thumbnailUrl,
                                caption = r.caption,
                                audioTitle = r.audioTitle,
                                audioArtist = r.audioArtist,
                                likeCount = r.likeCount,
                                commentCount = r.commentCount,
                                isLiked = r.isLiked,
                                isSaved = r.isSaved
                            )
                        }
                        database.reelDao().insertReels(entities)
                    }
                }
                NetworkResult.Success(reels)
            }
            is NetworkResult.Error -> {
                val cached = withContext(Dispatchers.IO) { database.reelDao().getCachedReels() }
                if (cached.isNotEmpty()) {
                    NetworkResult.Success(cached.map { it.toDomain() })
                } else {
                    result
                }
            }
            else -> result
        }
    }

    override suspend fun getExplore(cursor: String?): NetworkResult<List<Post>> {
        return dataSource.getExplore(cursor)
    }

    override suspend fun search(query: String): NetworkResult<Pair<List<User>, List<Reel>>> {
        return dataSource.searchAll(query)
    }

    override suspend fun getProfile(username: String): NetworkResult<User> {
        return dataSource.getUserInfo(username)
    }

    override suspend fun getUserPosts(userId: String): NetworkResult<List<Post>> {
        return dataSource.getUserPosts(userId)
    }

    override suspend fun likePost(postId: String): ActionResult {
        return dataSource.likeMedia(postId)
    }

    override suspend fun savePost(postId: String): ActionResult {
        return dataSource.saveMedia(postId)
    }

    override suspend fun getComments(postId: String): NetworkResult<List<Comment>> {
        return dataSource.getComments(postId)
    }

    override suspend fun getMessages(): NetworkResult<List<Message>> {
        return dataSource.getThreads()
    }

    override suspend fun getNotifications(): NetworkResult<List<String>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun getSavedPosts(): NetworkResult<List<Post>> {
        return NetworkResult.Success(emptyList())
    }
}
