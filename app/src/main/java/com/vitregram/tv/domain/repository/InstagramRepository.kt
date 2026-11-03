package com.vitregram.tv.domain.repository

import com.vitregram.tv.domain.model.*

/**
 * Clean Architecture repository abstraction for Instagram client operations.
 * Allows decoupling between official API, web session adapter, and unsupported feature fallbacks.
 */
interface InstagramRepository {

    suspend fun loginWithSession(cookieString: String): AuthResult

    suspend fun loginWithOAuthToken(accessToken: String, userId: String, cookies: Map<String, String> = emptyMap()): AuthResult

    suspend fun checkActiveSession(): Boolean

    suspend fun logout()

    suspend fun getFeed(cursor: String? = null): NetworkResult<List<Post>>

    suspend fun getStories(): NetworkResult<List<Story>>

    suspend fun getReels(cursor: String? = null): NetworkResult<List<Reel>>

    suspend fun getExplore(cursor: String? = null): NetworkResult<List<Post>>

    suspend fun search(query: String): NetworkResult<Pair<List<User>, List<Reel>>>

    suspend fun getProfile(username: String): NetworkResult<User>

    suspend fun getUserPosts(userId: String): NetworkResult<List<Post>>

    suspend fun likePost(postId: String): ActionResult

    suspend fun savePost(postId: String): ActionResult

    suspend fun getComments(postId: String): NetworkResult<List<Comment>>

    suspend fun getMessages(): NetworkResult<List<Message>>

    suspend fun getNotifications(): NetworkResult<List<String>>

    suspend fun getSavedPosts(): NetworkResult<List<Post>>
}
