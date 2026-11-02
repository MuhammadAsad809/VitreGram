package com.vitregram.tv.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vitregram.tv.data.local.db.dao.*
import com.vitregram.tv.data.local.db.entity.*

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        ReelEntity::class,
        StoryEntity::class,
        WatchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VitreGramDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun reelDao(): ReelDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: VitreGramDatabase? = null

        fun getInstance(context: Context): VitreGramDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitreGramDatabase::class.java,
                    "vitregram_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
