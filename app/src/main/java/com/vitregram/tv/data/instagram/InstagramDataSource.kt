package com.vitregram.tv.data.instagram

import com.vitregram.tv.core.python.PythonInstagramBridge
import com.vitregram.tv.domain.model.*

/**
 * Remote data source abstraction for Chaquopy-backed instagrapi Instagram operations.
 */
interface InstagramDataSource {
    suspend fun login(username: String, password: String, verificationCode: String = ""): AuthResult
    suspend fun loginBySessionId(sessionId: String): AuthResult
    suspend fun loginWithCookies(cookiesJson: String): AuthResult
    suspend fun restoreSession(settingsJson: String): Boolean
    suspend fun dumpSession(): String
    suspend fun checkSession(): Boolean
    suspend fun logout(): Boolean

    suspend fun getTimeline(cursor: String? = null): NetworkResult<PythonTimelineData>
    suspend fun getExplore(cursor: String? = null): NetworkResult<List<Post>>
    suspend fun getStoriesTray(): NetworkResult<List<Story>>
    suspend fun getUserStories(userId: String): NetworkResult<List<Story>>
    suspend fun markStorySeen(storyPk: String): Boolean
    suspend fun getReels(amount: Int = 15, lastMediaPk: String? = null): NetworkResult<List<Reel>>
    suspend fun getUserClips(userId: String, amount: Int = 20): NetworkResult<List<Reel>>

    suspend fun getThreads(amount: Int = 20): NetworkResult<List<Message>>
    suspend fun getThreadMessages(threadId: String, amount: Int = 20): NetworkResult<List<Message>>
    suspend fun sendMessage(threadId: String, text: String): NetworkResult<Message>

    suspend fun searchAll(query: String): NetworkResult<Pair<List<User>, List<Reel>>>
    suspend fun getUserInfo(identifier: String): NetworkResult<User>
    suspend fun getUserPosts(userId: String, amount: Int = 20): NetworkResult<List<Post>>

    suspend fun likeMedia(mediaId: String): ActionResult
    suspend fun unlikeMedia(mediaId: String): ActionResult
    suspend fun saveMedia(mediaId: String): ActionResult
    suspend fun followUser(userId: String): ActionResult
    suspend fun unfollowUser(userId: String): ActionResult
    suspend fun getComments(mediaId: String, amount: Int = 25): NetworkResult<List<Comment>>
    suspend fun addComment(mediaId: String, text: String): NetworkResult<Comment>
}

/**
 * Implementation of InstagramDataSource delegating calls to PythonInstagramBridge.
 */
class PythonInstagramDataSource(
    private val bridge: PythonInstagramBridge
) : InstagramDataSource {

    override suspend fun login(username: String, password: String, verificationCode: String): AuthResult =
        bridge.login(username, password, verificationCode)

    override suspend fun loginBySessionId(sessionId: String): AuthResult =
        bridge.loginBySessionId(sessionId)

    override suspend fun loginWithCookies(cookiesJson: String): AuthResult =
        bridge.loginWithCookies(cookiesJson)

    override suspend fun restoreSession(settingsJson: String): Boolean =
        bridge.restoreSession(settingsJson)

    override suspend fun dumpSession(): String =
        bridge.dumpSession()

    override suspend fun checkSession(): Boolean =
        bridge.checkSession()

    override suspend fun logout(): Boolean =
        bridge.logout()

    override suspend fun getTimeline(cursor: String?): NetworkResult<PythonTimelineData> =
        bridge.getTimeline(cursor)

    override suspend fun getExplore(cursor: String?): NetworkResult<List<Post>> =
        bridge.getExplore(cursor)

    override suspend fun getStoriesTray(): NetworkResult<List<Story>> =
        bridge.getStoriesTray()

    override suspend fun getUserStories(userId: String): NetworkResult<List<Story>> =
        bridge.getUserStories(userId)

    override suspend fun markStorySeen(storyPk: String): Boolean =
        bridge.markStorySeen(storyPk)

    override suspend fun getReels(amount: Int, lastMediaPk: String?): NetworkResult<List<Reel>> =
        bridge.getReels(amount, lastMediaPk)

    override suspend fun getUserClips(userId: String, amount: Int): NetworkResult<List<Reel>> =
        bridge.getUserClips(userId, amount)

    override suspend fun getThreads(amount: Int): NetworkResult<List<Message>> =
        bridge.getThreads(amount)

    override suspend fun getThreadMessages(threadId: String, amount: Int): NetworkResult<List<Message>> =
        bridge.getThreadMessages(threadId, amount)

    override suspend fun sendMessage(threadId: String, text: String): NetworkResult<Message> =
        bridge.sendMessage(threadId, text)

    override suspend fun searchAll(query: String): NetworkResult<Pair<List<User>, List<Reel>>> =
        bridge.searchAll(query)

    override suspend fun getUserInfo(identifier: String): NetworkResult<User> =
        bridge.getUserInfo(identifier)

    override suspend fun getUserPosts(userId: String, amount: Int): NetworkResult<List<Post>> =
        bridge.getUserPosts(userId, amount)

    override suspend fun likeMedia(mediaId: String): ActionResult =
        bridge.likeMedia(mediaId)

    override suspend fun unlikeMedia(mediaId: String): ActionResult =
        bridge.unlikeMedia(mediaId)

    override suspend fun saveMedia(mediaId: String): ActionResult =
        bridge.saveMedia(mediaId)

    override suspend fun followUser(userId: String): ActionResult =
        bridge.followUser(userId)

    override suspend fun unfollowUser(userId: String): ActionResult =
        bridge.unfollowUser(userId)

    override suspend fun getComments(mediaId: String, amount: Int): NetworkResult<List<Comment>> =
        bridge.getComments(mediaId, amount)

    override suspend fun addComment(mediaId: String, text: String): NetworkResult<Comment> =
        bridge.addComment(mediaId, text)
}
