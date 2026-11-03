package com.vitregram.tv.data.local.db.dao

import androidx.room.*
import com.vitregram.tv.data.local.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getCachedPosts(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    @Query("UPDATE posts SET isLiked = :isLiked, likeCount = likeCount + :delta WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, isLiked: Boolean, delta: Int)
}

@Dao
interface ReelDao {
    @Query("SELECT * FROM reels")
    fun getAllReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels")
    suspend fun getCachedReels(): List<ReelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelEntity>)

    @Query("DELETE FROM reels")
    suspend fun clearAll()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    suspend fun getCachedStories(): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("DELETE FROM stories")
    suspend fun clearAll()

    @Query("UPDATE stories SET isSeen = 1 WHERE id = :storyId")
    suspend fun markSeen(storyId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}
