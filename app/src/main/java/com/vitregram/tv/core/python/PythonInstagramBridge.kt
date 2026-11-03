package com.vitregram.tv.core.python

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vitregram.tv.data.instagram.*
import com.vitregram.tv.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thread-safe coroutine bridge between Android Kotlin and Chaquopy Python Instagram modules.
 * Executes all Python runtime operations strictly on Dispatchers.IO.
 */
class PythonInstagramBridge(
    private val runtimeManager: PythonRuntimeManager,
    private val gson: Gson = Gson()
) {

    private suspend fun callPython(moduleName: String, methodName: String, vararg args: Any?): String =
        withContext(Dispatchers.IO) {
            val module = runtimeManager.getModule(moduleName)
            val pyArgs = args.map { it ?: "" }.toTypedArray()
            val pyResult = module.callAttr(methodName, *pyArgs)
            pyResult.toString()
        }

    // ------------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------------

    suspend fun login(username: String, password: String, verificationCode: String = ""): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "login", username, password, verificationCode)
                val type = object : TypeToken<PythonResponse<PythonAuthData>>() {}.type
                val res: PythonResponse<PythonAuthData> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    val session = AuthSession(
                        userId = res.data.userId,
                        username = res.data.username.ifEmpty { username },
                        sessionToken = res.data.sessionToken,
                        csrfToken = res.data.csrfToken,
                        cookies = res.data.cookies
                    )
                    AuthResult.Success(session)
                } else if (res.errorType == "TwoFactorRequired") {
                    val twoFactorId = res.extra?.get("two_factor_identifier")?.toString() ?: ""
                    AuthResult.TwoFactorRequired(twoFactorId)
                } else if (res.errorType == "ChallengeRequired") {
                    val challengeUrl = res.extra?.get("challenge_url")?.toString()
                    AuthResult.ChallengeRequired(challengeType = "checkpoint", challengeUrl = challengeUrl)
                } else {
                    AuthResult.Error(res.message ?: "Authentication failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "login exception", e)
                AuthResult.Error(e.message ?: "Authentication failed")
            }
        }

    suspend fun loginBySessionId(sessionId: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "login_by_sessionid", sessionId)
                val type = object : TypeToken<PythonResponse<PythonAuthData>>() {}.type
                val res: PythonResponse<PythonAuthData> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    val session = AuthSession(
                        userId = res.data.userId,
                        username = res.data.username,
                        sessionToken = res.data.sessionToken,
                        csrfToken = res.data.csrfToken,
                        cookies = res.data.cookies
                    )
                    AuthResult.Success(session)
                } else {
                    AuthResult.Error(res.message ?: "Session restoration failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loginBySessionId exception", e)
                AuthResult.Error(e.message ?: "Session login failed")
            }
        }

    suspend fun loginWithCookies(cookiesJson: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "login_with_cookies", cookiesJson)
                val type = object : TypeToken<PythonResponse<PythonAuthData>>() {}.type
                val res: PythonResponse<PythonAuthData> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    val session = AuthSession(
                        userId = res.data.userId,
                        username = res.data.username,
                        sessionToken = res.data.sessionToken,
                        csrfToken = res.data.csrfToken,
                        cookies = res.data.cookies
                    )
                    AuthResult.Success(session)
                } else {
                    AuthResult.Error(res.message ?: "Failed to initialize session with cookies")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loginWithCookies exception", e)
                AuthResult.Error(e.message ?: "Failed to restore cookies")
            }
        }

    suspend fun restoreSession(settingsJson: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "restore_session", settingsJson)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                res.success && (res.data == true)
            } catch (e: Exception) {
                Log.e(TAG, "restoreSession exception", e)
                false
            }
        }

    suspend fun dumpSession(): String =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "dump_session")
                val type = object : TypeToken<PythonResponse<String>>() {}.type
                val res: PythonResponse<String> = gson.fromJson(json, type)
                res.data ?: "{}"
            } catch (e: Exception) {
                Log.e(TAG, "dumpSession exception", e)
                "{}"
            }
        }

    suspend fun checkSession(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "check_session")
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                res.success && (res.data == true)
            } catch (e: Exception) {
                Log.e(TAG, "checkSession exception", e)
                false
            }
        }

    suspend fun logout(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("authentication", "logout")
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                res.success
            } catch (e: Exception) {
                Log.e(TAG, "logout exception", e)
                true
            }
        }

    // ------------------------------------------------------------------------
    // Timeline Feed & Explore
    // ------------------------------------------------------------------------

    suspend fun getTimeline(cursor: String? = null): NetworkResult<PythonTimelineData> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("feed", "get_timeline", cursor ?: "")
                val type = object : TypeToken<PythonResponse<PythonTimelineData>>() {}.type
                val res: PythonResponse<PythonTimelineData> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to fetch timeline feed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getTimeline exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving timeline feed", throwable = e)
            }
        }

    suspend fun getExplore(cursor: String? = null): NetworkResult<List<Post>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("feed", "get_explore", cursor ?: "")
                val type = object : TypeToken<PythonResponse<List<Post>>>() {}.type
                val res: PythonResponse<List<Post>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to fetch explore page")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getExplore exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving explore items", throwable = e)
            }
        }

    // ------------------------------------------------------------------------
    // Stories & Reels
    // ------------------------------------------------------------------------

    suspend fun getStoriesTray(): NetworkResult<List<Story>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("stories", "get_stories_tray")
                val type = object : TypeToken<PythonResponse<List<Story>>>() {}.type
                val res: PythonResponse<List<Story>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve stories tray")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getStoriesTray exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving stories", throwable = e)
            }
        }

    suspend fun getUserStories(userId: String): NetworkResult<List<Story>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("stories", "get_user_stories", userId)
                val type = object : TypeToken<PythonResponse<List<Story>>>() {}.type
                val res: PythonResponse<List<Story>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve user stories")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserStories exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving user stories", throwable = e)
            }
        }

    suspend fun markStorySeen(storyPk: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("stories", "mark_story_seen", storyPk)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                res.success && (res.data == true)
            } catch (e: Exception) {
                Log.e(TAG, "markStorySeen exception", e)
                false
            }
        }

    suspend fun getReels(amount: Int = 15, lastMediaPk: String? = null): NetworkResult<List<Reel>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("reels", "get_reels", amount, lastMediaPk ?: "")
                val type = object : TypeToken<PythonResponse<List<Reel>>>() {}.type
                val res: PythonResponse<List<Reel>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve Reels")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getReels exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving reels", throwable = e)
            }
        }

    suspend fun getUserClips(userId: String, amount: Int = 20): NetworkResult<List<Reel>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("reels", "get_user_clips", userId, amount)
                val type = object : TypeToken<PythonResponse<List<Reel>>>() {}.type
                val res: PythonResponse<List<Reel>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve user clips")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserClips exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving user clips", throwable = e)
            }
        }

    // ------------------------------------------------------------------------
    // Direct Messaging
    // ------------------------------------------------------------------------

    suspend fun getThreads(amount: Int = 20): NetworkResult<List<Message>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("messages", "get_threads", amount)
                val type = object : TypeToken<PythonResponse<List<Message>>>() {}.type
                val res: PythonResponse<List<Message>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve direct threads")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getThreads exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving messages", throwable = e)
            }
        }

    suspend fun getThreadMessages(threadId: String, amount: Int = 20): NetworkResult<List<Message>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("messages", "get_thread_messages", threadId, amount)
                val type = object : TypeToken<PythonResponse<List<Message>>>() {}.type
                val res: PythonResponse<List<Message>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve conversation messages")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getThreadMessages exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving messages", throwable = e)
            }
        }

    suspend fun sendMessage(threadId: String, text: String): NetworkResult<Message> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("messages", "send_message", threadId, text)
                val type = object : TypeToken<PythonResponse<Message>>() {}.type
                val res: PythonResponse<Message> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to send message")
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage exception", e)
                NetworkResult.Error(e.message ?: "Error sending message", throwable = e)
            }
        }

    // ------------------------------------------------------------------------
    // Search & Profile
    // ------------------------------------------------------------------------

    suspend fun searchAll(query: String): NetworkResult<Pair<List<User>, List<Reel>>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("search", "search_all", query)
                val type = object : TypeToken<PythonResponse<PythonSearchData>>() {}.type
                val res: PythonResponse<PythonSearchData> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(Pair(res.data.users, res.data.media))
                } else {
                    NetworkResult.Error(res.message ?: "Search failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchAll exception", e)
                NetworkResult.Error(e.message ?: "Error executing search", throwable = e)
            }
        }

    suspend fun getUserInfo(identifier: String): NetworkResult<User> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("profile", "get_user_info", identifier)
                val type = object : TypeToken<PythonResponse<User>>() {}.type
                val res: PythonResponse<User> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to fetch user profile")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserInfo exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving user profile", throwable = e)
            }
        }

    suspend fun getUserPosts(userId: String, amount: Int = 20): NetworkResult<List<Post>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("profile", "get_user_posts", userId, amount)
                val type = object : TypeToken<PythonResponse<List<Post>>>() {}.type
                val res: PythonResponse<List<Post>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to fetch user posts")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserPosts exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving user posts", throwable = e)
            }
        }

    // ------------------------------------------------------------------------
    // Interactions
    // ------------------------------------------------------------------------

    suspend fun likeMedia(mediaId: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "like_media", mediaId)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                ActionResult(success = res.success && (res.data == true), message = res.message)
            } catch (e: Exception) {
                Log.e(TAG, "likeMedia exception", e)
                ActionResult(success = false, message = e.message)
            }
        }

    suspend fun unlikeMedia(mediaId: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "unlike_media", mediaId)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                ActionResult(success = res.success && (res.data == true), message = res.message)
            } catch (e: Exception) {
                Log.e(TAG, "unlikeMedia exception", e)
                ActionResult(success = false, message = e.message)
            }
        }

    suspend fun saveMedia(mediaId: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "save_media", mediaId)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                ActionResult(success = res.success && (res.data == true), message = res.message)
            } catch (e: Exception) {
                Log.e(TAG, "saveMedia exception", e)
                ActionResult(success = false, message = e.message)
            }
        }

    suspend fun followUser(userId: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "follow_user", userId)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                ActionResult(success = res.success && (res.data == true), message = res.message)
            } catch (e: Exception) {
                Log.e(TAG, "followUser exception", e)
                ActionResult(success = false, message = e.message)
            }
        }

    suspend fun unfollowUser(userId: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "unfollow_user", userId)
                val type = object : TypeToken<PythonResponse<Boolean>>() {}.type
                val res: PythonResponse<Boolean> = gson.fromJson(json, type)
                ActionResult(success = res.success && (res.data == true), message = res.message)
            } catch (e: Exception) {
                Log.e(TAG, "unfollowUser exception", e)
                ActionResult(success = false, message = e.message)
            }
        }

    suspend fun getComments(mediaId: String, amount: Int = 25): NetworkResult<List<Comment>> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "get_comments", mediaId, amount)
                val type = object : TypeToken<PythonResponse<List<Comment>>>() {}.type
                val res: PythonResponse<List<Comment>> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to retrieve comments")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getComments exception", e)
                NetworkResult.Error(e.message ?: "Error retrieving comments", throwable = e)
            }
        }

    suspend fun addComment(mediaId: String, text: String): NetworkResult<Comment> =
        withContext(Dispatchers.IO) {
            try {
                val json = callPython("interactions", "add_comment", mediaId, text)
                val type = object : TypeToken<PythonResponse<Comment>>() {}.type
                val res: PythonResponse<Comment> = gson.fromJson(json, type)
                if (res.success && res.data != null) {
                    NetworkResult.Success(res.data)
                } else {
                    NetworkResult.Error(res.message ?: "Failed to post comment")
                }
            } catch (e: Exception) {
                Log.e(TAG, "addComment exception", e)
                NetworkResult.Error(e.message ?: "Error posting comment", throwable = e)
            }
        }

    companion object {
        private const val TAG = "PythonInstagramBridge"
    }
}
